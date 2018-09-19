<?php
require_once("serviceconfig.php"); 


function connect_to_db($host, $user, $pass, $dbName) {
  $link = mysqli_connect($host, $user, $pass, $dbName) or die("Error connecting: ");
  if (!link) {
    // couldn't connect
    echo "could not connect to mysql";
  }
  return $link;
}

function connect_to_RW_location_db() {
  return connect_to_db(DB_HOST, DB_WRITE_USER, DB_WRITE_PASS, DB_NAME);
}

function connect_to_RO_location_db() {
  return connect_to_db(DB_HOST, DB_READ_USER, DB_READ_PASS, DB_NAME);
}

function connect_to_ADMIN_location_db() {
  return connect_to_db(DB_HOST, DB_ADMIN_USER, DB_ADMIN_PASS, DB_NAME);
}

/**
 * Gets the last location of all the users
 * with the specified ids. This gets the locations
 * of the user wether the requesting user has permissions to 
 * or not.  This must be fixed.
 * the user the the requesting user is allowed to get
 *
 *  RIGHT NOW THIS ONLY WORKS FOR ONE USER!!!!!!!!!!
 *
 * ARG: comma separated list of user ids
 * RETURN: An array of Location objects
 */
function findLastLocations($dbLink, $userIds)
{

    $sql = sprintf("SELECT t3.username, t1.track_log_id, t1.latitude, t1.longitude, t1.time, t3.user_id
                    FROM   track_log t1, track t2, user t3
                    WHERE t1.track_id = t2.track_id
                    AND   t2.user_id = t3.user_id
                    AND   t1.track_log_id IN (SELECT MAX(t4.track_log_id)
                                              FROM track_log t4, track t5
                                              WHERE t4.track_id = t5.track_id
                                              AND t3.user_id IN (%s)
                                              GROUP BY t5.user_id)",
                   mysqli_escape_string($dbLink, $userIds));




    $result = mysqli_query($dbLink, $sql);
    $numRows = mysqli_num_rows($result);

    for ($i =0 ; $i < $numRows; $i++) 
    {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $locations[$i] = new Location($row["username"],
                                      $row["user_id"],
                                      $row["latitude"],
                                      $row["longitude"],
                                      $row["time"]
                                     );
    }
    return $locations;
}

/**
 * Finds all the lat,lon coords for the user's last track
 */
function findLastTrack($dbLink, $userId)
{
    $sql = "SELECT t1.latitude, t1.longitude
            FROM   track_log t1, track t2 
            WHERE t1.track_id = t2.track_id
            AND   t2.user_id = $userId
            AND t1.track_id = (SELECT MAX(t2.track_id)
                          FROM track t1, track_log t2
                          WHERE t1.user_id = $userId)
            ORDER BY time";
    $result = mysqli_query($dbLink, $sql);
    $numRows = mysqli_num_rows($result);

    for ($i =0 ; $i < $numRows; $i++) 
    {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $locations[$i] = new Location(null,
                                      $row["user_id"],
                                      $row["latitude"],
                                      $row["longitude"],
                                      $row["time"]
                                     );
    }
    return $locations;
}

function startTransaction($dbLink) {
    mysqli_query($dbLink, "BEGIN") or die("Error in begin: \n\n - ".mysqli_error($dbLink));
}
function commitTransaction($dbLink) {
    mysqli_query($dbLink, "COMMIT") or die("Error in commit: \n\n - ".mysqli_error($dbLink));
}
function rollbackTransaction($dbLink) {
    mysqli_query($dbLink, "ROLLBACK") or die("Error in rollback: \n\n - ".mysqli_error($dbLink));
}

/**
 * Called when a user grabs a ball.  Associates the ball with that
 * user. Returns all the balls the user possesses
 */
function userGrabsBall($dbLink, $userId, $ballId)
{
    $returnBall = null;
    mysqli_query($dbLink, "BEGIN");
    $sql = "SELECT possessor_id FROM ball WHERE ball_id = ".mysqli_escape_string($dbLink, $ballId)." FOR UPDATE";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $numRows = mysqli_num_rows($result);

    if ($numRows > 0) 
    {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $possessorId = $row["possessor_id"];
    }

    if($possessorId != null)
    {
        mysqli_query($dbLink, "ROLLBACK");
    }
    else
    {
        $sql = "UPDATE ball SET possessor_id = ".mysqli_escape_string($dbLink, $userId).
               " WHERE ball_id = ".mysqli_escape_string($dbLink, $ballId);
        mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
        mysqli_query($dbLink, "COMMIT") or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
        $returnBall = findBall($dbLink, $ballId);
    }
    return $returnBall;
}

/**
 * moves a ball to the new coordinates.  It is an error
 * if the specified user does not possess the ball.
 * This also updates the ball_track_log table
 * @param userId the user moving (or launching) the ball;
 */
function moveBall($dbLink, $userId, $ballId, $originalLatitude, $originalLongitude, $originalAltitude, $newLatitude, $newLongitude, $airTime)
{
    mysqli_query($dbLink, "BEGIN");
    $sql = "SELECT possessor_id FROM ball WHERE ball_id = ".mysqli_escape_string($dbLink, $ballId)."
            AND possessor_id = ".mysqli_escape_string($dbLink, $userId)." FOR UPDATE";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $numRows = mysqli_num_rows($result);

    if ($numRows != 1) 
    {
        echo "User '$userId' does not possess the ball.";
        mysqli_query($dbLink, "ROLLBACK");
        return null;
    } else {
        $rightNow = time();
        $landingTime = $rightNow + ($airTime * 1000);
        $sql = "INSERT INTO ball_track_log (user_id, ball_id, latitude, longitude, altitude, event_type, time) VALUES (".
                mysqli_escape_string($dbLink, $userId).", ".
                mysqli_escape_string($dbLink, $ballId).", ".
                mysqli_escape_string($dbLink, $originalLatitude).", ".
                mysqli_escape_string($dbLink, $originalLongitude).", ".
                mysqli_escape_string($dbLink, $originalAltitude).", ".
                "'launch', ".
                "FROM_UNIXTIME($rightNow))";
        mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));

        $sql = "INSERT INTO ball_track_log (user_id, ball_id, latitude, longitude, event_type, time) VALUES (".
                mysqli_escape_string($dbLink, $userId).", ".
                mysqli_escape_string($dbLink, $ballId).", ".
                mysqli_escape_string($dbLink, $newLatitude).", ".
                mysqli_escape_string($dbLink, $newLongitude).", ".
                "'landing', ".
                "FROM_UNIXTIME($landingTime))";
        $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
        $sql = sprintf("UPDATE ball SET possessor_id = NULL WHERE ball_id = %s", $ballId);
        $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
        mysqli_query($dbLink, "COMMIT");
    }

    return findBall($dbLink, $ballId);
}

