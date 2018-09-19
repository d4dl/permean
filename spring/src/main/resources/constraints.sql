-- ALTER TABLE cell ADD PRIMARY KEY (id);
-- ALTER TABLE vertex ADD PRIMARY KEY (id);
-- ALTER TABLE cell_vertices ADD PRIMARY KEY (cell_id, sequence);

ALTER TABLE cell_vertices ADD FOREIGN KEY (cell_id) REFERENCES cell(id);
ALTER TABLE cell_vertices ADD FOREIGN KEY (vertices_id) REFERENCES vertex(id);

ALTER TABLE vertex ADD CONSTRAINT UK5monbyfs20x2wt22vh0eproo3 UNIQUE (latitude, longitude);
-- Unessecary becuase of the compound key on cell_id and sequence
-- CREATE INDEX cell_idx ON cell_vertices (cell_id);

-- CREATE INDEX lat_idx ON vertex (latitude);
CREATE INDEX lng_idx ON vertex (longitude);

CREATE INDEX cell_idx ON cell (center_longitude);
CREATE INDEX cell_idx2 ON cell (center_latitude, center_longitude);

