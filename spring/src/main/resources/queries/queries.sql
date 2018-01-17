select distinct count(v.id) from cell c join cell_vertices cv on cv.cell_id = c.id JOIN vertex v on v.id = cv.vertices_id group by c.id;



CREATE INDEX lat_idx ON vertex (latitude);
CREATE INDEX lnt_idx ON vertex (longitude);

-- Just get some coordinates
SELECT v1.latitude, v1.longitude
  FROM vertex v1
  WHERE v1.latitude BETWEEN 24.29324189 AND 24.29324191
  AND v1.longitude BETWEEN 70.84387674 AND 70.84387676;

-- Get some coordinates and join them to a cell_vertex
SELECT v1.latitude, v1.longitude, cv1.cell_id
  FROM vertex v1
  JOIN cell_vertices cv1
    ON cv1.vertices_id = v1.id
  WHERE v1.latitude BETWEEN 24.29324189 AND 24.29324191
  AND v1.longitude BETWEEN 70.84387674 AND 70.84387676;

-- Get some coordinates and join them to a cell_vertex and then all the way to the cell
SELECT v1.latitude lat, v1.longitude lng, cv1.cell_id cell
  FROM vertex v1
  JOIN cell_vertices cv1
    ON cv1.vertices_id = v1.id
  JOIN cell
    ON cv1.cell_id = cell.id
  WHERE v1.latitude BETWEEN 24.29324189 AND 24.29324191
  AND v1.longitude BETWEEN 70.84387674 AND 70.84387676;

-- Get some coordinates and join them to a cell_vertex and then all the way to the cell then join to get other cell_vertices
SELECT cv1.cell_id target_cell, cv2.vertices_id, v2.latitude lat1, v2.longitude lat2, cv2.sequence
  FROM vertex v1
  JOIN cell_vertices cv1
    ON cv1.vertices_id = v1.id
  JOIN cell
    ON cv1.cell_id = cell.id
  JOIN cell_vertices cv2
    ON cv2.cell_id = cell.id
  JOIN vertex v2
    ON cv2.vertices_id = v2.id
  WHERE v1.latitude BETWEEN 27.125300143532133 AND 27.16669602267853
  AND v1.longitude BETWEEN -32.07185468292238 AND -31.97014531707765;


-- Get some more coordinates and join them to a cell_vertex and then all the way to the cell then join to get other cell_vertices
SELECT cv1.cell_id target_cell, cv2.vertices_id, v2.latitude lat1, v2.longitude lat2, cv2.sequence
  FROM vertex v1
  JOIN cell_vertices cv1
    ON cv1.vertices_id = v1.id
  JOIN cell
    ON cv1.cell_id = cell.id
  JOIN cell_vertices cv2
    ON cv2.cell_id = cell.id
  JOIN vertex v2
    ON cv2.vertices_id = v2.id
  WHERE v1.latitude BETWEEN 24.29324189 AND 24.29324191
  AND v1.longitude BETWEEN 70.84387674 AND 70.84387676


SELECT
  cell.id,
  v2.id,
  cv2.sequence,
  vertices.latitude,
  vertices.longitude
FROM vertex target_vertices
JOIN cell_vertices cv1
  ON cv1.vertices_id = target_vertices.id
JOIN cell
  ON cv1.cell_id = cell.id
JOIN cell_vertices cv2
  ON cv2.cell_id = cell.id
JOIN vertex vertices
  ON cv2.vertices_id = vertices.id LIMIT 2;

SELECT
  cell.id,
  vertices.id,
  cv1.sequence,
  vertices.latitude,
  vertices.longitude
FROM cell
JOIN cell_vertices cv1
  ON cv1.cell_id = cell.id
JOIN vertex vertices
  ON cv1.vertices_id = vertices.id
WHERE cell.id IN (
'00000012-a9ae-4a63-ae07-3a4cb127aad2',
'0000002f-fa70-424f-aff6-b210305617d3',
'00000089-1293-4524-8ec2-8e2adf574434',
'000000a0-d33a-4f50-8c13-cae02a3c241f',
'000000e9-dc8f-4169-812e-fcf5fb2372b2',
'000000f8-8440-483c-b9ca-c190ee03435b',
'000000fd-c91a-4ced-9de2-c4985582fb53',
'0000012b-4633-4992-adb2-799c0c837674',
'000001ab-2a48-4a30-a933-13708a8327cb',
'000001b2-e438-42ef-b2aa-0cdadf9198ca'
);

SELECT
  cell.id,
  vertices.id,
  cv1.sequence,
  vertices.latitude,
  vertices.longitude
FROM cell
JOIN cell_vertices cv1
  ON cv1.cell_id = cell.id
JOIN vertex vertices
  ON cv1.vertices_id = vertices.id
WHERE cell.id IN (
'ffffffd2-737c-456d-8449-c7f8f674b1d3',
'ffffff85-7ec3-4475-83c1-6d1c5d40ae83',
'fffffeca-f604-4407-9093-4dac7756f5a5',
'fffffeab-0a53-4a91-a7f9-c7b396cc807f',
'fffffda5-4d2a-4346-bc3d-f6000f096f49',
'fffffd85-22d6-47c5-9dd2-bb271f36a979',
'fffffd68-a545-4cdb-b729-d00deb32b9ec',
'fffffd37-30d9-4b80-97cf-39ec5e35f300',
'fffffcbd-07ec-4fa0-aea1-a589de2bf1cd',
'fffffca6-a91c-46e4-bf48-c42da5aae685'
);



SELECT id from cell ORDER BY id desc LIMIT 10;

