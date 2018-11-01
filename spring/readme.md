
Long format mesh files contain vertexes with uuid keys.
The cells contain vertex arrays that refer to those vertex keys.
A small format file contains vertexes in order followed by cells containing vertex arrays that refer the the index of the corresponding vertex
A short format file can be generated from a long format file

`mvn package`
## Create a mesh file (long format)
`java  -Dsphere.divisions=25 -Xmx5G -Xmx15G -jar target/permean-0.0.1-SNAPSHOT-jar-with-dependencies.jar cells.json`
## Validate the mesh file can be read
`java -Dvalidate=true -Xmx5G -Xmx15G -cp target/permean-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.d4dl.permean.mesh.CellFileConverter cells.json`
## Create a short format mesh file from a long one
`java -Xmx5G -Xmx15G -cp target/permean-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.d4dl.permean.mesh.CellFileConverter cells.json cellShort.json`

## Create a kml file from a short format file
`java -Xmx5G -Xmx15G -cp target/permean-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.d4dl.permean.mesh.KMLWriter cellShort.json cellsShort.kml`

## Create a kml file from a short format file... but for only 18% of the cells
`java -Xmx5G -Xmx15G -Dinitiator18Only=true -cp target/permean-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.d4dl.permean.mesh.KMLWriter cellShort.json cellsShort.kml`
