/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.resources;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.polito.nffg.neo4j.exceptions.MyConstraintViolationException;
import it.polito.nffg.neo4j.exceptions.MyGenericException;
import it.polito.nffg.neo4j.exceptions.MyNotFoundException;
import it.polito.nffg.neo4j.jaxb.Nffg;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;
import it.polito.nffg.neo4j.jaxb.Paths;
import it.polito.nffg.neo4j.jaxb.Property;

/**
 * This class defines the methods that are mapped to HTTP request at path '/graphs/graphId'.
 */
public class GraphResource 
{	
	private String graphId;
	private String message;
	private ObjectFactory obFactory = GraphsResource.lib.getObjectFactory();
	private static Logger logger = Logger.getLogger(GraphResource.class.getCanonicalName());
	
	/**
	 * Constructor method that initializes graphId field with the value of the passed argument.
	 * 
	 * @param id the id of the graph taken from URL.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 */
	public GraphResource(String id)	
	{
		this.graphId = id;
	}
	
	/**
	 * Method associated with HTTP GET. It's used to retrieve a requested graph.
	 * 
	 * @return the requested graph.
	 * @throws MyNotFoundException if there is no graph whose id is equal to the passed one.
	 * @throws MyGenericException in case of any other unpredictable errors.
	 * @see Nffg
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/GET.html">@GET</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Produces.html">@Produces</a>
	 * @see MyNotFoundException
	 * @see MyGenericException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Nffg getGraph() throws MyGenericException
	{
		Nffg graph;
		
		try
		{
			graph = GraphsResource.lib.retrieveNffg("nffg_" + graphId);
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
			throw new MyGenericException();
		}
		
		if (graph == null) 
		{
			message = "There is no graph whose Id is '" + graphId + "'";
			logger.log(Level.INFO, message);
			
			throw new MyNotFoundException(message);
		}
		
		return graph;
	}
	
	/**
	 * Method associated with HTTP GET. It's used to retrieve all paths from a source node to a destination one 
	 * within the graph specified in the URL.
	 * 
	 * @param dir the direction considered in the calculation of paths (possible values are 'incoming', 
	 * 'outgoing' and 'both').
	 * @param src the id of the source node.
	 * @param dst the id of the destination node.
	 * @return the retrieved paths.
	 * @throws MyNotFoundException if there is no graph whose id is equal to the taken from URL one, or if this problem 
	 * affects the source node or the destination one.
	 * @throws MyConstraintViolationException if the value of direction isn't admissible.
	 * @throws MyGenericException in case of any other unpredictable errors.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/GET.html">@GET</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Path.html">@Path</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Produces.html">@Produces</a>
	 * @see Paths
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/DefaultValue.html">@DefaultValue</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/QueryParam.html">@QueryParam</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see MyNotFoundException
	 * @see MyConstraintViolationException
	 * @see MyGenericException
	 */
	@GET
	@Path("paths")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Paths getPaths(@DefaultValue("both") @QueryParam("dir") String dir, @DefaultValue("src_x") @QueryParam("src") String src, @DefaultValue("dst_y") @QueryParam("dst") String dst) throws MyGenericException
	{
		Set<String> paths;
		
		if (!dir.equals("incoming") && !dir.equals("outgoing") && !dir.equals("both"))
		{
			message = "The possible values for 'dir' are: 'incoming', 'outgoing' and 'both'";
			logger.log(Level.WARNING, message);
			
			throw new MyConstraintViolationException(message);
		}
		
		try
		{
			paths = GraphsResource.lib.findAllPaths("nffg_" + graphId, src, dst, dir);
		}
		catch (MyNotFoundException mnfe)
		{
			logger.log(Level.INFO, Status.NOT_FOUND.getReasonPhrase(), mnfe);
			throw mnfe;
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
			throw new MyGenericException();
		}
		
		Paths p = obFactory.createPaths();
		p.setSource(src);
		p.setDestination(dst);
		p.setDirection(dir);
		
		if (paths.isEmpty()) 
		{
			p.setMessage("No available paths");
		}
		else 
		{
			p.getPath().addAll(paths);
		}
		
		return p;
	}
	
	/**
	 * Method associated with HTTP GET. It's used to check whether a property on a given graph is satisfied or not. 
	 * For the moment, the only property supported is 'reachability'.
	 * 
	 * @param dir the direction considered to determine whether a destination node is reachable 
	 * from a source one or not (possible values are 'incoming', 'outgoing' and 'both').
	 * @param src the id of the source node.
	 * @param dst the id of the destination node.
	 * @return an object of the JAXB annotated HttpMessage class that contains the response.
	 * @throws MyNotFoundException if there is no graph whose id is equal to the taken from URL one, or if this problem 
	 * affects the source node or the destination one.
	 * @throws MyConstraintViolationException if the value of direction isn't admissible 
	 * or if this problem affect the property.
	 * @throws MyGenericException in case of any other unpredictable errors.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/GET.html">@GET</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Path.html">@Path</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Produces.html">@Produces</a>
	 * @see Property
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/DefaultValue.html">@DefaultValue</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/QueryParam.html">@QueryParam</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 * @see MyNotFoundException
	 * @see MyConstraintViolationException
	 * @see MyGenericException
	 */
	@GET
	@Path("property")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Property getProperty(@DefaultValue("reachability") @QueryParam("name") String prop, @DefaultValue("both") @QueryParam("dir") String dir, @DefaultValue("src_x") @QueryParam("src") String src, @DefaultValue("dst_y") @QueryParam("dst") String dst) throws MyGenericException
	{		
		if (!dir.equals("incoming") && !dir.equals("outgoing") && !dir.equals("both"))
		{
			message = "The possible values for 'dir' are: 'incoming', 'outgoing' and 'both'";
			logger.log(Level.WARNING, message);
			
			throw new MyConstraintViolationException(message);
		}
		
		switch (prop)
		{
			case "reachability":
			{
				Set<String> paths;
				
				try
				{
					paths = GraphsResource.lib.findAllPaths("nffg_" + graphId, src, dst, dir);
				}
				catch (MyNotFoundException mnfe)
				{
					logger.log(Level.INFO, Status.NOT_FOUND.getReasonPhrase(), mnfe);
					throw mnfe;
				}
				catch (Exception e)
				{
					logger.log(Level.SEVERE, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
					throw new MyGenericException();
				}
				
				Property p = obFactory.createProperty();
				p.setName(prop);
				p.setSource(src);
				p.setDestination(dst);
				p.setDirection(dir);
				p.setResponse((paths.isEmpty()) ? false : true);
				
				return p;
			}
			default:
			{
				message = "The only property verificable for the moment is 'reachability'";
				logger.log(Level.WARNING, message);
				
				throw new MyConstraintViolationException(message);
			}
		}
	}
	
	/**
	 * Method associated with HTTP DELETE. It's used to delete a graph.
	 * 
	 * @return an empty No_Content Response.
	 * @throws MyNotFoundException if there is no graph whose id is equal to the taken from URL one.
	 * @throws MyGenericException in case of any other unpredictable errors.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/DELETE.html">@DELETE</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.html">Response</a>
	 * @see MyNotFoundException
	 * @see MyGenericException
	 */
	@DELETE
	public Response deleteGraph() throws MyGenericException
	{		
		try
		{
			GraphsResource.lib.deleteNffg("nffg_" + graphId);
		}
		catch (MyNotFoundException mnfe)
		{
			logger.log(Level.INFO, Status.NOT_FOUND.getReasonPhrase(), mnfe);
			throw mnfe;
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
			throw new MyGenericException();
		}
		
		return Response.noContent().build();
	}
}