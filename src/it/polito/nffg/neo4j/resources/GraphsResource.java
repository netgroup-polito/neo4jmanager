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
package it.polito.nffg.neo4j.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import it.polito.nffg.neo4j.constraints.Graph;
import it.polito.nffg.neo4j.exceptions.MyConstraintViolationException;
import it.polito.nffg.neo4j.exceptions.MyGenericException;
import it.polito.nffg.neo4j.exceptions.MyNotFoundException;
import it.polito.nffg.neo4j.jaxb.HttpMessage;
import it.polito.nffg.neo4j.jaxb.Nffg;
import it.polito.nffg.neo4j.jaxb.NffgSet;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;
import it.polito.nffg.neo4j.manager.Neo4jLibrary;

/**
 * This class defines the methods that are mapped to HTTP request at path '/graphs'.
 *
 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Path.html">@Path</a>
 */
@Path("/graphs")
public class GraphsResource 
{
	@Context
	UriInfo uriInfo;
	private String message;
	static Neo4jLibrary lib = Neo4jLibrary.instance;
	private ObjectFactory obFactory = lib.getObjectFactory();
	private HttpMessage response = obFactory.createHttpMessage();
	private static Logger logger = Logger.getLogger(GraphsResource.class.getCanonicalName());
	
	/**
	 * Method associated with HTTP GET. It's used to retrieve all graphs.
	 * 
	 * @return the retrieved graphs.
	 * @throws MyNotFoundException if there are no available graphs.
	 * @throws MyGenericException in case of any other unpredictable errors.
	 * @see NffgSet
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/GET.html">@GET</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Produces.html">@Produces</a>
	 * @see MyNotFoundException
	 * @see MyGenericException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public NffgSet getGraphs() throws MyGenericException
	{
		NffgSet graphs;
		
		try
		{
			graphs = lib.retrieveNffgs("all");
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
			throw new MyGenericException();
		}
		
		if (graphs.getNffg().isEmpty()) 
		{
			message = "There are no available graphs";
			logger.log(Level.INFO, message);
			
			throw new MyNotFoundException(message);
		}
		
		return graphs;
	}
	
	/**
	 * Method associated with HTTP POST. It's used to create a graph.
	 * 
	 * @param graph the given graph.
	 * @return a response that contains ah HttpMessage object and the URL location.
	 * @throws MyConstraintViolationException if a validation error occurs during the process.
	 * @throws MyGenericException in case of any other unpredictable errors.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.html">Response</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/POST.html">@POST</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Consumes.html">@Consumes</a>
	 * @see Graph
	 * @see Nffg
	 * @see HttpMessage
	 * @see MyConstraintViolationException
	 * @see MyGenericException
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response createGraph(@Graph Nffg graph) throws MyGenericException
	{
		Integer nffgId;
		URI graphUri = null;
		String absPath = uriInfo.getAbsolutePath().toString();
		
		try
		{
			nffgId = lib.createNffg(graph);
		}
		catch (MyConstraintViolationException mcve)
		{
			logger.log(Level.WARNING, Status.BAD_REQUEST.getReasonPhrase(), mcve);
			throw mcve;
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
			throw new MyGenericException();
		}
				
		try 
		{
			graphUri = new URI(absPath + ((absPath.charAt(absPath.length() - 1) != '/') ? "/" + nffgId : nffgId));
			message = "The graph is now reachable at " + graphUri;
			response.setStatusCode(Status.CREATED.getStatusCode());
			response.setReasonPhrase(Status.CREATED.getReasonPhrase());
			response.setMessage(message);
		} 
		catch (URISyntaxException e) 
		{
			message = "A problem occurred during the generation of the URI, ";
			message += "but probably the creation of the graph was successful. ";
			message += "In this case, the graph has been saved with id '" + nffgId + "'";
			logger.log(Level.INFO, "URISyntaxException", e);
			
			throw new MyGenericException(message, Status.OK);
		}
			
		return Response.created(graphUri).entity(response).build();
	}
	
	/**
	 * Creates an instance of GraphResource class for manage HTTP request at path '/graphs/graphId'
	 * 
	 * @param id the graphId taken directly from URL.
	 * @return the created instance of GraphResource class.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/Path.html">@Path</a>
	 * @see GraphResource
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/PathParam.html">@PathParam</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 */
	@Path("{ graphId: [0-9]+ }")
	public GraphResource getGraph(@PathParam("graphId") String id)
	{
		return new GraphResource(id);
	}
}