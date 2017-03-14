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
 *	This exception is thrown when we need to send an HTTP Not Found message to the client.
 *
 *	@see MyGenericException
 */
public class MyNotFoundException extends MyGenericException
{
	private static final long serialVersionUID = -1337751234736465663L;

	/**
	 * Constructor method that initializes the message field with the passed argument.
	 * 
	 * @param message the detail message.
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/String.html">String</a>
	 */
	public MyNotFoundException(String message) 
	{
		super(message, Status.NOT_FOUND);
	}
}
