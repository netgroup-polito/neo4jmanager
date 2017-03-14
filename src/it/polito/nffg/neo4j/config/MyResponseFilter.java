/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.config;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import it.polito.nffg.neo4j.jaxb.HttpMessage;
import it.polito.nffg.neo4j.jaxb.ObjectFactory;

/**
 * This class is used to intercept the responses and insert them into an HttpMessage object entity eventually.
 * 
 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/container/ContainerResponseFilter.html">ContainerResponseFilter</a>
 * @see HttpMessage 
 */
public class MyResponseFilter implements ContainerResponseFilter
{
	private ObjectFactory obFactory = new ObjectFactory();
	private HttpMessage response = obFactory.createHttpMessage();
	
	private MediaType getContentTypeForResponse(ContainerRequestContext req)
	{
		List<MediaType> mList = req.getAcceptableMediaTypes();
		
		if (!mList.isEmpty())
		{
			for (int i = 0; i < mList.size(); i++)
			{
				if (mList.get(i).toString().equals(MediaType.APPLICATION_JSON_TYPE.toString()) ||
					mList.get(i).toString().equals(MediaType.APPLICATION_XML_TYPE.toString()))
				{
					return mList.get(i);
				}	
			}
		}
		
		return null;
	}
	
	/**
	 * This method does what the class has been built for.
	 * 
	 * @throws IOException if an I/O exception occurs.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/container/ContainerRequestContext.html">ContainerRequestContext</a>
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/container/ContainerResponseContext.html">ContainerResponseContext</a>
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/io/IOException.html">IOException</a>
	 */
	@Override
	public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException 
	{		
		if (!res.hasEntity() && res.getStatusInfo().getStatusCode() != Status.NO_CONTENT.getStatusCode())
		{			
			if (getContentTypeForResponse(req) != null) 
			{
				res.getHeaders().add("content-type", getContentTypeForResponse(req));
			}
			
			response.setStatusCode(res.getStatusInfo().getStatusCode());
			response.setReasonPhrase(res.getStatusInfo().getReasonPhrase());
			res.setEntity(response);
		}
	}
}