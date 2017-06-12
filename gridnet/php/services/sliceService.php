<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Max-Age: 3628800");
header("Access-Control-Allow-Methods: OPTIONS, POST, GET");
header("Access-Control-Allow-Headers: x-requested-with");
if($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit;
}
require_once("location.inc");
require_once("FieldHandler.php");

if($_REQUEST['action'] == "calculateFields") {
    $fieldHandler = new FieldHandler();
    $dbLink = connect_to_RW_location_db();
    $foundSlices = findGeoQuadrangles($dbLink, $_REQUEST['top'], $_REQUEST['bottom'], $_REQUEST['west'], $_REQUEST['east']);
    $skipFieldIds = array();
    foreach($foundSlices as $foundSlice) {
        array_push($skipFieldIds, $foundSlice->uid);
    }
    $fields = $fieldHandler->calculateFields($_REQUEST['top'], $_REQUEST['bottom'], $_REQUEST['west'], $_REQUEST['east'], $_REQUEST['sliceSize'], $skipFieldIds);
    $fields = array_merge($fields, $foundSlices);
    $output = json_encode($fields);
}


if($_REQUEST['action'] == "getAllPoints") {
    $fieldHandler = new FieldHandler();
    $dbLink = connect_to_RW_location_db();
    $foundPoints = findAllPoints($dbLink);
    $output = json_encode($foundPoints);
}

//There are 28,285,515 fields
if($_REQUEST['action'] == "countCalculatedFields") {
    $fieldHandler = new FieldHandler();
    $count = $fieldHandler->countCalculatedFields(90, 0, 0, 180, $_REQUEST['sliceSize']);
    $count += $fieldHandler->countCalculatedFields(-90, 0, -179.999999999999, 0, $_REQUEST['sliceSize']);
    $count += $fieldHandler->countCalculatedFields(90, -90, -179.999999999999, 0, $_REQUEST['sliceSize']);
    $count += $fieldHandler->countCalculatedFields(-90, 0, 0, 180, $_REQUEST['sliceSize']);
    $output = "Found $count fields";
}

if($_REQUEST['action'] == "submitToken") {
    $dbLink = connect_to_RW_location_db();
    startTransaction($dbLink);
    $backer_token = $_REQUEST['token'];
    $allotmentCount = getGeoQuadrangleAllotmentCount($dbLink, $backer_token);
    $outputObj = new stdClass();
    if($allotmentCount === null) {
        $outputObj->message = "That token doesn't exist.  You can back the project at kickstarter.com.  If this doesn't sound right.  Contact Joshua at the Kickstarter project page.";
    } else if($allotmentCount === 0) {
        $outputObj->message = "You have used all your backer tokens.  Email <a href='mailTo:jdeford@gmail.com'>Joshua</a> to get more tokens. ";
        $fieldHandler = new FieldHandler();
        $outputObj->slices = $fieldHandler->getQuadranglesForKickstarterBacker($backer_token);
    } else {
        $outputObj->message = "<p>You may choose to become the monitor for $allotmentCount region ".($allotmentCount > 1 ? "s" : "").".</p>".
              "<p>Zoom in until you see the region outlines.  To select a region click it while holding the ALT key down.</p>".
              "<p>Then <b>Add Selected Slices</b> to indicate that you'd like to be a monitor for those regions.</p>";


        $outputObj->allotmentCount = $allotmentCount;
        $fieldHandler = new FieldHandler();
        $outputObj->slices = $fieldHandler->getQuadranglesForKickstarterBacker($backer_token);
    }

    commitTransaction($dbLink);
    $output = json_encode($outputObj);
}


if($_REQUEST['action'] == "commitSlices") {
    $dbLink = connect_to_RW_location_db();
    startTransaction($dbLink);
    $slices = json_decode($_REQUEST['slices']);
    $token = $_REQUEST['token'];
    $allotmentCount = getGeoQuadrangleAllotmentCount($dbLink, $token);
    $outputObj = new stdClass();
    if($allotmentCount === null) {
        $outputObj->message = "You don't have any backer tokens.  You can back the project at kickstarter.com.  If this doesn't sound right.  Contact Joshua at the Kickstarter project page.";
    } else if($allotmentCount > 0 && $allotmentCount < count($slices)) {
        $outputObj->message = "Only $allotmentCount are alloted but you selected ".count($slices) .", If this doesn't sound right.  Contact Joshua at the Kickstarter project page.";
    } else if($allotmentCount === 0) {
        $outputObj->message = "You have used all your backer tokens.  Email <a href='mailTo:jdeford@gmail.com'>Joshua</a> to get more tokens. ";
    } else {
        allotSlices($dbLink, $token, $slices);
        $outputObj->allotmentCount = getGeoQuadrangleAllotmentCount($dbLink, $token);
        $outputObj->message = "<p>You are now the monitor for your chosen regions.</p><p>Use your backer token the next time you visit this site to view the regions.</p><p>You will be updated as the project progresses.</p>";
    }
    commitTransaction($dbLink);
    $output = json_encode($outputObj);
}

echo $output;

/*
delete from kickstarter_backer_token_geo_quadrangle_rel;
delete from geo_quadrangle;
delete from kickstarter_backer_token;
INSERT INTO kickstarter_backer_token (backer_token, geo_quadrangle_allotment, geo_quadrangle_allotment_used) VALUES('jdeford@troux.com', 1, 0);
INSERT INTO kickstarter_backer_token (backer_token, geo_quadrangle_allotment, geo_quadrangle_allotment_used) VALUES('jdeford@gmail.com', 1, 0);

*/

//<img src="http://www.gridocracy.net/php/services/location/googlemapsproxy.php"/>
?>

