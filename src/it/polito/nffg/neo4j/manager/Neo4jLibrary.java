/*
 * Copyright 2016 Politecnico di Torino
 * Authors:
 * Project Supervisor and Contact: Riccardo Sisto (riccardo.sisto@polito.it)
 * 
 * This file is part of Verigraph.
 * 
 * Verigraph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Verigraph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with Verigraph.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package it.polito.nffg.neo4j.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.traversal.Paths;
import org.neo4j.tooling.GlobalGraphOperations;

import it.polito.nffg.neo4j.config.Neo4jApplication;
import it.polito.nffg.neo4j.exceptions.MyConstraintViolationException;
import it.polito.nffg.neo4j.exceptions.MyNotFoundException;
import it.polito.nffg.neo4j.jaxb.ActionEnumType;
import it.polito.nffg.neo4j.jaxb.ActionType;
import it.polito.nffg.neo4j.jaxb.ActionsType;
import it.polito.nffg.neo4j.jaxb.CiType;
import it.polito.nffg.neo4j.jaxb.CpType;
import it.polito.nffg.neo4j.jaxb.CpointsType;
import it.polito.nffg.neo4j.jaxb.CtrlInterfacesType;
import it.polito.nffg.neo4j.jaxb.EpCpType;
import it.polito.nffg.neo4j.jaxb.EpType;
import it.polito.nffg.neo4j.jaxb.EpsCpsType;
import it.polito.nffg.neo4j.jaxb.FlowrulesType;
import it.polito.nffg.neo4j.jaxb.MonParamsType;
import it.polito.nffg.neo4j.jaxb.NeType;
import it.polito.nffg.neo4j.jaxb.NfType;
import it.polito.nffg.neo4j.jaxb.Nffg;
import it.polito.nffg.neo4j.jaxb.NffgSet;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;
import it.polito.nffg.neo4j.jaxb.PortDirEnumType;
import it.polito.nffg.neo4j.jaxb.PortType;
import it.polito.nffg.neo4j.jaxb.SpecType;
import it.polito.nffg.neo4j.jaxb.CiType.Attributes;
import it.polito.nffg.neo4j.jaxb.CiType.Attributes.Attribute;
import it.polito.nffg.neo4j.jaxb.FlowrulesType.Flowspace.Ip;
import it.polito.nffg.neo4j.jaxb.FlowrulesType.Flowspace.Mac;
import it.polito.nffg.neo4j.jaxb.FlowrulesType.Flowspace.Tcp;
import it.polito.nffg.neo4j.jaxb.FlowrulesType.Flowspace.Udp;
import it.polito.nffg.neo4j.jaxb.MonParamsType.Parameter;
import it.polito.nffg.neo4j.jaxb.SpecType.Cpu;
import it.polito.nffg.neo4j.jaxb.SpecType.Deployment;
import it.polito.nffg.neo4j.jaxb.SpecType.Image;
import it.polito.nffg.neo4j.jaxb.SpecType.Memory;
import it.polito.nffg.neo4j.jaxb.SpecType.Storage;

/**
 * The Neo4jLibrary is an enumeration with only one possible value,
 * then is a natural singleton. This feature is guaranteed by the JVM. 
 */
public enum Neo4jLibrary
{
	instance;
	
	private GraphDatabaseFactory dbFactory;
	private GraphDatabaseService graphDB;
	private GlobalGraphOperations gcOperations; 
	private ObjectFactory obFactory;
	private Properties pr = Neo4jApplication.PropCache.getProp();
	private static final int MAX_DEPTH = 50;
	private final String dbPath = pr.getProperty("my.user.dir") + "/" + pr.getProperty("graphDBPath");
	
	private Neo4jLibrary()
	{
		dbFactory = new GraphDatabaseFactory();
		graphDB = dbFactory.newEmbeddedDatabase(new File(dbPath)); //pwd = nffg
		registerShutdownHook(graphDB);
		setConstraints();
		gcOperations = GlobalGraphOperations.at(graphDB);
		obFactory = new ObjectFactory();
	}
	