/** Used to create users from other sites that are validated. **/
function createExternalUserForExternalID($dbLink, $externalUserID, $externalSourceID, $usernamePrefix)
{
    $userID = null;
    try {
        mysqli_query($dbLink, "BEGIN");
        $insertUserSQL = sprintf("INSERT INTO user (username, 
                                                    password, 
                                                    email, 
                                                    seclev, 
                                                    receiveReminders, 
                                                    status, 
                                                    createdDate)
                                  VALUES           ('autogen_".$usernamePrefix."_extId:".$externalUserID."', 
                                                    md5(NOW()),
                                                   'autogenemail_".$usernamePrefix."_extId:".$externalUserID."', 
                                                    0,
                                                    'no',
                                                    'enabled',
                                                    NOW())");
        mysqli_query($dbLink, $insertUserSQL) or die("Error in query: \n\n $insertUserSQL - ".mysqli_error($dbLink));
        $userID = mysqli_insert_id($dbLink);
        $sql = sprintf("INSERT INTO external_user (external_user_id,
                                                   external_source_id,
                                                   user_id,
                                                   createdDate)  
                        VALUES                     ('%s', '%s', $userID, NOW())",mysqli_escape_string($dbLink, $externalUserID), mysqli_escape_string($dbLink, $externalSourceID));
        mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
        mysqli_query($dbLink, "COMMIT") or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    } catch (Exception $e) {
        mysqli_query($dbLink, "ROLLBACK");
        return null;
    }
    return $userID;
}

function getFacebookIDForUserID($dbLink, $userID)
{
    $facebookIDs = getFacebookIDsForUserIDs($dbLink, array($userID));
    return $facebookIDs[0];
}

function getFacebookIDsForUserIDs($dbLink, $userIDs)
{
    $reducedUserIDs = reduce_with_quoted_commas($userIDs, $dbLink);
    $sql = sprintf("SELECT t1.external_user_id AS user_id
                    FROM external_user t1
                    WHERE t1.user_id IN (%s)
                    AND t1.external_source_id = 1",
        $reducedUserIDs);
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $facebookUIDs = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_array($result);
        $facebookUIDs[$i] = $row[0];
    }
    return $facebookUIDs;
}

function allotSlices($dbLink, $token, $slices)
{
    $allotmentCountAndId = getTokenRowIdAndCount($dbLink, $token);
    $allotmentCount = $allotmentCountAndId['allotmentCount'];
    $tokenId = $allotmentCountAndId['tokenId'];
    if($slices && count($slices) > 0) {
        while($allotmentCount > 0) {//Yes this is done twice on purpose.  The allotment count is looked up again when it hits 0
            $used = 0;
            while($allotmentCount > 0) {
                $allotmentCount--;
                $used++;
                $valueClause = "";
                for ($i=0 ; $i < count($slices); $i++) {
                    if($i > 0) {
                        $valueClause .= ",";
                    }
                    $slice = $slices[$i];
                    $valueClause .= "\n                    (".
                        "'".mysqli_escape_string($dbLink, $slice->uid)."',".
                        mysqli_escape_string($dbLink, $slice->top).",".
                        mysqli_escape_string($dbLink, $slice->bottom).",".
                        mysqli_escape_string($dbLink, $slice->west).",".
                        mysqli_escape_string($dbLink, $slice->east).")";
                }

                $relValueClause = "";
                $count = $i < count($slices);
                for ($i=0 ; $count; $i++) {
                    if($i > 0) {
                        $relValueClause .= ",";
                    }
                    $slice = $slices[$i];
                    $relValueClause .= "\n                    (".
                        mysqli_escape_string($dbLink, $tokenId).",".
                        "(SELECT geo_quadrangle_id FROM geo_quadrangle WHERE geo_quadrangle_uid = '".mysqli_escape_string($dbLink, $slice->uid)."'))";
                }
            }

            $sql = "UPDATE kickstarter_backer_token SET geo_quadrangle_allotment_used = $used where backer_token_id = $tokenId";
            $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
            $sql = "INSERT INTO geo_quadrangle  (geo_quadrangle_uid, top_latitude, bottom_latitude, west_longitude, east_longitude) VALUES $valueClause";
            $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
            $sql = "INSERT INTO kickstarter_backer_token_geo_quadrangle_rel (kickstarter_backer_token_id, geo_quadrangle_id) VALUES $relValueClause";
            $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));


            $allotmentCountAndId = getTokenRowIdAndCount($dbLink, $token);
            $allotmentCount = $allotmentCountAndId['allotmentCountAndId'];
            $tokenId = $allotmentCountAndId['tokenId'];
        }
    }
}

