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
package it.polito.nffg.neo4j.constraints;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.dom.DOMResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import it.polito.nffg.neo4j.config.Neo4jApplication;
import it.polito.nffg.neo4j.jaxb.Nffg;

/**
 * Annotation whose purpose is to validate an object of the class Nffg using the Validator class.
 * 
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/annotation/Target.html">@Target</a>
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/annotation/Retention.html">@Retention</a>
 * @see <a href="https://docs.oracle.com/javaee/6/api/javax/validation/Constraint.html">@Constraint</a>
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { Graph.Validator.class })
public @interface Graph 
{
	String message() default "";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
	
	/**
	 * Class that defines the method used to validate a Nffg instance.
	 * 
	 * @see Graph
	 * @see Nffg
	 * @see <a href="https://docs.oracle.com/javaee/6/api/javax/validation/ConstraintValidator.html">ConstraintValidator</a>
	 */
	public class Validator implements ConstraintValidator<Graph, Nffg>
	{
		private String message;
		private Properties pr = Neo4jApplication.PropCache.getProp();
		private final String xsdPath = pr.getProperty("my.user.dir") + "/schema/" + pr.getProperty("schemaForValidating");
		private static Logger logger = Logger.getLogger(Validator.class.getCanonicalName());
		
		/**
		 * Initialize the validator in preparation for isValid calls. 
		 * More in details it initializes the message field with a generic validation error message.
		 * 
		 * @param graph instance of Graph annotation.
		 * @see Graph
		 */
		@Override
		public void initialize(Graph graph) 
		{
			message = "Server error during validation phase";
		}

		/**
		 * Implement the validation of the graph against a given XSD schema.
		 * 
		 * @param graph the graph to validate.
		 * @param context the context in which the constraint is evaluated.
		 * @return true is validation has been successful, otherwise false.
		 * @see Nffg
		 * @see <a href="https://docs.oracle.com/javaee/6/api/javax/validation/ConstraintValidatorContext.html">ConstraintValidatorContext</a>
		 */
		@Override
		public boolean isValid(Nffg graph, ConstraintValidatorContext context) 
		{
			if (!MyMarshal(graph, xsdPath))
			{
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
				
				return false;
			}
			
			return true;
		}
		
		private boolean MyMarshal(Nffg graph, String xsd)
		{
			try
			{
				JAXBContext jc = JAXBContext.newInstance("it.polito.nffg.neo4j.jaxb");
				Marshaller m = jc.createMarshaller();
				SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
				
				try
				{
					Schema schema = sf.newSchema(new File(xsd));
					m.setSchema(schema);
					
					m.setEventHandler(new ValidationEventHandler() 
					{
						@Override
						public boolean handleEvent(ValidationEvent ve) 
						{
							if (ve.getSeverity() != ValidationEvent.WARNING) 
							{
								message = ve.getMessage();
								logger.log(Level.SEVERE, message);
								
								return false;
							}
		                            
							return true;
						}
					});
				}
				catch (SAXException e) 
				{ 
					logger.log(Level.SEVERE, e.getClass().getName(), e);
	                return false;
	            }
				
	            m.marshal(graph, new DOMResult());
	            return true;
			}
			catch (MarshalException e) 
			{ 
	            logger.log(Level.SEVERE, e.getClass().getName(), e);
	            return false;
	        }
			catch (JAXBException e) 
			{
				logger.log(Level.SEVERE, e.getClass().getName(), e);
				return false;
			}
			catch (Exception e)
			{
				logger.log(Level.SEVERE, e.getClass().getName(), e);
				return false;
			}
		}
	}
}