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
package it.polito.nffg.neo4j.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import it.polito.nffg.neo4j.exceptions.ConstraintViolationExceptionMapper;
import it.polito.nffg.neo4j.exceptions.MyExceptionMapper;
import it.polito.nffg.neo4j.exceptions.MyGenericExceptionMapper;

/**
 * This class is used by the Jersey servlet during the initialization phase to set/disable some features.
 * To obtain that, we need to add appropriate information into the web.xml file.
 */
public class Neo4jApplication extends ResourceConfig
{
	/**
	 * Constructor method for:
	 * enable the sending of bean validation error messagges to the client; 
	 * register the class ConstraintViolationExceptionMapper; 
	 * register the class MyGenericExceptionMapper; 
	 * register the class MyResponseFilter;
	 * register the class MyExceptionMapper
	 * enable indentation for json output.
	 */
    public Neo4jApplication() 
    {
    	property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    	register(MyResponseFilter.class);
    	register(MyExceptionMapper.class);
    	register(ConstraintViolationExceptionMapper.class);
    	register(MyGenericExceptionMapper.class);
        register(new MoxyJsonConfig().setFormattedOutput(true).resolver());
    }
   
    /**
     * The PropCache is an enumeration with only one possible value,
     * then is a natural singleton. This feature is guaranteed by the JVM.
     * It contains the properties of the server loaded from a the file server.properties. 
     */
    public enum PropCache
    {
    	instance;
    	
    	private BufferedReader br;
    	private static Properties prop;
    	
    	private PropCache()
    	{
    		try 
            {
    			loadFileProperties();
    		} 
            catch (Exception e) 
            {
    			e.printStackTrace();
    		}
    	}
    	
    	private void loadFileProperties() throws IOException 
        {     	
        	prop = new Properties();
        	String neo4jDeploymentFolder = System.getProperty("catalina.home") + "/webapps/neo4jmanager";
        	prop.setProperty("my.user.dir", neo4jDeploymentFolder);
        	br = new BufferedReader(new FileReader(prop.getProperty("my.user.dir") + "/server.properties"));
        	prop.load(br);
        	br.close();
        }
    	
    	/**
    	 * Getter method to obtain the instance of Properties initialized in the private constructor of the cache.
    	 * 
    	 * @return the cache that contains the properties.
    	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html">Properties</a>
    	 */
    	public static Properties getProp()
        {
        	return prop;
        }
    }
}