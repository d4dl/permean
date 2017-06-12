<?php 

    require_once("location.inc");
    require_once("JSON.php");
    $dbLink = connect_to_RW_location_db();

    $ballId = $_POST['BallId'] ? $_POST['BallId'] : 2;
    $userId = $_POST['UserId'] ? $_POST['UserId'] : 1;

    $grabbedBall = userGrabsBall($dbLink, $userId, $ballId);
    if($grabbedBall) {
        $json = new Services_JSON();
        echo $json->encode($grabbedBall);
    }
?>