function getQuadranglesForKickstarterBacker($dbLink, $token)
{
    $sql = "SELECT  t1.geo_quadrangle_id id,
                    t1.geo_quadrangle_uid uid,
                    t1.top_latitude top,
                    t1.bottom_latitude bottom,
                    t1.east_longitude east,
                    t1.west_longitude west,
                    t3.backer_token
                    FROM geo_quadrangle t1
                    JOIN kickstarter_backer_token_geo_quadrangle_rel t2
                         ON  t1.geo_quadrangle_id = t2.geo_quadrangle_id
                    JOIN kickstarter_backer_token t3
                         ON t2.kickstarter_backer_token_id = t3.backer_token_id
                    WHERE t3.backer_token='".mysqli_escape_string($dbLink, $token)."'";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createGeoPolygons($dbLink, $result);
}

function getGeoPolygonAllotmentCount($dbLink, $token)
{
    $sql = "SELECT geo_quadrangle_allotment, geo_quadrangle_allotment_used FROM kickstarter_backer_token WHERE backer_token='".mysqli_escape_string($dbLink, $token)."' ORDER BY backer_token_id";
    //echo $sql;
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));

    $numRows = mysqli_num_rows($result);
    if($numRows == 0) {
        return null;
    }
    $allotment = 0;
    for ($i=0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_assoc($result);
        $row_allotment = $row['geo_quadrangle_allotment'];
        $allotment += $row_allotment;
        $row_allotment_used = $row['geo_quadrangle_allotment_used'];
        $allotment = $allotment - $row_allotment_used;
    }
    return $allotment;
}

//Looks at all the token rows in the backer token table and returns the first one that hasn't been all the way alloted.
function getTokenRowIdAndCount($dbLink, $token)
{
    $sql = "SELECT geo_quadrangle_allotment, geo_quadrangle_allotment_used, backer_token_id FROM kickstarter_backer_token WHERE backer_token='".mysqli_escape_string($dbLink, $token)."' ORDER BY backer_token_id";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));

    $numRows = mysqli_num_rows($result);
    if($numRows == 0) {
        return null;
    }
    $allotment = 0;
    for ($i=0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_assoc($result);
        $allotment = $row['geo_quadrangle_allotment'];
        $used = $row['geo_quadrangle_allotment_used'];
        $tokenId = $row['backer_token_id'];
        if($used < $allotment) {
            return array('allotmentCount'=>$allotment, 'tokenId'=>$tokenId);
        }
    }
    return null;
}


function getUserIDForExternalID($dbLink, $externalUserID, $externalSourceID)
{
    $userIDs = getUserIDsForExternalIDs($dbLink, array($externalUserID), $externalSourceID);
    return $userIDs[0];
}

function getUserIDsForExternalIDs($dbLink, $externalUserIDs, $externalSourceID)
{
    $reducedUserIDs = reduce_with_quoted_commas($externalUserIDs, $dbLink);
    $sql = sprintf("SELECT t1.user_id AS user_id
                    FROM external_user t1
                    WHERE t1.external_user_id IN (%s) 
                    AND t1.external_source_id = %s",
                   $reducedUserIDs, mysqli_escape_string($dbLink, $externalSourceID));
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $returnIDs = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_array($result);
        $returnIDs[$i] = $row[0];
    }
    return $returnIDs;
}

function reduce_with_commas($arrayToReduce, $dbLink) {
    $returnValue = "";
    $count = count($arrayToReduce);
    for ($i=0 ; $i < $count; $i++) {
        if($i > 0) {
            $returnValue .= ',';
        }
        $returnValue .= mysqli_escape_string($dbLink, $arrayToReduce[$i]);
    }
    return $returnValue;
}

function reduce_with_quoted_commas($arrayToReduce, $dbLink) {
    $returnValue = "";
    $count = count($arrayToReduce);
    for ($i=0 ; $i < $count; $i++) {
        if($i > 0) {
            $returnValue .= ',';
        }
        $returnValue .= "'".mysqli_escape_string($dbLink, $arrayToReduce[$i])."'";
    }
    return $returnValue;
}

/**
 * Retrieves the string values of all the permissions for the specified user.
 */
function globalUserPermissions($dbLink, $userID)
{
    $sql = sprintf("SELECT DISTINCT t3.label
                    FROM user t1, user_permission_rel t2, permission t3
                    WHERE t1.user_id = t2.user_id
                    AND t2.permission_id = t3.permission_id 
                    AND t1.user_id = %s
                    AND t3.domain='global'", mysqli_escape_string($dbLink, $userID));
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $permissions = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) 
    {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $permissions[$i] = $row["label"];
    }
    return $permissions;
}

/**
 * Retrieves all the balls possessed by the specified user.
 */