	private static void registerShutdownHook(final GraphDatabaseService graphDB)
	{
	    // Registers a shutdown hook for the Neo4j instance so that it shuts down 
		// nicely when the VM exits (even if you "Ctrl-C" the running application).
	    Runtime.getRuntime().addShutdownHook(new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDB.shutdown();
	        }
	    });
	}
	
	/**
	 * Getter method to obtain the instance of ObjectFactory initialized in the private constructor of the library.
	 * 
	 * @return a reference to the instance.
	 * @see ObjectFactory
	 */
	public ObjectFactory getObjectFactory()
	{
		return obFactory;
	}

	private enum NodeType implements Label
	{
		Referenceable, Pathable, Endpoint, NetworkFunction, NetworkElement, ConnectionPoint,
		MonitoringParameter, Flowspace, Specification, CtrlInterface, Flowrules, Action, Nffg;
	}
	
	private enum RelationType implements RelationshipType
	{
		PathRelationship, InfoRelationship;
	}
	
	private boolean getConstraintExist(Label nodeType, String property)
	{
		for (ConstraintDefinition cd : graphDB.schema().getConstraints(nodeType))
		{
			if (cd.getConstraintType() == ConstraintType.UNIQUENESS)
			{
				for (String propertyKey : cd.getPropertyKeys())
				{
					if (propertyKey.equals(property))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private void setConstraints()
	{
		Transaction tx = graphDB.beginTx();
		
		try
		{
			if (!getConstraintExist(NodeType.Nffg, "id")) {
				graphDB.schema().constraintFor(NodeType.Nffg).assertPropertyIsUnique("id").create();
			}
			
			if (!getConstraintExist(NodeType.Endpoint, "uniqueProp")) {
				graphDB.schema().constraintFor(NodeType.Endpoint).assertPropertyIsUnique("uniqueProp").create();
			}
			
			if (!getConstraintExist(NodeType.NetworkFunction, "uniqueProp")) {
				graphDB.schema().constraintFor(NodeType.NetworkFunction).assertPropertyIsUnique("uniqueProp").create();
			}
			
			if (!getConstraintExist(NodeType.NetworkElement, "uniqueProp")) {
				graphDB.schema().constraintFor(NodeType.NetworkElement).assertPropertyIsUnique("uniqueProp").create();
			}
			
			if (!getConstraintExist(NodeType.ConnectionPoint, "uniqueProp")) {
				graphDB.schema().constraintFor(NodeType.ConnectionPoint).assertPropertyIsUnique("uniqueProp").create();
			}
			
			if (!getConstraintExist(NodeType.CtrlInterface, "uniqueProp")) {
				graphDB.schema().constraintFor(NodeType.CtrlInterface).assertPropertyIsUnique("uniqueProp").create();
			}
			
			tx.success();
		}
		catch (Exception e)
		{
			tx.failure();
			e.printStackTrace();
		}
		finally
		{
			tx.close();
		}
		
	}
	
	private Node createUniqueNode(NodeType nodeLabel, String propertyName, Object propertyValue)
	{
		Transaction tx = graphDB.beginTx();
		
		try
		{
		    String queryString = "CREATE (n:" + nodeLabel.name() + " {" + propertyName + ": {value}}) RETURN n";
		    Map<String, Object> parameters = new HashMap<>();
		    parameters.put("value", propertyValue);
		    ResourceIterator<Node> resultIterator = graphDB.execute(queryString, parameters).columnAs("n");
		    Node result = resultIterator.next();
		    resultIterator.close();
		    tx.success();
		    
		    return result;
		}
		catch (QueryExecutionException e)
		{
			tx.failure();
			return null;
		}
		catch (TransactionFailureException e)
		{
			tx.failure();
			return null;
		}
		catch (Exception e)
		{
			tx.failure();
			throw e;
		}
		finally
		{
			tx.close();
		}
	}
	
	private void setMacHeadersInFlowspaceNode(Mac mac, Node fsNode)
	{
		if (mac != null)
		{
			if (mac.getSrc() != null) {
				fsNode.setProperty("mac-src", mac.getSrc());
			}
			
			if (mac.getDst() != null) {
				fsNode.setProperty("mac-dst", mac.getDst());
			}
			
			if (mac.getEthertype() != null)	{
				fsNode.setProperty("mac-ethertype", mac.getEthertype().intValue());
			}
			
			if (mac.getVlanId() != null) {
				fsNode.setProperty("mac-vlanId", mac.getVlanId().intValue());
			}
			
			if (mac.getVlanPcp() != null) {
				fsNode.setProperty("mac-vlanPcp", mac.getVlanPcp().intValue());
			}
		}
	}
	
	private Mac setMacHeadersInFlowspaceElement(Node fsNode)
	{
		Mac elMac = null;
		
		if (fsNode.hasProperty("mac-src") || fsNode.hasProperty("mac-dst") || fsNode.hasProperty("mac-vlanId") ||
			fsNode.hasProperty("mac-vlanPcp") || fsNode.hasProperty("mac-ethertype"))
		{
			elMac = obFactory.createFlowrulesTypeFlowspaceMac();
			
			if (fsNode.hasProperty("mac-src")) {
				elMac.setSrc((String) fsNode.getProperty("mac-src"));
			}
			
			if (fsNode.hasProperty("mac-dst")) {
				elMac.setDst((String) fsNode.getProperty("mac-dst"));
			}
			
			if (fsNode.hasProperty("mac-ethertype")) {
				elMac.setEthertype((Integer) fsNode.getProperty("mac-ethertype"));
			}
			
			if (fsNode.hasProperty("mac-vlanId")) {
				elMac.setVlanId((Integer) fsNode.getProperty("mac-vlanId"));
			}
			
			if (fsNode.hasProperty("mac-vlanPcp")) {
				elMac.setVlanPcp((Integer) fsNode.getProperty("mac-vlanPcp"));
			}
		}
		
		return elMac;
	}
	
	private void setIpHeadersInFlowspaceNode(Ip ip, Node fsNode)
	{
		if (ip != null)
		{
			if (ip.getSrc() != null) {
				fsNode.setProperty("ip-src", ip.getSrc());
			}
			
			if (ip.getDst() != null) {
				fsNode.setProperty("ip-dst", ip.getDst());
			}
			
			if (ip.getIpProtocol() != null) {
				fsNode.setProperty("ip-ipProtocol", ip.getIpProtocol().shortValue());
			}
			
			if (ip.getTos() != null) {
				fsNode.setProperty("ip-tos", ip.getTos().shortValue());
			}
		}
	}
	
	private Ip setIpHeadersInFlowspaceElement(Node fsNode)
	{
		Ip elIp = null;
		
		if (fsNode.hasProperty("ip-src") || fsNode.hasProperty("ip-dst") ||
			fsNode.hasProperty("ip-ipProtocol") || fsNode.hasProperty("ip-tos"))
		{
			elIp = obFactory.createFlowrulesTypeFlowspaceIp();
			
			if (fsNode.hasProperty("ip-src")) {
				elIp.setSrc((String) fsNode.getProperty("ip-src"));
			}
			
			if (fsNode.hasProperty("ip-dst")) {
				elIp.setDst((String) fsNode.getProperty("ip-dst"));
			}
			
			if (fsNode.hasProperty("ip-ipProtocol")) {
				elIp.setIpProtocol((Short) fsNode.getProperty("ip-ipProtocol"));
			}
			
			if (fsNode.hasProperty("ip-tos")) {
				elIp.setTos((Short) fsNode.getProperty("ip-tos"));
			}
		}
		
		return elIp;
	}
	
	private void setTcpHeadersInFlowspaceNode(Tcp tcp, Node fsNode)
	{
		if (tcp != null)
		{
			if (tcp.getSrc() != null) {
				fsNode.setProperty("tcp-src", tcp.getSrc());
			}
			
			if (tcp.getDst() != null) {
				fsNode.setProperty("tcp-dst", tcp.getDst());
			}
		}
	}
	
	private Tcp setTcpHeadersInFlowspaceElement(Node fsNode)
	{
		Tcp elTcp = null;
		
		if (fsNode.hasProperty("tcp-src") || fsNode.hasProperty("tcp-dst"))
		{
			elTcp = obFactory.createFlowrulesTypeFlowspaceTcp();
			
			if (fsNode.hasProperty("tcp-src")) {
				elTcp.setSrc((Integer) fsNode.getProperty("tcp-src"));
			}
			
			if (fsNode.hasProperty("tcp-dst")) {
				elTcp.setDst((Integer) fsNode.getProperty("tcp-dst"));
			}
		}
		
		return elTcp;
	}
	
	private void setUdpHeadersInFlowspaceNode(Udp udp, Node fsNode)
	{
		if (udp != null)
		{
			if (udp.getSrc() != null) {
				fsNode.setProperty("udp-src", udp.getSrc());
			}
			
			if (udp.getDst() != null) {
				fsNode.setProperty("udp-dst", udp.getDst());
			}
		}
	}
	
	private Udp setUdpHeadersInFlowspaceElement(Node fsNode)
	{
		Udp elUdp = null;
		
		if (fsNode.hasProperty("udp-src") || fsNode.hasProperty("udp-dst"))
		{
			elUdp = obFactory.createFlowrulesTypeFlowspaceUdp();
			
			if (fsNode.hasProperty("udp-src")) {
				elUdp.setSrc((Integer) fsNode.getProperty("udp-src"));
			}
			
			if (fsNode.hasProperty("udp-dst")) {
				elUdp.setDst((Integer) fsNode.getProperty("udp-dst"));
			}
		}
		
		return elUdp;
	}
	
	private void setMonitoringParametersNode(MonParamsType mpt, Node tmpNode, String nffgId)
	{		
		if (!mpt.getParameter().isEmpty())
		{		
			int n = 0;
			String[] tmpArray;
			Node mpNode = graphDB.createNode(NodeType.MonitoringParameter);
			tmpNode.createRelationshipTo(mpNode, RelationType.InfoRelationship);
			
			for (Parameter par : mpt.getParameter())
			{
				tmpArray = new String[par.getValue().size()];
				tmpArray = par.getValue().toArray(tmpArray);
				mpNode.setProperty("parameters[" + (n++) +"]", tmpArray);
				mpNode.setProperty("nffgId", nffgId);
			}
		}
	}
	
	private void setMonitoringParametersElement(MonParamsType mpt, Node mptNode)
	{
		int i = 0;
		String[] tmpArray;
		Parameter par;
		
		while (mptNode.hasProperty("parameters[" + i + "]"))
		{
			par = obFactory.createMonParamsTypeParameter();
			tmpArray = (String[]) mptNode.getProperty("parameters[" + (i++) + "]");
			
			for (int j = 0; j < tmpArray.length; j++)
			{
				par.getValue().add(tmpArray[j]);
			}
			
			mpt.getParameter().add(par);
		}
	}
	
	/**
	 * Save a new graph into the Neo4j database.
	 *
	 * @param graph the Network Function Forwarding Graph we want to save into the database.
	 * @return the id assigned to the graph into the database.
	 * @throws MyConstraintViolationException if a validation error occurs during the process.
	 * @throws Exception in case of any other unpredictable errors.
	 * @see Nffg
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Integer.html">Integer</a>
	 * @see MyConstraintViolationException
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public synchronized Integer createNffg(Nffg graph) throws Exception
	{
		SpecType st; EpType.Flowspace efs;
		FlowrulesType.Flowspace ffs; String[] tmpArray;
		Node nffgNode, epNode, nfNode, cpNode, neNode, fsNode, spNode, ciNode, frNode, acNode, tmpNode;
		Set<String> refs = new HashSet<String>();
		String nffgId; int tmpId = 0;
		
		Transaction tx = graphDB.beginTx();
		
		try
		{
			tmpId = getValidNffgId(graph.getId());
			nffgId = "nffg_" + tmpId;			
			nffgNode = createUniqueNode(NodeType.Nffg, "id", nffgId);
			
			if (nffgNode == null)
			{
				tx.failure();
				throw new Exception();
			}
			
			if (graph.getVersion() != null) {
				nffgNode.setProperty("version", graph.getVersion());
			}
			
			setMonitoringParametersNode(graph.getMonitoringParameters(), nffgNode, nffgId);
			
			for (EpType ept : graph.getEndpoints().getEndpoint())
			{
				epNode = createUniqueNode(NodeType.Endpoint, "uniqueProp", nffgId + "-" + ept.getId());
					
				if (epNode == null)
				{
					tx.failure();
					throw new MyConstraintViolationException("Unique value '" + ept.getId() + "' duplicated within the document");
				}
					
				refs.add(ept.getId());
				epNode.addLabel(NodeType.Referenceable);
				epNode.addLabel(NodeType.Pathable);
				epNode.setProperty("nffgId", nffgId);
				epNode.setProperty("id", ept.getId());
				nffgNode.createRelationshipTo(epNode, RelationType.InfoRelationship);
				efs = ept.getFlowspace();
					
				if (efs.getNodeId() != null || efs.getIngPhysPort() != null || efs.getMac() != null ||	
					efs.getIp() != null || efs.getTcp() != null || efs.getUdp() != null)
				{
					fsNode = graphDB.createNode(NodeType.Flowspace);
					fsNode.setProperty("nffgId", nffgId);
					epNode.createRelationshipTo(fsNode, RelationType.InfoRelationship);
						
					if (efs.getNodeId() != null) {
						fsNode.setProperty("nodeId", efs.getNodeId());
					}
						
					if (efs.getIngPhysPort() != null) {
						fsNode.setProperty("ingPhysPort", efs.getIngPhysPort());
					}
						
					setMacHeadersInFlowspaceNode(efs.getMac(), fsNode);
					setIpHeadersInFlowspaceNode(efs.getIp(), fsNode);
					setTcpHeadersInFlowspaceNode(efs.getTcp(), fsNode);
					setUdpHeadersInFlowspaceNode(efs.getUdp(), fsNode);
				}
			}
			
			for (NfType nft : graph.getNetworkFunctions().getNetworkFunction())
			{
				nfNode = createUniqueNode(NodeType.NetworkFunction, "uniqueProp", nffgId + "-" + nft.getId());
						
				if (nfNode == null)
				{
					tx.failure();
					throw new MyConstraintViolationException("Unique value '" + nft.getId() + "' duplicated within the document");
				}
					
				nfNode.addLabel(NodeType.Pathable);
				nffgNode.createRelationshipTo(nfNode, RelationType.InfoRelationship);
				nfNode.setProperty("functionType", nft.getFunctionalType());
				nfNode.setProperty("nffgId", nffgId);
				nfNode.setProperty("id", nft.getId());
				setMonitoringParametersNode(nft.getMonitoringParameters(), nfNode, nffgId);
				st = nft.getSpecification();
					
				if (!st.getDeployment().getType().equals("N.A.") || !st.getImage().getUri().equals("N.A.") ||
					!st.getCpu().getModel().equals("N.A.") || !st.getCpu().getArchitecture().equals("N.A.") ||
					st.getCpu().getNumCores() != 1 || !st.getCpu().getClockSpeed().equals("N.A.") ||
					!st.getMemory().getType().equals("N.A.") || !st.getMemory().getSize().equals("N.A.") ||
					!st.getStorage().getType().equals("N.A.") || !st.getStorage().getSize().equals("N.A."))
				{
					spNode = graphDB.createNode(NodeType.Specification);
					spNode.setProperty("nffgId", nffgId);
					nfNode.createRelationshipTo(spNode, RelationType.InfoRelationship);
					
					if (!st.getDeployment().getType().equals("N.A.")) {
						spNode.setProperty("deployment", st.getDeployment().getType());
					}
						
					if (!st.getImage().getUri().equals("N.A.")) {
						spNode.setProperty("image", st.getImage().getUri());
					}
						
					if (!st.getCpu().getModel().equals("N.A.")) {
						spNode.setProperty("cpu-model", st.getCpu().getModel());
					}
						
					if (!st.getCpu().getArchitecture().equals("N.A.")) {
						spNode.setProperty("cpu-architecture", st.getCpu().getArchitecture());
					}
						
					if (st.getCpu().getNumCores() != 1) {
						spNode.setProperty("cpu-numCores", st.getCpu().getNumCores());
					}
						
					if (!st.getCpu().getClockSpeed().equals("N.A.")) {
						spNode.setProperty("cpu-clockSpeed", st.getCpu().getClockSpeed());
					}
						
					if (!st.getMemory().getType().equals("N.A.")) {
						spNode.setProperty("mem-type", st.getMemory().getType());
					}
						
					if (!st.getMemory().getSize().equals("N.A.")) {
						spNode.setProperty("mem-size", st.getMemory().getSize());
					}
						
					if (!st.getStorage().getType().equals("N.A.")) {
						spNode.setProperty("sto-type", st.getStorage().getType());
					}
						
					if (!st.getStorage().getSize().equals("N.A.")) {
						spNode.setProperty("sto-size", st.getStorage().getSize());
					}
				}
					
				for (CpType cpt : nft.getConnectionPoints().getConnectionPoint())
				{
					cpNode = createUniqueNode(NodeType.ConnectionPoint, "uniqueProp", nffgId + "-" + cpt.getId());
						
					if (cpNode == null)
					{
						tx.failure();
						throw new MyConstraintViolationException("Unique value '" + cpt.getId() + "' duplicated within the document");
					}
						
					refs.add(cpt.getId());
					cpNode.setProperty("nffgId", nffgId);
					cpNode.setProperty("id", cpt.getId());
					cpNode.addLabel(NodeType.Referenceable);
					cpNode.addLabel(NodeType.Pathable);
						
					if (cpt.getPort().getDirection() == PortDirEnumType.IN)
					{
						cpNode.createRelationshipTo(nfNode, RelationType.PathRelationship);
					}
					else if (cpt.getPort().getDirection() == PortDirEnumType.OUT)
					{
						nfNode.createRelationshipTo(cpNode, RelationType.PathRelationship);
					}
					else
					{
						cpNode.createRelationshipTo(nfNode, RelationType.PathRelationship);
						nfNode.createRelationshipTo(cpNode, RelationType.PathRelationship);
					}
						
					cpNode.setProperty("port-id", cpt.getPort().getId());
					cpNode.setProperty("port-direction", cpt.getPort().getDirection().value());
						
					if (!cpt.getPort().getType().equals("N.A.")) {
						cpNode.setProperty("port-type", cpt.getPort().getType());
					}
				}
					
				for (CiType cit : nft.getControlInterfaces().getControlInterface())
				{
					ciNode = createUniqueNode(NodeType.CtrlInterface, "uniqueProp", nffgId + "-" + cit.getId());
						
					if (ciNode == null)
					{
						tx.failure();
						throw new MyConstraintViolationException("Unique value '" + cit.getId() + "' duplicated within the document");
					}	
						
					ciNode.setProperty("nffgId", nffgId);
					ciNode.setProperty("id", cit.getId());
					nfNode.createRelationshipTo(ciNode, RelationType.InfoRelationship);
						
					if (cit.getAttributes().getAttribute().size() > 0)
					{
						tmpArray = new String[cit.getAttributes().getAttribute().size()];
							
						for (int i = 0; i < tmpArray.length; i++)
						{
							tmpArray[i] = cit.getAttributes().getAttribute().get(i).getValue();
						}
							
						ciNode.setProperty("attributes", tmpArray);
					}
				}
			}
			
			for (NeType net : graph.getNetworkElements().getNetworkElement())
			{
				neNode = createUniqueNode(NodeType.NetworkElement, "uniqueProp", nffgId + "-" + net.getId());
					
				if (neNode == null)
				{
					tx.failure();
					throw new MyConstraintViolationException("Unique value '" + net.getId() + "' duplicated within the document");
				}
					
				nffgNode.createRelationshipTo(neNode, RelationType.InfoRelationship);
				neNode.setProperty("type", net.getType());
				neNode.setProperty("nffgId", nffgId);
				neNode.setProperty("id", net.getId());
				setMonitoringParametersNode(net.getMonitoringParameters(), neNode, nffgId);
					
				for (EpCpType epcpt : net.getEpsCps().getEpCp())
				{
					if (!refs.contains(epcpt.getIdRef()))
					{
						tx.failure();
						throw new MyConstraintViolationException("id_ref '" + epcpt.getIdRef() + "' not found within the document");
					}
						
					for (FlowrulesType frt : epcpt.getFlowrules())
					{
						frNode = graphDB.createNode(NodeType.Flowrules);
						frNode.setProperty("nffgId", nffgId);
						neNode.createRelationshipTo(frNode, RelationType.InfoRelationship);
						frNode.setProperty("epcp-idRef", epcpt.getIdRef());
							
						for (ActionType act : frt.getActions().getAction())
						{
							acNode = graphDB.createNode(NodeType.Action);
							acNode.setProperty("nffgId", nffgId);
							frNode.createRelationshipTo(acNode, RelationType.InfoRelationship);
							acNode.setProperty("type", act.getType().value());
								
							if (act.getType() == ActionEnumType.OUTPUT)
							{
								if (act.getPort() == null)
								{
									tx.failure();
									throw new MyConstraintViolationException("Type of the action, associated with ep-cp whose id_ref = " + epcpt.getIdRef() + ", is 'output' but there is no specified port");
								}
									
								if (!refs.contains(act.getPort()))
								{
									tx.failure();
									throw new MyConstraintViolationException("The specified port '" + act.getPort() + "' doesn't match with any ep/cp within the document");
								}
									
								acNode.setProperty("port", act.getPort());
								tmpNode = graphDB.findNode(NodeType.Referenceable, "uniqueProp", nffgId + "-" + epcpt.getIdRef());
								tmpNode.createRelationshipTo(graphDB.findNode(NodeType.Referenceable, "uniqueProp", nffgId + "-" + act.getPort()), RelationType.PathRelationship);
							}
							else
							{
								if (act.getPort() != null)
								{
									tx.failure();
									throw new MyConstraintViolationException("Type of the action, associated with ep-cp whose id_ref = " + epcpt.getIdRef() + ", is 'discard' but there is a specified port");
								}
							}	
						}
							
						ffs = frt.getFlowspace();
							
						if (ffs.getIngPort() != null || ffs.getMac() != null ||	
							ffs.getIp() != null || ffs.getTcp() != null || ffs.getUdp() != null)
						{
							fsNode = graphDB.createNode(NodeType.Flowspace);
							fsNode.setProperty("nffgId", nffgId);
							frNode.createRelationshipTo(fsNode, RelationType.InfoRelationship);
								
							if (ffs.getIngPort() != null) {
								fsNode.setProperty("ingPort", ffs.getIngPort());
							}
								
							setMacHeadersInFlowspaceNode(ffs.getMac(), fsNode);
							setIpHeadersInFlowspaceNode(ffs.getIp(), fsNode);
							setTcpHeadersInFlowspaceNode(ffs.getTcp(), fsNode);
							setUdpHeadersInFlowspaceNode(ffs.getUdp(), fsNode);
						}
					}
				}
			}
			
			tx.success();
			return tmpId;
		}
		finally
		{
			tx.close();
		}
	}
	
	/**
	 * Save into the Neo4j database some new graphs.
	 * 
	 * @param graphs the list of graphs we want to save into the database.
	 * @throws MyConstraintViolationException if a validation error occurs during the process.
	 * @throws Exception in case of any other unpredictable errors.
	 * @see NffgSet
	 * @see MyConstraintViolationException
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public void createNffgs(NffgSet graphs) throws Exception
	{		
		for (Nffg nt : graphs.getNffg())
		{
			createNffg(nt);
		}
	}
	
	private EpCpType getEpCp(EpsCpsType epscps, Node frNode)
	{
		for (EpCpType epcp : epscps.getEpCp())
		{
			if (epcp.getIdRef().equals(frNode.getProperty("epcp-idRef")))
			{
				return epcp;
			}	
		}
		
		EpCpType tmp = obFactory.createEpCpType();
		tmp.setIdRef((String) frNode.getProperty("epcp-idRef"));
		epscps.getEpCp().add(tmp);
		
		return tmp;
	}
	
	private List<Integer> getAllNffgIds()
	{
		String id;
		List<Integer> ids;
		Transaction tx = graphDB.beginTx();
		
		try
		{	
			ids = new ArrayList<Integer>();
			ResourceIterator<Node> nodes = graphDB.findNodes(NodeType.Nffg);
			
			while (nodes.hasNext())
			{
				id = ((String) nodes.next().getProperty("id")).substring(new String("nffg_").length());
				ids.add(Integer.parseInt(id));
			}
			
			nodes.close();
			
			if (ids.size() > 1)
			{
				Collections.sort(ids);
			}
			
			tx.success();
			return ids;
		}
		catch (Exception e)
		{
			tx.failure();
			throw e;
		}
		finally
		{
			tx.close();
		}
	}
	
	private int getValidNffgId(String proposedId)
	{
		int propId;
		List<Integer> ids = getAllNffgIds();
		
		if (proposedId != null)
		{
			propId = Integer.parseInt(proposedId.substring(new String("nffg_").length()));
			
			if (!ids.contains(propId))
			{
				return propId;
			}
		}
		
		if (!ids.isEmpty())
		{
			if (ids.get(ids.size() - 1) == ids.size() - 1)
			{
				return ids.size();
			}
			
			for (int i = 0; i < ids.get(ids.size() - 1); i++)
			{
				if (!ids.contains(i))
				{
					return i;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Load some graphs from Neo4j database and create with them an instance of NffgSet.
	 * 
	 * @param ids array whose length is variable that contains the graphIds we want to retrieve.
	 * @return the instance of NffgSet that contains the graphs specified, 
	 * or all graphs if the first element of array parameter is equal to 'all'. 
	 * @throws Exception in case of any unpredictable errors.
	 * @see NffgSet
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public NffgSet retrieveNffgs(String ... ids) throws Exception
	{
		Nffg graph;
		NffgSet graphs = obFactory.createNffgSet();
		
		if (ids[0].equalsIgnoreCase("all"))
		{
			for (Integer nffgId : getAllNffgIds())
			{
				graph = retrieveNffg("nffg_" + nffgId);
				
				if (graph != null) 
				{
					graphs.getNffg().add(graph);
				}
			}
		}
		else
		{
			for (int i = 0; i < ids.length; i++)
			{
				graph = retrieveNffg("nffg_" + ids[i]);
				
				if (graph != null) 
				{
					graphs.getNffg().add(graph);
				}
			}
		}
		
		return graphs;
	}
	
	/**
	 * Load the graph specified by parameter id from Neo4j database and create with it an instance of Nffg.
	 * 
	 * @param id the graphId of the graph we want to retrieve.
	 * @return the instance of Nffg with the graph retrieved or null 
	 * if doesn't exist a graph whose id is equal to the passed one.
	 * @throws Exception in case of any unpredictable errors.
	 * @see Nffg
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public Nffg retrieveNffg(String id) throws Exception
	{
		Relationship tmpRel;
		EpType.Flowspace eft; EpType ept; NfType nft;
		SpecType spt; CiType cit; CtrlInterfacesType cits;
		Deployment dt; Image it; Cpu cpu; Memory mem;
		Storage sto; String[] tmpArray; Attributes atts;
		Attribute att; CpointsType cpts; CpType cpt; 
		PortType port; MonParamsType mpts; NeType net;
		EpsCpsType refs; EpCpType ref; FlowrulesType frt; 
		ActionsType acts; ActionType act; FlowrulesType.Flowspace fft; 
		Node otherNode, otherNode2, nffgNode, epNode, nfNode, neNode, mpNode;
		Nffg graph = obFactory.createNffg();
		Transaction tx = graphDB.beginTx();
		
		try
		{	
			nffgNode = graphDB.findNode(NodeType.Nffg, "id", id);
			
			if (nffgNode == null) 
			{
				tx.failure();
				return null;
			}
			
			graph.setId((String) nffgNode.getProperty("id"));
					
			if (nffgNode.hasProperty("version")) {
				graph.setVersion((String) nffgNode.getProperty("version"));
			}
			
			ResourceIterator<Node> eps = graphDB.findNodes(NodeType.Endpoint, "nffgId", id);
			graph.setEndpoints(obFactory.createEpointsType());
					
			while (eps.hasNext())
			{
				epNode = eps.next();
				ept = obFactory.createEpType();
				ept.setId((String) epNode.getProperty("id"));
				tmpRel = epNode.getSingleRelationship(RelationType.InfoRelationship, Direction.OUTGOING);
				eft = obFactory.createEpTypeFlowspace();
				
				if (tmpRel != null)
				{
					otherNode = tmpRel.getOtherNode(epNode);
					
					if (otherNode.hasProperty("ingPhysPort")) {
						eft.setIngPhysPort((String) otherNode.getProperty("ingPhysPort"));
					}
					
					if (otherNode.hasProperty("nodeId")) {
						eft.setNodeId((String) otherNode.getProperty("nodeId"));
					}
					
					eft.setMac(setMacHeadersInFlowspaceElement(otherNode));
					eft.setIp(setIpHeadersInFlowspaceElement(otherNode));
					eft.setTcp(setTcpHeadersInFlowspaceElement(otherNode));
					eft.setUdp(setUdpHeadersInFlowspaceElement(otherNode));
				}
				
				ept.setFlowspace(eft);
				graph.getEndpoints().getEndpoint().add(ept);
			}
			
			eps.close();
			ResourceIterator<Node> nfs = graphDB.findNodes(NodeType.NetworkFunction, "nffgId", id);
			graph.setNetworkFunctions(obFactory.createNfunctionsType());
			
			while (nfs.hasNext())
			{
				nfNode = nfs.next();
				nft = obFactory.createNfType();
				nft.setId((String) nfNode.getProperty("id"));
				nft.setFunctionalType((String) nfNode.getProperty("functionType"));
				spt = obFactory.createSpecType();
				cits = obFactory.createCtrlInterfacesType();
				cpts = obFactory.createCpointsType();
				mpts = obFactory.createMonParamsType();
				
				for (Relationship r : nfNode.getRelationships(Direction.OUTGOING, RelationType.InfoRelationship))
				{
					otherNode = r.getOtherNode(nfNode);
					
					if (otherNode.hasLabel(NodeType.Specification))
					{
						if (otherNode.hasProperty("deployment")) 
						{
							dt = obFactory.createSpecTypeDeployment();
							dt.setType((String) otherNode.getProperty("deployment"));
							spt.setDeployment(dt);
						}
							
						if (otherNode.hasProperty("image"))
						{
							it = obFactory.createSpecTypeImage();
							it.setUri((String) otherNode.getProperty("image"));
							spt.setImage(it);
						}
							
						cpu = obFactory.createSpecTypeCpu();
						
						if (otherNode.hasProperty("cpu-model")) {
							cpu.setModel((String) otherNode.getProperty("cpu-model"));			
						}
							
						if (otherNode.hasProperty("cpu-architecture")) {
							cpu.setArchitecture((String) otherNode.getProperty("cpu-architecture"));
						}
							
						if (otherNode.hasProperty("cpu-numCores")) {
							cpu.setNumCores((Short) otherNode.getProperty("cpu-numCores"));
						}
							
						if (otherNode.hasProperty("cpu-clockSpeed")) {
							cpu.setClockSpeed((String) otherNode.getProperty("cpu-clockSpeed"));
						}
							
						spt.setCpu(cpu);
						mem = obFactory.createSpecTypeMemory();
							
						if (otherNode.hasProperty("mem-type")) {
							mem.setType((String) otherNode.getProperty("mem-type"));
						}
							
						if (otherNode.hasProperty("mem-size")) {
							mem.setSize((String) otherNode.getProperty("mem-size"));
						}
							
						spt.setMemory(mem);
						sto = obFactory.createSpecTypeStorage();
							
						if (otherNode.hasProperty("sto-type")) {
							sto.setType((String) otherNode.getProperty("sto-type"));
						}
							
						if (otherNode.hasProperty("sto-size")) {
							sto.setSize((String) otherNode.getProperty("sto-size"));
						}
							
						spt.setStorage(sto);
						nft.setSpecification(spt);
					}
					else if (otherNode.hasLabel(NodeType.CtrlInterface))
					{
						cit = obFactory.createCiType();
						cit.setId((String) otherNode.getProperty("id"));
						atts = obFactory.createCiTypeAttributes();
							
						if (otherNode.hasProperty("attributes")) 
						{
							tmpArray = (String[]) otherNode.getProperty("attributes");
								
							for (int i = 0; i < tmpArray.length; i++)
							{
								att = obFactory.createCiTypeAttributesAttribute();
								att.setValue(tmpArray[i]);
								atts.getAttribute().add(att);
							}
						}
							
						cit.setAttributes(atts);
						cits.getControlInterface().add(cit);
					}
					else
					{
						setMonitoringParametersElement(mpts, otherNode);
					}
				}
				
				for (Relationship r : nfNode.getRelationships(Direction.BOTH, RelationType.PathRelationship))
				{
					otherNode = r.getOtherNode(nfNode);
					cpt = obFactory.createCpType();
					cpt.setId((String) otherNode.getProperty("id"));
					port = obFactory.createPortType();
					port.setId((int) otherNode.getProperty("port-id"));
					port.setDirection(PortDirEnumType.fromValue((String) otherNode.getProperty("port-direction")));
					
					if (otherNode.hasProperty("port-type")) {
						port.setType((String) otherNode.getProperty("port-type"));
					}
					
					cpt.setPort(port);
					cpts.getConnectionPoint().add(cpt);
				}

				nft.setSpecification(spt);
				nft.setControlInterfaces(cits);
				nft.setConnectionPoints(cpts);
				nft.setMonitoringParameters(mpts);
				graph.getNetworkFunctions().getNetworkFunction().add(nft);
			}
			
			nfs.close();
			ResourceIterator<Node> nes = graphDB.findNodes(NodeType.NetworkElement, "nffgId", id);
			graph.setNetworkElements(obFactory.createNelementsType());
			
			while (nes.hasNext())
			{
				neNode = nes.next();
				net = obFactory.createNeType();
				net.setId((String) neNode.getProperty("id"));
				net.setType((String) neNode.getProperty("type"));
				refs = obFactory.createEpsCpsType();
				mpts = obFactory.createMonParamsType();
				
				for (Relationship r : neNode.getRelationships(Direction.OUTGOING))
				{
					otherNode = r.getOtherNode(neNode);
					
					if (otherNode.hasLabel(NodeType.Flowrules))
					{
						ref = getEpCp(refs, otherNode);
						frt = obFactory.createFlowrulesType();
						fft = obFactory.createFlowrulesTypeFlowspace();
						acts = obFactory.createActionsType();

						for (Relationship ar : otherNode.getRelationships(Direction.OUTGOING))
						{
							otherNode2 = ar.getOtherNode(otherNode);
							
							if (otherNode2.hasLabel(NodeType.Action))
							{
								act = obFactory.createActionType();
								act.setType(ActionEnumType.fromValue((String) otherNode2.getProperty("type")));
								
								if (otherNode2.hasProperty("port")) {
									act.setPort((String) otherNode2.getProperty("port"));
								}
								
								acts.getAction().add(act);
							}
							else
							{
								if (otherNode2.hasProperty("ingPort")) {
									fft.setIngPort((String) otherNode2.getProperty("ingPort"));
								}
								
								fft.setMac(setMacHeadersInFlowspaceElement(otherNode2));
								fft.setIp(setIpHeadersInFlowspaceElement(otherNode2));
								fft.setTcp(setTcpHeadersInFlowspaceElement(otherNode2));
								fft.setUdp(setUdpHeadersInFlowspaceElement(otherNode2));
							}
						}
						
						frt.setFlowspace(fft);
						frt.setActions(acts);
						ref.getFlowrules().add(frt);
					}
					else
					{
						setMonitoringParametersElement(mpts, otherNode);
					}
				}

				net.setEpsCps(refs);
				net.setMonitoringParameters(mpts);
				graph.getNetworkElements().getNetworkElement().add(net);
			}
			
			nes.close();
			ResourceIterator<Node> mps = graphDB.findNodes(NodeType.MonitoringParameter, "nffgId", id);
			mpts = obFactory.createMonParamsType();
			
			while (mps.hasNext())
			{
				mpNode = mps.next();
				tmpRel = mpNode.getSingleRelationship(RelationType.InfoRelationship, Direction.INCOMING);
				
				if (tmpRel.getOtherNode(mpNode).hasLabel(NodeType.Nffg))
				{
					setMonitoringParametersElement(mpts, mpNode);
				}
			}
			
			mps.close();			
			graph.setMonitoringParameters(mpts);
			tx.success();
			
			return graph;
		}
		finally
		{
			tx.close();
		}
	}
	
	private void deleteNode(Node n)
	{
		n.getAllProperties().clear();
					
		for (Relationship r : n.getRelationships())
		{
			r.getAllProperties().clear();
			r.delete();
		}
					
		n.delete();	
	}
	
	/**
	 * Delete some graphs from Neo4j database.
	 * 
	 * @param ids array whose length is variable that contains the ids of the graphs we want to delete 
	 * (if the first element of the array is equal to 'all', all graphs saved into the database will be deleted).
	 * @throws Exception in case of any unpredictable errors.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public void deleteNffgs(String ... ids) throws Exception
	{		
		Node graph;
		Transaction tx = graphDB.beginTx();
		
		try
		{
			if (ids[0].equalsIgnoreCase("all"))
			{
				for (Node n : gcOperations.getAllNodes())
				{
					deleteNode(n);
				}
				
				tx.success();
			}
			else
			{
				for (int i = 0; i < ids.length; i++)
				{
					graph = graphDB.findNode(NodeType.Nffg, "id", ids[i]);
					
					if (graph != null)
					{
						for (Node n : gcOperations.getAllNodes())
						{
							if (n.hasProperty("nffgId") && n.getProperty("nffgId").equals(ids[i]))
							{
								deleteNode(n);
							}
						}
						
						deleteNode(graph);
					}	
				}
				
				tx.success();
			}
		}
		finally
		{
			tx.close();
		}
	}
	
	/**
	 * Delete the graph whose id is equal to parameter id, from the Neo4j database.
	 * 
	 * @param id the id of the graph we want to delete.
	 * @throws MyNotFoundException if there is no graph whose id is equal to the passed one.
	 * @throws Exception in case of any other unpredictable errors.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see MyNotFoundException
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public void deleteNffg(String id) throws Exception
	{	
		Node graph;
		Lock lock = null;
		Transaction tx = graphDB.beginTx();
		
		try
		{				
			graph = graphDB.findNode(NodeType.Nffg, "id", id);
			
			if (graph != null) 
			{
				lock = tx.acquireWriteLock(graph);
			}
			
			graph = graphDB.findNode(NodeType.Nffg, "id", id);
			
			if (graph != null)
			{
				for (Node n : gcOperations.getAllNodes())
				{
					if (n.hasProperty("nffgId") && n.getProperty("nffgId").equals(id))
					{
						deleteNode(n);
					}
				}
				
				deleteNode(graph);
				lock.release();
				tx.success();
			}
			else
			{
				tx.failure();
				throw new MyNotFoundException("There is no graph whose Id is '" + id.substring("nffg_".length()) + "'");
			}	
		}
		finally
		{
			tx.close();
		}
	}
	
	/**
	 * Calculates and returns the paths from a source node to a destination one within a given graph 
	 * that is specified by its id.
	 * 
	 * @param nffgId the id of the graph.
	 * @param srcNodeId the id of the source node.
	 * @param dstNodeId the id of the destination node.
	 * @param direction the direction considered in the calculation of paths (possible values are 'incoming', 
	 * 'outgoing' and 'both').
	 * @return a Set of String that represent the paths.
	 * @throws MyNotFoundException if there is no graph whose id is equal to the passed one, or if this problem 
	 * affects the source node or the destination one.
	 * @throws Exception in case of any other unpredictable errors.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Set.html">Set</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see MyNotFoundException
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
	 */
	public Set<String> findAllPaths(String nffgId, String srcNodeId, String dstNodeId, String direction) throws Exception
	{
		Transaction tx = graphDB.beginTx();
		
		try
		{
			if (graphDB.findNode(NodeType.Nffg, "id", nffgId) == null)
			{
				tx.failure();
				throw new MyNotFoundException("There is no graph whose Id is '" + nffgId.substring("nffg_".length()) + "'");
			}
			
			Node srcNode = graphDB.findNode(NodeType.Pathable, "uniqueProp", nffgId + "-" + srcNodeId);
			
			if (srcNode == null)
			{
				tx.failure();
				throw new MyNotFoundException("The node '" + srcNodeId + "', indicated as source node, doesn't exist within the graph");
			}
			
			Node dstNode = graphDB.findNode(NodeType.Pathable, "uniqueProp", nffgId + "-" + dstNodeId);
			
			if (dstNode == null)
			{
				tx.failure();
				throw new MyNotFoundException("The node '" + dstNodeId + "', indicated as destination node, doesn't exist within the graph");
			}
			
			direction = direction.toLowerCase();
			
			Set<String> paths_printed = new HashSet<String>();
			PathFinder<Path> finder = GraphAlgoFactory.allPaths(PathExpanders.forTypeAndDirection(RelationType.PathRelationship, Direction.valueOf(direction.toUpperCase())), MAX_DEPTH);
		    
		    for (Path p : finder.findAllPaths(srcNode, dstNode))
		    {
		    	paths_printed.add(Paths.simplePathToString(p, "id"));
		    }
		    
		    tx.success();
		    return paths_printed;
		}
		finally
		{
			tx.close();
		}
	}
}