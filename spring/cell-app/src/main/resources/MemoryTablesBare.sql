CREATE DATABASE memory_plm CHARACTER SET latin1;
use memory_plm;

DROP TABLE IF EXISTS cell_vertices;
DROP TABLE IF EXISTS cell;
DROP TABLE IF EXISTS vertex;

CREATE TABLE cell (
  id          VARCHAR(36) NOT NULL,
  area        FLOAT       NOT NULL,
  parent_size INTEGER      NOT NULL,
  center_latitude FLOAT,
  center_longitude FLOAT
) ENGINE=MEMORY DEFAULT CHARSET=latin1;

CREATE TABLE vertex (
  id        VARCHAR(36) NOT NULL,
  latitude  FLOAT       NOT NULL,
  longitude FLOAT       NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=latin1;

CREATE TABLE cell_vertices (
  cell_id     VARCHAR(36) NOT NULL,
  vertices_id VARCHAR(36) NOT NULL,
  sequence    INTEGER      NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=latin1;




