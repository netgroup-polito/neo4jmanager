/*******************************************************************************
 * Copyright (c) 2017 Politecnico di Torino and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package it.polito.nffg.neo4j.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

public class Neo4jClient 
{	
	private static URI getBaseURI() 
	{
		return UriBuilder.fromUri("http://localhost:8080/neo4jmanager").build();
	}
	
	private static MediaType getMediaTypeFromString(String s)
	{		
		return (s.equalsIgnoreCase("application/json")) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
	}
	
	private static String readFile(String fileName) throws IOException 
	{
	    BufferedReader br = new BufferedReader(new FileReader(fileName));
	    
	    try 
	    {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) 
	        {
	            sb.append(line + "\n");
	            line = br.readLine();
	        }
	        
	        return sb.toString();
	    } 
	    finally 
	    {
	        br.close();
	    }
	}
	
	private static void writeFile(String content, String fileName) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));		
		bw.write(content);
		bw.close();
	}
	
	public static void main(String[] params)
	{
		ClientConfig config = new ClientConfig();
	    Client client = ClientBuilder.newClient(config);
	    WebTarget service = client.target(getBaseURI());
	    Response res = null; MediaType mt;
		
		if (params.length == 0)
		{
			System.err.println("For info about the service usage: java " + Neo4jClient.class.getCanonicalName() + " -help");
			System.exit(0);
		}    
		
		switch (params[0])
		{
			case "-create":
			{
				try
				{
					mt = getMediaTypeFromString(params[2]);
					res = service.path("rest/graphs").request().post(Entity.entity(readFile(params[1] + "." + mt.getSubtype()), mt));
				}
				catch (IOException e)
				{
					e.printStackTrace();
					System.exit(1);
				}
				
				System.out.println(res.readEntity(String.class));
				break;
			}
			case "-retrieve":
			{
				mt = getMediaTypeFromString(params[2]);
				res = service.path("rest/graphs").path((params[1].equals("all")) ? "" : params[1]).request(mt).get();
				
				if (res.getStatus() == Status.OK.getStatusCode())
				{
					try
					{
						writeFile(res.readEntity(String.class), params[3] + "." + mt.getSubtype());
						System.out.println("The graph" + ((params[1].equals("all")) ? "s have" : " has") + " been saved into the file " + params[3] + "." + mt.getSubtype());
					}
					catch (IOException e)
					{
						e.printStackTrace();
						System.exit(1);
					}
				}
				else
				{
					System.out.println(res.readEntity(String.class));
				}
				
				break;
			}
			case "-paths":
			{
				mt = getMediaTypeFromString(params[5]);
				res = service.path("rest/graphs").path(params[1]).path("paths").queryParam("src", params[2]).queryParam("dst", params[3]).queryParam("dir", params[4]).request(mt).get();
				
				if (res.getStatus() == Status.OK.getStatusCode())
				{
					try
					{
						writeFile(res.readEntity(String.class), params[6] + "." + mt.getSubtype());
						System.out.println("The paths have been saved into the file " + params[6] + "." + mt.getSubtype());
					}
					catch (IOException e)
					{
						e.printStackTrace();
						System.exit(1);
					}
				}
				else
				{
					System.out.println(res.readEntity(String.class));
				}
			
				break;
			}
			case "-reachability":
			{
				mt = getMediaTypeFromString(params[5]);
				res = service.path("rest/graphs").path(params[1]).path("property").queryParam("name", "reachability").queryParam("src", params[2]).queryParam("dst", params[3]).queryParam("dir", params[4]).request(mt).get();
				
				if (res.getStatus() == Status.OK.getStatusCode())
				{
					try
					{
						writeFile(res.readEntity(String.class), params[6] + "." + mt.getSubtype());
						System.out.println("The response has been saved into the file " + params[6] + "." + mt.getSubtype());
					}
					catch (IOException e)
					{
						e.printStackTrace();
						System.exit(1);
					}
				}
				else
				{
					System.out.println(res.readEntity(String.class));
				}
				
				break;
			}
			case "-delete":
			{
				res = service.path("rest/graphs").path(params[1]).request().delete();
				
				if (res.getStatus() == Status.NO_CONTENT.getStatusCode())
				{
					System.out.println("The graph has been deleted succesfully");
				}
				else
				{
					System.out.println(res.readEntity(String.class));
				}
				
				break;
			}
			case "-help":
			{
				try 
				{
					System.out.println(readFile("help.txt"));
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
					System.exit(1);
				}
				
				break;
			}
			default:
			{
				System.err.println("For info about the service usage: java " + Neo4jClient.class.getCanonicalName() + " -help");
			}
		}
	}
}