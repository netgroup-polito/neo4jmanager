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
