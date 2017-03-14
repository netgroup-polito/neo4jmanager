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
 *	This exception is thrown when we need to send an HTTP Bad Request message to the client 
 *	because of a validation error.
 *
 *	@see MyGenericException
 */
public class MyConstraintViolationException extends MyGenericException
{
	private static final long serialVersionUID = 7455648209797668448L;

	/**
	 * Constructor method that initializes the message field with the passed argument.
	 * 
	 * @param message the detail message.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 */
	public MyConstraintViolationException(String message) 
	{
		super(message, Status.BAD_REQUEST);
	}
}
