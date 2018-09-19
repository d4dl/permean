<?php
/**
 * Given a set of coordinates and a distance will return all the balls in a
 * rectangular area that are within the specified distance.  The distance
 * is calculated based on WGS-84 and is specified in meters using Vincenty's 
 * formula.
 *
 * Calculates the lat of the top and bottom of the square and and long 
 * of the left and right sides
 * a range query for the balls;
 */

    require_once("location.inc");
    require_once("JSON.php");
    
    connect_to_RO_location_db();


    $currentLongitude = $_POST['Longitude'] ? $_POST['Longitude'] : -97.856;
    $currentLatitude = $_POST['Latitude'] ? $_POST['Latitude'] : 30.2158;
    $distance = $_POST['Distance'] ? $_POST['Distance'] : 100;
    $geoRect = calculateGeoRectangle($currentLatitude, $currentLongitud, $distance) {

    $sql = "SELECT t4.user_id, t4.username, t1.latitude, t1.longitude, t1.time, t2.name, t2.ball_id, t2.possessor_id,
            t3.ball_spec_id, t3.name as spec_name, t3.mass, t3.diameter
            FROM  ball_track_log t1, ball t2, ball_spec t3
            LEFT OUTER JOIN user t4 ON t4.user_id = ball.possessor_id 
            WHERE t1.ball_id = t2.ball_id
            AND   t2.ball_spec_id = t3.ball_spec_id
            AND   latitude BETWEEN".$geoRect->latTop." AND ".geoRect->latBottom.
          " AND   longitude BETWEEN".$geoRect->lonWest." AND ".$geoRect->lonEast";
    echo $result;
    $result = mysql_query($sql) or die("Error in query: $sql - ".mysql_error());
    $balls = createBalls($result);

    $json = new Services_JSON();
    if($balls != null)
        echo $json->encode($balls);

?>
