============
Neo4JManager
============

This document provides a description of Neo4JManager, a tool to easily interact with the **Neo4J** 
graph-oriented database to create, delete update service graphs. The tool allows also to query the
data-base in order to check the existing paths that connects two nodes of a selected graph.

.. contents::
   :depth: 3
   :local:


Development environment
-----------------------
For more info please download the report.pdf file, under the documentation folder:

- Windows 7 Ultimate 64 bit
- Eclipse Mars 4.5.1 64 bit
- Jdk 7u80 64 bit
- Tomcat 8.0
- Ant 1.9.7

Input
-----
Neo4JManager accepts service graph description in JSON and XML formats, by means of a RESTful API.
Full description of the data-structure description is available in ``[neo4jmanager]/documentation/Report.pdf``.
Example of JSON and XML files accepted by Neo4JManager are available in the ``[neo4jmanager]/xml-json`` folder

Output
------
Documentation about the Response messages and Status codes is available `here <http://localhost:8080/neo4jmanager>`_,
when the web service has been deployed and started.

Installation guide
------------------
Install Tomcat and configure Tomcat Manager by creating an user with at least the ``manager-script`` role. 
In order to enable this permission, open the ``[catalina_home]/conf/tomcat-users.xml`` and place the following content under the tomcat-users tag:
    
    ``<role rolename="root"/> 
    <user username="root" password="root" roles="tomcat,manager-gui, manager-script"/>``

Probably Tomcat Manager will need to increase the war file size limit defined in  ``[catalina_home]/webapps/manager/WEB-INF/web.xml``.

Update the Ant script ``[neo4jmanager]/tomcat-build.xml`` with the proper information about your Tomcat installation:

- Username
- Password
- Server location
- Server port

The Ant script ``[neo4jmanager]/build.xml`` helps in creating the war file and to deploy the web service in Tomcat.
Under the project directory (i.e., ``[neo4jmanager]``) run the following command:

- ant package-service
- ant deployWS

Client test
-----------
A simple Client for testing the main operations allowed by Neo4JManager is available under the ```[neo44manager]/src/it/polito/nffg/neo4j/client`` folder. To run the Client, the Ant script provides
a target for each method implemented by the web service. Otherwise, the Client can be run as a simple Java application.

**Get information about existing graph:**

- ``ant retrieve-nffg`` 
- ``java Neo4jClient retrieve -nffgId -mediaType -XMLName``

  where:

  - nffgId is a non negative number that identifies the graph ("all" for get all graphs)
  - mediaType is the chosen format for the response (application/xml o application/json)
  - XMLName is the name of the file in which the response will be saved
    
**Create new graph**

- ``ant create-nff``
- ``java Neo4jClient create -XMLFile -mediaType``

  where:

  - XMLFile is the name of the file that contains the graph to save into Neo4j DB

**Delete a graph**

- ``ant delete-nffg``
- ``java Neo4jClient delete -nffgId``

  where:

  - nffgId is a non negative number that identifies the graph

**Get all paths from source node to destination node**

- ``ant find-paths``
- ``java Neo4jClient paths -nffgId -srcNode -dstNode -direction -mediaType -XMLName``

  where:

  - srcNode is the ID that identifies the source node within the indicated graph
  - dstNode is the ID that identifies the destination node within the indicated graph
  - direction impact on the calculation of the paths (values: incoming, outgoing, both)

**Check if source node and destination node are reachable**

- ``ant valuate-reachability``
- ``java Neo4jClient -property -nffgId -srcNode -dstNode -direction -mediaType, -XMLName``

  where:
  
  - property is the property to check. The only value for the moment is 'reachability'


