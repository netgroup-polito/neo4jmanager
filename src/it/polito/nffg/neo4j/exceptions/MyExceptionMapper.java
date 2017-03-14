/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import it.polito.nffg.neo4j.jaxb.HttpMessage;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;

/**
 * This class is used to map a 'captured' Exception to an instance of class Response.
 * Within the response is contained an object of the JAXB annotated class HttpMessage.
 * 
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/io/Exception.html">Exception</a>
 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/ext/ExceptionMapper.html">ExceptionMapper</a>
 */
public class MyExceptionMapper implements ExceptionMapper<Exception>
{
	private Status status;
	private ObjectFactory obFactory = new ObjectFactory();
	private HttpMessage response = obFactory.createHttpMessage();
	
	/**
	 * This method is automatically 'launched' when an instance of Exception is 'captured' 
	 * and return the generated object of the Response class.
	 * 
	 *  @param e the instance of Exception class.
	 *  @return an object of the Response class that contains an object of HttpMessage class.
	 *  @see <a href="https://docs.oracle.com/javase/7/docs/api/java/io/Exception.html">Exception</a>
	 *  @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.html">Response</a>
	 *  @see HttpMessage
	 */
	@Override
	public Response toResponse(Exception e) 
	{
		if (e instanceof WebApplicationException)
		{
			return ((WebApplicationException) e).getResponse();
		}
		
		status = (e instanceof NumberFormatException) ? Status.BAD_REQUEST : Status.INTERNAL_SERVER_ERROR;
		response.setStatusCode(status.getStatusCode());
		response.setReasonPhrase(status.getReasonPhrase());
		
		if (e.getMessage() != null)
		{
			response.setMessage(e.getMessage());
		}
		
		return Response.status(status).entity(response).build();
	}
}