function getUserPossessedBalls($dbLink, $possessorId)
{
    $sql = sprintf("SELECT t1.latitude,
                           t1.longitude,
                           t1.time,
                           t2.name,
                           t2.possessor_id,
                           t2.ball_id,
                           t3.name as spec_name,
                           t3.mass,
                           t3.small_icon_url,
                           t3.icon_url,
                           t3.image_url,
                           t3.diameter,
                           t4.user_id as possessor_id,
                           t4.username as possessor_name,
                           t5.user_id as creator_id,
                           t5.username as creator_name
                    FROM ball_track_log t1, ball_spec t3, user t4, ball t2
                    LEFT OUTER JOIN user t5 ON t5.user_id = t2.possessor_id 
                    WHERE t1.ball_id = t2.ball_id 
                        AND t2.ball_spec_id = t3.ball_spec_id 
                        AND t2.creator_id = t5.user_id 
                        AND t4.user_id = %s
                        AND t1.ball_track_log_id =
                              (SELECT MAX(ball_track_log_id) from ball_track_log g1 WHERE g1.ball_id = t2.ball_id)
                        ORDER BY t1.ball_id", mysqli_escape_string($dbLink, $possessorId));
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createBalls($dbLink, $result);
}

/**
 * Calculates a GeoPolygon (a square) with a height and width of distance.
 */
function calculateGeoRectangle($latitude, $longitude, $distance) {
    $bearingNorth = 0;
    $bearingSouth = 180;

    $bearingEast = 270;
    $bearingWest = 90;

    $top =  calculateLocation($latitude, $longitude, $bearingNorth, $distance); 
    $bottom = calculateLocation($latitude, $longitude, $bearingSouth, $distance);

    $left = calculateLocation($latitude, $longitude, $bearingEast, $distance);
    $right = calculateLocation($latitude, $longitude, $bearingWest, $distance);

    $lat1 = $top->latitude < $bottom->latitude ? $top->latitude : $bottom->latitude;
    $lat2 = $top->latitude > $bottom->latitude ? $top->latitude : $bottom->latitude;

    $lon1 = $left->longitude < $right->longitude ? $left->longitude : $right->longitude;
    $lon2 = $left->longitude > $right->longitude ? $left->longitude : $right->longitude;
    return new GeoPolygon(null, $lat1, $lat2, $lon1, $lon2, null);
}


/**
 * Gets the ball with the specified id.
 */
function findBall($dbLink, $ballID)
{
    $id = mysqli_escape_string($dbLink, $ballID);
    $sql = sprintf("SELECT  t1.latitude, 
                            t1.longitude, 
                            t1.time, 
                            t2.name, 
                            t2.ball_id, 
                            t2.possessor_id as possessor_id, 
                            t2.creator_id as creator_id,
                            t4.username as creator_name,
                            t5.username as possessor_name,
                            t3.ball_spec_id, 
                            t3.name as spec_name, 
                            t3.mass, 
                            t3.small_icon_url,
                            t3.icon_url,
                            t3.image_url,
                            t3.diameter
                    FROM   ball_track_log t1, ball_spec t3, user t4, ball t2
                    LEFT OUTER JOIN user t5 ON t5.user_id = t2.possessor_id 
                    WHERE t1.ball_id = t2.ball_id
                    AND t2.ball_spec_id = t3.ball_spec_id
                    AND t1.ball_id = %s
                    AND t2.creator_id = t4.user_id
                    AND t1.ball_track_log_id =
                         (SELECT MAX(ball_track_log_id) from ball_track_log g1 WHERE g1.ball_id = t2.ball_id)",$id);
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $balls = createBalls($dbLink, $result);
    return $balls[0];
}

/**
 * Gets all the quadrangles that are in the quadrangle
 * defined by the  parameters
 */
function findGeoQuadrangles($dbLink, $topLatitude=11.7, $bottomLatitude=11.5,$westLongitude=46, $eastLongitude=47) {
    $sql = "SELECT DISTINCT cell.id cell_id, v2.id vertex_id, v2.latitude, v2.longitude
            FROM vertex v1
            JOIN cell_vertices cv1
              ON cv1.vertices_id = v1.id
            JOIN cell
              ON cv1.cell_id = cell.id
            JOIN cell_vertices cv2
              ON cv2.cell_id = cell.id
            JOIN vertex v2
              ON cv2.vertices_id = v2.id
            WHERE v1.latitude BETWEEN ".mysqli_escape_string($dbLink, $topLatitude)." AND ".mysqli_escape_string($dbLink, $bottomLatitude)."
            AND v1.longitude BETWEEN ".mysqli_escape_string($dbLink, $eastLongitude)." AND ".mysqli_escape_string($dbLink, $westLongitude);
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createGeoPolygons($dbLink, $result);
}

/**
 * Gets all the quadrangles that are in the quadrangle
 * defined by the  parameters
 */
function findGeoPolygons($dbLink, $topLatitude=11.7, $bottomLatitude=11.5,$westLongitude=46, $eastLongitude=47) {
    $sql = "SELECT  t1.geo_quadrangle_uid uid,
                    t1.top_latitude top,
                    t1.bottom_latitude bottom,
                    t1.east_longitude east,
                    t1.west_longitude west,
                    t3.backer_token
                    FROM geo_quadrangle t1
                    LEFT JOIN kickstarter_backer_token_geo_quadrangle_rel t2
                         ON  t1.geo_quadrangle_id = t2.geo_quadrangle_id
                    LEFT JOIN kickstarter_backer_token t3
                         ON t2.kickstarter_backer_token_id = t3.backer_token_id
                    WHERE t1.bottom_latitude <= ".mysqli_escape_string($dbLink, $topLatitude)."
                    AND t1.top_latitude >= ".mysqli_escape_string($dbLink, $bottomLatitude)."
                    AND t1.west_longitude <= ".mysqli_escape_string($dbLink, $eastLongitude)."
                    AND t1.east_longitude >= ".mysqli_escape_string($dbLink, $westLongitude)." ORDER BY cell.id, cv2.sequence";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createGeoPolygons($dbLink, $result);
}


/**
 * Gets all the quadrangles that are in the quadrangle
 * defined by the  parameters
 */
function findPoints($dbLink, $topLatitude, $bottomLatitude,$westLongitude, $eastLongitude) {
    $sql = "SELECT  t1.top_latitude top,
                    t1.east_longitude east
                    FROM geo_quadrangle t1
                    WHERE t1.bottom_latitude <= ".mysqli_escape_string($dbLink, $topLatitude)."
                    AND t1.top_latitude >= ".mysqli_escape_string($dbLink, $bottomLatitude)."
                    AND t1.west_longitude <= ".mysqli_escape_string($dbLink, $eastLongitude)."
                    AND t1.east_longitude >= ".mysqli_escape_string($dbLink, $westLongitude);
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));

    $points = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $points[$i] = array("lat"=>$row["top"], "lng"=>$row["east"]);
    }

    return $points;
}

/**
 * Gets all the quadrangles that are in the quadrangle
 * defined by the  parameters
 */
function findAllPoints($dbLink) {
    $sql = "SELECT  t1.top_latitude top,
                    t1.east_longitude east
                    FROM geo_quadrangle t1";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));

    $points = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $points[$i] = array("lat"=>$row["top"], "lng"=>$row["east"]);
    }

    return $points;
}


/**
 * Finds balls users have ever had interaction with.
 */
