Development environment:

Windows 7 Ultimate 64 bit

Eclipse Mars 4.5.1 64 bit

Jdk 7u80 64 bit

Tomcat 8.0

For more info please download report.pdf file that is under documentation dir


--HOW TO USE THE CLIENT--

GET:
java Neo4jClient retrieve -nffgId -mediaType -XMLName

where:
-nffgId is a non negative number that identifies the graph ("all" for get all graphs)
-mediaType is the chosen format for the response (application/xml o application/json)
-XMLName is the name of the file in which the response will be saved

GET:
java Neo4jClient paths -nffgId -srcNode -dstNode -direction -mediaType -XMLName

where:
-srcNode is the ID that identifies the source node within the indicated graph
-dstNode is the ID that identifies the destination node within the indicated graph
-direction impact on the calculation of the paths (values: incoming, outgoing, both)

GET: java Neo4jClient -property -nffgId -srcNode -dstNode -direction -mediaType, -XMLName

where:
-property is the property to check. The only value for the moment is 'reachability'

POST:
java Neo4jClient create -XMLFile -mediaType

where:
-XMLFile is the name of the file that contains the graph to save into Neo4j DB

DELETE:
java Neo4jClient delete -nffgId