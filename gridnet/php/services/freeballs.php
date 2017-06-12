<?php 
    require_once("location.inc");
    require_once("JSON.php");
    require_once("UserObjectHandler.php");
    $currentLongitude = $_POST['Longitude'] ? $_POST['Longitude'] : -97.856;
    $currentLatitude = $_POST['Latitude'] ? $_POST['Latitude'] : 30.2158;
    $distance = $_POST['Distance'] ? $_POST['Distance'] : 100;

    $json = new Services_JSON();
    $userObjectHandler = new UserObjectHandler();
    $ballArray = $userObjectHandler->findBalls($currentLatitude, $currentLongitude, $distance);
    echo $json->encode($ballArray);
?>
