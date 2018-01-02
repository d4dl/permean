ALTER TABLE cell_vertices ADD FOREIGN KEY (cell_id) REFERENCES cell(id);
ALTER TABLE cell_vertices ADD FOREIGN KEY (vertices_id) REFERENCES vertex(id);