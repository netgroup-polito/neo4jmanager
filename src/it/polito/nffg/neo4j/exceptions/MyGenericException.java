/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.exceptions;

import javax.ws.rs.core.Response.Status;

/**
 * Root of the hierarchy for custom exceptions. 
 * By register a mapper for this exception, it will work for other ones also.
 * 
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
 */
public class MyGenericException extends Exception
{
	private Status status;
	private static final long serialVersionUID = -8315982634672780740L;

	/**
	 * Constructor method without arguments. 
	 * By default it initializes the status field with value Status.INTERNAL_SERVER_ERROR.
	 */
	public MyGenericException() 
	{
		status = Status.INTERNAL_SERVER_ERROR;
	}

	/**
	 * Constructor method that initializes the message and status field with the relative passed arguments. 
	 * 
	 * @param message the detail message.
	 * @param status the HTTP status associated with this exception.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 */
	public MyGenericException(String message, Status status) 
	{
		super(message);
		this.status = status;
	}

	/**
	 * Getter method to obtain the value of the status field.
	 * 
	 * @return the HTTP status.
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.Status.html">Status</a>
	 */
	public Status getStatus() 
	{
		return status;
	}

	/**
	 * Setter method for the status field.
	 * 
	 * @param status the HTTP status associated with this exception. 
	 * @see <a href="https://jersey.java.net/nonav/apidocs/latest/jersey/javax/ws/rs/core/Response.Status.html">Status</a>
	 */
	public void setStatus(Status status) 
	{
		this.status = status;
	}
}