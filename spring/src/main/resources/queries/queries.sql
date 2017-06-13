select distinct count(v.id) from cell c join cell_vertices cv on cv.cell_id = c.id JOIN vertex v on v.id = cv.vertices_id group by c.id;



CREATE INDEX lat_idx ON vertex (latitude);
CREATE INDEX lnt_idx ON vertex (longitude);


SELECT DISTINCT cell.id, v2.id, cv2.sequence, v2.latitude, v2.longitude
  FROM vertex v1
  JOIN cell_vertices cv1
    ON cv1.vertices_id = v1.id
  JOIN cell
    ON cv1.cell_id = cell.id
  JOIN cell_vertices cv2
    ON cv2.cell_id = cell.id
  JOIN vertex v2
    ON cv2.vertices_id = v2.id
  WHERE v1.latitude BETWEEN 20.182909309648863 AND 20.182909309648863
  AND v1.longitude BETWEEN 30.895141547755717 AND 30.895141547755717
  ORDER BY cell.id, cv2.sequence
