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