DROP TABLE IF EXISTS cell_vertices;
DROP TABLE IF EXISTS cell;
DROP TABLE IF EXISTS vertex;

CREATE TABLE cell (
  id          CHAR(36) NOT NULL,
  area        FLOAT       NOT NULL,
  parent_size INTEGER      NOT NULL,
  center_latitude FLOAT,
  center_longitude FLOAT,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE vertex (
  id        CHAR(36) NOT NULL,
  latitude  FLOAT       NOT NULL,
  longitude FLOAT       NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE cell_vertices (
  cell_id     CHAR(36) NOT NULL,
  vertices_id VARCHAR(255) NOT NULL,
  sequence    INTEGER      NOT NULL,
  PRIMARY KEY (cell_id, sequence)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

