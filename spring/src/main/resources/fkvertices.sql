LOCK TABLES cell WRITE, cell_vertices WRITE;
ALTER TABLE cell_vertices ADD FOREIGN KEY (vertices_id) REFERENCES vertex(id);
UNLOCK TABLES;