function findPastBalls($dbLink, $userID) {
    $sql = "SELECT  t1.latitude, 
                    t1.longitude, 
                    t1.time, 
                    t2.name, 
                    t2.ball_id, 
                    t2.possessor_id as possessor_id, 
                    t2.creator_id as creator_id,
                    t4.username as creator_name,
                    t3.ball_spec_id, 
                    t3.name as spec_name, 
                    t3.mass, t3.diameter,
                    t3.small_icon_url,
                    t3.icon_url,
                    t3.image_url,
                    FROM   ball_track_log t1, ball t2, ball_spec t3, user t4, ball_track_log t5
                    WHERE t1.ball_id = t2.ball_id
                    AND t2.ball_spec_id = t3.ball_spec_id
                    AND t2.creator_id = t4.user_id
                    AND t2.possessor_id is NULL
                    AND t5.ball_id = t1.ball_id
                    AND t5.user_id = ".  mysqli_escape_string($dbLink, $userID).
                   " AND t1.ball_track_log_id =
                         (SELECT MAX(ball_track_log_id) from ball_track_log g1 WHERE g1.ball_id = t2.ball_id)";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createBalls($dbLink, $result);
}


/**
 * Gets all the balls that don't have an possesssor.
function findFreeBalls($dbLink, $latitude = 30.2158, $longitude = -97.856, $distance = 100)
{
    $geoRect = calculateGeoRectangle($latitude, $longitude, $distance);
    $sql = "SELECT  t1.latitude, 
                    t1.longitude, 
                    t1.time, 
                    t2.name, 
                    t2.ball_id, 
                    t2.possessor_id as possessor_id, 
                    t2.creator_id as creator_id,
                    t4.username as creator_name,
                    t3.ball_spec_id, 
                    t3.name as spec_name, 
                    t3.mass, t3.diameter,
                    t3.small_icon_url,
                    t3.icon_url,
                    t3.image_url
                    FROM   ball_track_log t1, ball t2, ball_spec t3, user t4
                    WHERE t1.ball_id = t2.ball_id
                    AND t2.ball_spec_id = t3.ball_spec_id
                    AND t2.creator_id = t4.user_id
                    AND t2.possessor_id is NULL
                    AND   latitude BETWEEN ".$geoRect->top." AND ".$geoRect->bottom.
                  " AND   longitude BETWEEN ".$geoRect->west." AND ".$geoRect->east.
                  " AND t1.ball_track_log_id =
                         (SELECT MAX(ball_track_log_id) from ball_track_log g1 WHERE g1.ball_id = t2.ball_id)";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createBalls($dbLink, $result);
}
 */
function findFreeBalls($dbLink, $latitude = 30.2158, $longitude = -97.856, $distance = 100)
{
    $geoRect = calculateGeoRectangle($latitude, $longitude, $distance);
    $sql = "SELECT  t1.latitude, 
                    t1.longitude, 
                    t1.time, 
                    t2.name, 
                    t2.ball_id, 
                    t2.possessor_id as possessor_id, 
                    t2.creator_id as creator_id,
                    t4.username as creator_name,
                    t3.ball_spec_id, 
                    t3.name as spec_name, 
                    t3.mass, t3.diameter,
                    t3.small_icon_url,
                    t3.icon_url,
                    t3.image_url
                    FROM   ball_track_log t1, ball t2, ball_spec t3, user t4
                    WHERE t1.ball_id = t2.ball_id
                    AND t2.ball_spec_id = t3.ball_spec_id
                    AND t2.creator_id = t4.user_id
                    AND t2.possessor_id is NULL
                    AND t1.ball_track_log_id =
                         (SELECT MAX(ball_track_log_id) from ball_track_log g1 WHERE g1.ball_id = t2.ball_id)";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createBalls($dbLink, $result);
}


/**
 * Retrieves all the fieldIDs specified in fieldIDs that are part of
 * any transaction.  These then
 * shouldn't be included in any other transactions or already in the current transaction. This
 * ensures that two users can't both buy the same field.
 * @returns the fieldsIDs that can't be part of another transaction
 */
function getTransactionRestrictedFieldIDs($dbLink, $transactionID, $fieldIDs) {
    $restrictedFieldIDs = array();
    $reducedFieldIDs = reduce_with_commas($fieldIDs, $dbLink);
    $sql = "SELECT DISTINCT t1.geo_quadrangle_id
            FROM   stewardship_purchase_transaction_geo_quadrangle_rel t1,
                   stewardship_purchase_transaction t2
            WHERE  t1.stewardship_purchase_transaction_id = t2.stewardship_purchase_transaction_id
            AND    t1.geo_quadrangle_id IN ($reducedFieldIDs)
            AND    t2.defunct = 0";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) 
    {
        $row = mysqli_fetch_array($result);
        $restrictedFieldIDs[$i] = $row[0];
    }
    return $restrictedFieldIDs;
}

/**
 * Just returns the transaction id or creates a new transaction and returns
 * its id if there isn't one.
 */
function getOrCreateTransaction($dbLink, $transactionID, $userID) {
    if(!$transactionID) {
        $sql = "INSERT INTO stewardship_purchase_transaction (user_id, status) VALUES (".
                mysqli_escape_string($dbLink, $userID).",'standby')";
        $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
        $transactionID = mysqli_insert_id($dbLink);
    } 
    return $transactionID;
}

/**
 * Adds the fieldIDs to the transaction table with the specified transaction id.
 */
function addFieldsToStewardTransaction($dbLink, $transactionID, $fieldIDs, $userID) {
    if($fieldIDs && count($fieldIDs) > 0) {
        $valueClause = "";
        for ($i=0 ; $i < count($fieldIDs); $i++) {
            if($i > 0) {
                $valueClause .= ",";
            }
            $fieldID = $fieldIDs[$i];
            $valueClause .= "\n                    (".
                            mysqli_escape_string($dbLink, $transactionID).",".
                            mysqli_escape_string($dbLink, $fieldID).")";
        }
    }

    $sql = "INSERT INTO stewardship_purchase_transaction_geo_quadrangle_rel
                    (stewardship_purchase_transaction_id, geo_quadrangle_id) VALUES $valueClause";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
}


