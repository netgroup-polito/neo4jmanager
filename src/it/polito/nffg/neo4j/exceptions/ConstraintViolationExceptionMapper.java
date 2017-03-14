/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.exceptions;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import it.polito.nffg.neo4j.jaxb.HttpMessage;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;

/**
 * This class is used to map a 'captured' ConstraintViolationException to an instance of class Response.
 * Within the response is contained an object of the JAXB annotated class HttpMessage.
 * 
 * @see <a href="https://docs.oracle.com/javaee/6/api/javax/validation/ConstraintViolationException.html">ConstraintViolationException</a>
 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/ext/ExceptionMapper.html">ExceptionMapper</a>
 */
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> 
{
	private ObjectFactory obFactory = new ObjectFactory();
	private HttpMessage response = obFactory.createHttpMessage();
	
	/**
	 * This method is automatically 'launched' when an instance of ConstraintViolationException is 'captured' 
	 * and return the generated object of the Response class.
	 * 
	 *  @param cve the instance of ConstraintViolationException class.
	 *  @return an object of the Response class that contains an object of HttpMessage class.
	 *  @see <a href="https://docs.oracle.com/javaee/6/api/javax/validation/ConstraintViolationException.html">ConstraintViolationException</a>
	 *  @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.html">Response</a>
	 *  @see HttpMessage
	 */
	@Override
	public Response toResponse(ConstraintViolationException cve) 
	{
		response.setStatusCode(Status.BAD_REQUEST.getStatusCode());
		response.setReasonPhrase(Status.BAD_REQUEST.getReasonPhrase());
		
		if (cve.getConstraintViolations().iterator().hasNext())
		{
			String msg = cve.getConstraintViolations().iterator().next().getMessage();
			
			if (msg.equals("Server error during validation phase"))
			{
				response.setStatusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
				response.setReasonPhrase(Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
			}
			
			response.setMessage(msg);
		}
		
		return Response.status(Status.BAD_REQUEST).entity(response).build();
	}
}