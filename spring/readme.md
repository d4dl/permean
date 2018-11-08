
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


On my laptop the null writer will allow 170,000 cells/s with a maximum of 430,000 vertices/second


We'll err on the side of having streams capable of processing more than we thing we'll send.
So for 150,000 cells/s at 38 bytes each that would be 5500 KiB/s or 5.3 MiB/s
For vertexes at 350000 v/s at 8 bytes each would be 2740 KiB/s or 2.6 MiB/s 

Each shard can support writing 1000 records per second each one is rounded up to 1kb.
Based on that the cell stream should have 150000 shards and 
the vertex stream should have 350000 shards


HOWEVER
aggregation allows multiple cells be put into a single kinesis record

26 cells can fit into a 1MB kinesis record
128 vertices can fit into a 1MB kinesis record

So for cells it would be 5769 records / s
For vertices it would be 2734 records / s

Basically, it translates into MB/s