/**
 * Gets all the balls that don't have an possesssor.
 */
function saveBallSpecification($dbLink, $ballSpec)
{
    if($ballSpec->id) {
        $sql = "UPDATE ball_spec SET name = ".mysqli_escape_string($dbLink, $ballSpec->name).
                                  " mass = ".mysqli_escape_string($dbLink, $ballSpec->mass).
                                  " small_icon_url = ". mysqli_escape_string($dbLink, $ballSpec->smallIconURL).
                                  " icon_url = ". mysqli_escape_string($dbLink, $ballSpec->iconURL).
                                  " image_url = ". mysqli_escape_string($dbLink, $ballSpec->imageURL).
                                  " diameter = ". mysqli_escape_string($dbLink, $ballSpec->diamater).
               " WHERE id=".mysqli_escape_string($dbLink, $ballSpec->id);
    } else {
        $sql = "INSERT INTO ball_spec ball_spec_id,
                                  name,
                                  mass,
                                  small_icon_url,
                                  icon_url,
                                  image_url,
                                  diameter
                VALUES (". mysqli_escape_string($dbLink, $ballSpec->id).", ".
                           mysqli_escape_string($dbLink, $ballSpec->name).", ".
                           mysqli_escape_string($dbLink, $ballSpec->mass).", ".
                           mysqli_escape_string($dbLink, $ballSpec->smallIconURL).", ".
                           mysqli_escape_string($dbLink, $ballSpec->iconURL).", ".
                           mysqli_escape_string($dbLink, $ballSpec->imageURL).", ".
                           mysqli_escape_string($dbLink, $ballSpec->diamater).")";
    }
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createBallSpecifications($dbLink, $result);
}


/**
 * Gets all the balls that don't have an possesssor.
 */
function findAllBallSpecifications($dbLink)
{
    $sql = "SELECT t1.ball_spec_id,
                   t1.name as spec_name,
                   t1.mass,
                   t1.small_icon_url,
                   t1.icon_url,
                   t1.image_url,
                   t1.diameter
             FROM ball_spec t1";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    return createBallSpecifications($dbLink, $result);
}

/**
 * Retrives the ids of all the users that have an entry in the base table
 */
function getUsersHavingBases($dbLink, $userIDs) {
    $reducedUserIDs = reduce_with_quoted_commas($userIDs, $dbLink);
    $sql = "SELECT user_id 
            FROM home_base
            WHERE user_id IN ($reducedUserIDs)";
    $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
    $retVal = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_array($result);
        $retVal[$i] = $row[0];
    }
    return $retVal;
}

/**
 * Creates balls from the sql query result.  All expected location, specification and 
 * ball parameters must exist.
 */ 
function createBallSpecifications($dbLink, $result)
{
    $ballSpecifications = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) 
    {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $spec = new BallSpecification($row["ball_spec_id"], 
                                      $row["spec_name"], 
                                      $row["small_icon_url"], 
                                      $row["icon_url"], 
                                      $row["image_url"], 
                                      $row["mass"], 
                                      $row["diameter"]);
        $ballSpecifications[$i] = $spec;
    }
    return $ballSpecifications;
}

/**
 * Creates geo quadrangles from the result.
 * This should only be used to create small quadrangles
 * that are known never to be greater the 45º tall or 90º wide.
 * 
 */ 
function createGeoPolygons($dbLink, $result)
{
    $polygons = array();
    $numRows = mysqli_num_rows($result);
    $geometry = array();
    $lastCellId = null;
    $lastPolygon = null;
    for ($i =0 ; $i < $numRows; $i++) {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        $cellId = $row["cell_id"];
        if($cellId !== $lastCellId) {
            if($lastPolygon !== null) {
                $lastPolygon.setPolygon($geometry);
                $polygons[] = $lastPolygon;
            }
            $lastPolygon = new GeoPolygon($cellId, $row["backer_token"]);
            $geometry = [];
        }
        $geometry[] = [$row['latitude'], $row['longitude']];
    }
    if($lastPolygon !== null) {
        $lastPolygon.setPolygon($geometry);
        $polygons[] = $lastPolygon;
    }
    return $polygons;
}

function setStewardshipOnQuadrangles($dbLink, $topLatitude, $bottomLatitude, $westLongitude, $eastLongitude) {
}


/**
 * Creates balls from the sql query result.  All expected location, specification and 
 * ball parameters must exist.
 */ 
function createBalls($dbLink, $result)
{
    $balls = array();
    $numRows = mysqli_num_rows($result);
    for ($i =0 ; $i < $numRows; $i++) 
    {
        $row = mysqli_fetch_array($result, MYSQL_ASSOC);
        if($row["possessor_id"] ) {
            $possessor = new BallUser($row["possessor_id"], $row["possessor_name"]);
        }
        if($row["creator_id"]) {
            $creator = new BallUser($row["creator_id"], $row["creator_name"]);
        }
        $location = new Location(null,
                                 null,
                                 $row["latitude"],
                                 $row["longitude"],
                                 $row["time"]
                                  );
        $spec = new BallSpecification($row["ball_spec_id"], 
                                      $row["spec_name"], 
                                      $row["small_icon_url"], 
                                      $row["icon_url"], 
                                      $row["image_url"], 
                                      $row["mass"], 
                                      $row["diameter"]);
        $balls[$i] = new Ball($row["ball_id"], $location, $possessor, $creator, $row["name"], $spec); 
    }
    return $balls;
}

class BallSpecification
{
    var $id;
    var $name;
    var $smallIconURL;
    var $iconURL;
    var $imageURL;
    var $mass;
    var $diameter;

