<?php 

    require_once("location.inc");
    require_once("JSON.php");
    $dbLink = connect_to_RO_location_db();
    $possessorID = $_POST['UserID'] ? $_POST['UserID'] : 2;
    $json = new Services_JSON();
    $ballArray = getUserPossessedBalls($dbLink, $possessorID);
    echo $json->encode($ballArray);
?>
