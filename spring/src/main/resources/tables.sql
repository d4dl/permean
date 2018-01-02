DROP TABLE IF EXISTS cell_vertices;
DROP TABLE IF EXISTS cell;
DROP TABLE IF EXISTS vertex;

CREATE TABLE cell (
  id          VARCHAR(255) NOT NULL,
  area        DOUBLE       NOT NULL,
  parent_size INTEGER      NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE vertex (
  id        VARCHAR(255) NOT NULL,
  latitude  DOUBLE       NOT NULL,
  longitude DOUBLE       NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE cell_vertices (
  cell_id     VARCHAR(255) NOT NULL,
  vertices_id VARCHAR(255) NOT NULL,
  sequence    INTEGER      NOT NULL,
  PRIMARY KEY (cell_id, sequence)
);

ALTER TABLE vertex ADD CONSTRAINT UK5monbyfs20x2wt22vh0eproo3 UNIQUE (latitude, longitude);

CREATE INDEX lat_idx ON vertex (latitude);
CREATE INDEX lnt_idx ON vertex (longitude);