    function BallSpecification($id, $name, $smallIconURL, $iconURL, $imageURL, $mass, $diameter)
    {
        $this->id = $id;
        $this->name = $name;
        $this->smallIconURL = $smallIconURL;
        $this->iconURL = $iconURL;
        $this->imageURL = $imageURL;
        $this->mass = $mass;
        $this->diameter = $diameter;
    }
}

class Location
{
    var $username;
    var $userId;
    var $latitude;
    var $longitude;
    var $time;

    function Location($username, $userId, $lat, $lon, $time)
    {
        $this->username = $username;
        $this->userId = $userId;
        $this->latitude = $lat;
        $this->longitude = $lon;
        $this->time = $time;
    }

    function toString()
    {
	return "".$this->latitude.", ".$this->longitude;
    }
}

class Ball
{
    var $id;
    var $location;
    var $possessor;
    var $creator;
    var $name;
    var $specification;

    function Ball($id, $location, $possessor, $creator, $name, $specification)
    {
        $this->id = $id;
        $this->location = $location;
        $this->possessor = $possessor;
        $this->creator = $creator;
        $this->name = $name;
        $this->specification = $specification;
    }
}

class GeoPolygon
{
    var $id;
    var $polygon;

    //id is the db id
  function GeoPolygon($id, $backerToken) {
      $this->id = $id;
      $this->backerToken = $backerToken;
  }

    /**
     * @return mixed
     */
    public function getPolygon() {
        return $this->polygon;
    }

    /**
     * @param mixed $polygon
     */
    public function setPolygon($polygon) {
        $this->polygon = $polygon;
    }

}

class BallUser
{
  var $id;
  var $username;
    
  function BallUser($id, $username)
  {
        $this->username = $username;
        $this->id = $id;
  }

}

/*
 * Calculate destination point given start point (lat/long), bearing (d/m/s) & distance (in m).
 *
 * from: Vincenty direct formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the 
 *       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975
 *       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
 * param latitude in degrees
 * param longitude in degrees
 * param bearing in degrees
 * param distance in meters
 */
 

function calculateLocation($latitude, $longitude, $bearing, $distance) 
{
  $RADIAN_CONV = 0.017453292;
    
  $latitude = $latitude * $RADIAN_CONV;
  $longitude = $longitude * $RADIAN_CONV;
  $bearing = $bearing * $RADIAN_CONV;
  $ellipsoidA = 6378137;
  $ellipsoidB = 6356752.3142; 
  $ellipsoidF = 1/298.257223563;
  $sinAlpha1 = sin($bearing);
  $cosAlpha1 = cos($bearing);
  
  $tanU1 = (1-$ellipsoidF) * tan($latitude);
  $cosU1 = 1 / sqrt((1 + $tanU1*$tanU1)); 
  $sinU1 = $tanU1*$cosU1;
  $sigma1 = atan2($tanU1, $cosAlpha1);
  $sinAlpha = $cosU1 * $sinAlpha1;
  $cosSqAlpha = 1 - $sinAlpha*$sinAlpha;
  $uSq = $cosSqAlpha * ($ellipsoidA*$ellipsoidA - $ellipsoidB*$ellipsoidB) / ($ellipsoidB*$ellipsoidB);
  $A = 1 + $uSq/16384*(4096+$uSq*(-768+$uSq*(320-175*$uSq)));
  $B = $uSq/1024 * (256+$uSq*(-128+$uSq*(74-47*$uSq)));

  $sigma = $distance / ($ellipsoidB*$A);
  $sigmaP = 2*M_PI;
  $sinSigma = sin($sigma);
  $cosSigma = cos($sigma);
  $cos2SigmaM = cos(2*$sigma1 + $sigma);
  while (abs($sigma-$sigmaP) > 1e-12) 
  {
    $deltaSigma = $B*$sinSigma*($cos2SigmaM+$B/4*($cosSigma*(-1+2*$cos2SigmaM*$cos2SigmaM)-
      $B/6*$cos2SigmaM*(-3+4*$sinSigma*$sinSigma)*(-3+4*$cos2SigmaM*$cos2SigmaM)));
    $sigmaP = $sigma;
    $sigma = $distance / ($ellipsoidB*$A) + $deltaSigma;
  }

  $tmp = $sinU1*$sinSigma - $cosU1*$cosSigma*$cosAlpha1;
  $lat2 = atan2($sinU1*$cosSigma + $cosU1*$sinSigma*$cosAlpha1,
      (1-$ellipsoidF)*sqrt($sinAlpha*$sinAlpha + $tmp*$tmp));
  $lambda = atan2($sinSigma*$sinAlpha1, $cosU1*$cosSigma - $sinU1*$sinSigma*$cosAlpha1);
  $C = $ellipsoidF/16*$cosSqAlpha*(4+$ellipsoidF*(4-3*$cosSqAlpha));
  $L = $lambda - (1-$C) * $ellipsoidF * $sinAlpha *
      ($sigma + $C*$sinSigma*($cos2SigmaM+$C*$cosSigma*(-1+2*$cos2SigmaM*$cos2SigmaM)));
  
  //$revAz = atan2($sinAlpha, -$tmp);  // final bearing

  $finalLat = $lat2*180/M_PI;
  $finalLon = ($longitude+$L)*180/M_PI;
  return new Location(null, null, $finalLat, $finalLon, null);
}


/**
 * Inverse Vincenty Formula return value in meters.
 * This formula doesn't like having the same values for longitudes
 */
