/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import it.polito.nffg.neo4j.jaxb.HttpMessage;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;

/**
 * This class is used to map a 'captured' MyGenericException to an instance of class Response.
 * Within the response is contained an object of the JAXB annotated class HttpMessage.
 * 
 * @see MyGenericException
 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/ext/ExceptionMapper.html">ExceptionMapper</a>
 */
public class MyGenericExceptionMapper implements ExceptionMapper<MyGenericException>
{
	private ObjectFactory obFactory = new ObjectFactory();
	private HttpMessage response = obFactory.createHttpMessage();
	
	/**
	 * This method is automatically 'launched' when an instance of MyGenericException is 'captured' 
	 * and return the generated object of the Response class.
	 * 
	 *  @param mge the instance of MyGenericException class.
	 *  @return an object of the Response class that contains an object of HttpMessage class.
	 *  @see MyGenericException
	 *  @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.html">Response</a>
	 *  @see HttpMessage
	 */
	@Override
	public Response toResponse(MyGenericException mge) 
	{
		response.setStatusCode(mge.getStatus().getStatusCode());
		response.setReasonPhrase(mge.getStatus().getReasonPhrase());
		
		if (mge.getMessage() != null) 
		{
			response.setMessage(mge.getMessage());
		}
		
		return Response.status(mge.getStatus()).entity(response).build();
	}
}