function calculateDistance($lat1,$lat2,$lon1,$lon2){
    $a = 6378137 - 21 * sin(lat);
    $b = 6356752.3142; 
    $f = 1/298.257223563;

    $p1_lat = $lat1/57.29577951;
    $p2_lat = $lat2/57.29577951;
    $p1_lon = $lon1/57.29577951;
    $p2_lon = $lon2/57.29577951;

    $L = $p2_lon - $p1_lon;

    $U1 = atan((1-$f) * tan($p1_lat));
    $U2 = atan((1-$f) * tan($p2_lat));

    $sinU1 = sin($U1);
    $cosU1 = cos($U1);
    $sinU2 = sin($U2);
    $cosU2 = cos($U2);

    $lambda = $L;
    $lambdaP = 2*PI;
    $iterLimit = 20;
 
    while(abs($lambda-$lambdaP) > 1e-12 && $iterLimit-->0) {
        $sinLambda = sin($lambda);
        $cosLambda = cos($lambda);
        $sinSigma = sqrt(($cosU2*$sinLambda) * ($cosU2*$sinLambda) + ($cosU1*$sinU2-$sinU1*$cosU2*$cosLambda) * ($cosU1*$sinU2-$sinU1*$cosU2*$cosLambda));
   
        //if ($sinSigma==0){return 0;}  // co-incident points
        $cosSigma = $sinU1*$sinU2 + $cosU1*$cosU2*$cosLambda;
        $sigma = atan2($sinSigma, $cosSigma);
        $alpha = asin($cosU1 * $cosU2 * $sinLambda / $sinSigma);
        $cosSqAlpha = cos($alpha) * cos($alpha);
        $cos2SigmaM = $cosSigma - 2*$sinU1*$sinU2/$cosSqAlpha;
        $C = $f/16*$cosSqAlpha*(4+$f*(4-3*$cosSqAlpha));
        $lambdaP = $lambda;
        $lambda = $L + (1-$C) * $f * sin($alpha) * ($sigma + $C*$sinSigma*($cos2SigmaM+$C*$cosSigma*(-1+2*$cos2SigmaM*$cos2SigmaM)));
    }

      if ($iterLimit<=0) {
          //echo "*****Hit the ITERATION LIMIT<br>";
          //return "Niks";  // formula failed to converge
      }

    $uSq = $cosSqAlpha*($a*$a-$b*$b)/($b*$b);
    $A = 1 + $uSq/16384*(4096+$uSq*(-768+$uSq*(320-175*$uSq)));
    $B = $uSq/1024 * (256+$uSq*(-128+$uSq*(74-47*$uSq)));
 
    $deltaSigma = $B*$sinSigma*($cos2SigmaM+$B/4*($cosSigma*(-1+2*$cos2SigmaM*$cos2SigmaM)- $B/6*$cos2SigmaM*(-3+4*$sinSigma*$sinSigma)*(-3+4*$cos2SigmaM*$cos2SigmaM)));
 
    $s = $b*$A*($sigma-$deltaSigma);
    return $s;
} 


/**
    THIS NO LONGER WORKS, DON'T USE IT.  LOOK AT THE FieldHandler
 */
function sliceTheEarth($dbLink, $sliceSize=7200) {
    //This comes out pretty closely to 20003931.457471
    $halfAMeridian = calculateDistance(0,90,90,89.0000000000001);//From equator to north pole, the other way will be exactly 
                                                                     //the same so do two inserts, one for -s and n hemisphere.
    $retVal = "Slicing the earth into $sliceSize sized slices";
    $retVal .= "The length of a meridian is $halfAMeridian";
    $longitudinalIncrement = $halfAMeridian/($sliceSize/4);

    //Start at the north pole and go to the south pole incrementing by longitudinal increment.
    $bottomLatitude = 0;
    $counter = 0;
    echo "Starting the slices incrementing on the meridians at $longitudinalIncrement<br>";
    $topLatitude = 0;
    $lastNumPieces = 0;
    $totalPoings = 0;
    $sqlINSERT = "INSERT INTO geo_quadrangle (bottom_latitude, top_latitude, west_longitude, east_longitude) VALUES";
    echo "Starting the loop";
    //Go down the prime meridian starting at the equator using the top latitude line and drawing a vertical line due south sliceSize meters long.
    for ($metersSouth=$longitudinalIncrement; $metersSouth < $halfAMeridian; $metersSouth += $longitudinalIncrement) {
    //for ($metersSouth=9965847; $metersSouth < $halfAMeridian; $metersSouth += $longitudinalIncrement) {
        $bottomLocation = calculateLocation(0, 90, 0, $metersSouth);
        $bottomLatitude = -($bottomLocation->latitude);
        $midwayLatitude = $bottomLatitude - (($bottomLatitude - $topLatitude)/2);
        $midwayCircumference = calculateDistance($midwayLatitude, $midwayLatitude, 0, 90)*2;

        $numPieces = floor($midwayCircumference / ($sliceSize/2));
        $arcDistance = 360 / ($numPieces+1);
        $lastLongitude = 0;
        $currentLongitude = $arcDistance;
        echo "\n\nGoing around $bottomLatitude, $metersSouth m south of the equator. 
             Pieces=$numPieces height=".calculateDistance($bottomLatitude, $topLatitude, 0,0.0000000001).
             "Top Width = ".calculateDistance($topLatitude, $topLatitude,0,$arcDistance). 
             "Bottom Width = ".calculateDistance($bottomLatitude, $bottomLatitude,0,$arcDistance);
        for($i=0; $i <= $numPieces; $i++) {//Go around the entire circumference.
            $sql = $sqlINSERT." (".($bottomLatitude < $topLatitude ? $bottomLatitude : $topLatitude).", ".
                     ($topLatitude > $bottomLatitude ? $topLatitude : $bottomLatitude).", ".
                     ($lastLongitude < $currentLongitude ? $lastLongitude : $currentLongitude).", ".
                     ($currentLongitude > $lastLongitude ? $currentLongitude : $lastLongitude).")";
            echo "\n".$sql;
            $result = mysqli_query($dbLink, $sql) or die("Error in query: \n\n$sql - ".mysqli_error($dbLink));
            $lastLongitude = $currentLongitude;
            if($i == ($numPieces - 1)) {
                $currentLongitude = 0;
            } else {
                $currentLongitude += $arcDistance;
                if($currentLongitude > 180) {
                    $currentLongitude = -(180 - ($currentLongitude - 180));
                }
            }
        }
        $topLatitude = $bottomLatitude;
    }
    return $retVal;
}


?>
