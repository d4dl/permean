<?php 
/**
 * Calculates the distance a ball is thrown and moves it 
 * to that location. Although the distance calculation
 * takes the ellipsoid curvature of the earth into
 * account, the vector calculation for the travel
 * of the ball considers the ball to be travelling over
 * flat land.
 * The acceleration of gravity is taken to be 9.8 m/s^2
 * The initial velocity is determined by (force/mass)*time.
 * And everybody know it takes 1 second to throw a ball.
 */
    require_once("location.inc");
    require_once("JSON.php");
    $dbLink = connect_to_RW_location_db();
    $RADIAN_CONV = 0.017453292;


   $ballId = $_POST['BallId'] ? $_POST['BallId'] : 1;
   $userId =  $_POST['UserId'] ? $_POST['UserId'] : 2;
   $ballMass =  $_POST['BallMass'] ? $_POST['BallMass'] : 10;
   $force =  $_POST['Force'] ? $_POST['Force'] : 100;
   $accelerationDuration =  $_POST['AccelerationTime'] ? $_POST['AccelerationTime'] : 1;//Default to 1s
   $latitude = $_POST['Latitude'] ? $_POST['Latitude'] : 30;
   $longitude = $_POST['Longitude'] ? $_POST['Longitude'] : -97;
   $bearing = $_POST['Bearing'] ? $_POST['Bearing'] : 0;
   $bearingAngle = $bearing * $RADIAN_CONV;//Convert to radians
   $altitude = $_POST['Altitude'] ? $_POST['Altitude'] : 0.0;//This is the azimuth the parameter
   $azimuth = ($_POST['Azimuth'] ? $_POST['Azimuth'] : 45) * $RADIAN_CONV;

   $initialVelocity = calculateInitialVelocity($force, $accelerationDuration, $ballMass);
   $airTime = calculateAirTime($ballMass, $altitude, $initialVelocity);
   $distance = calculateHorizontalDistance($initialVelocity, $azimuth, $airTime);
   $newLocation = calculateLocation($latitude, $longitude, $bearingAngle, $distance);
   $ball = moveBall($dbLink, $userId, $ballId, $latitude, $longitude, $altitude, $newLocation->latitude, $newLocation->longitude, $airTime);


    $json = new Services_JSON();
    if($ball != null)
        echo $json->encode($ball);

function calculateInitialVelocity($force, $accelerationDuration, $ballMass) 
{
    return ($force * $accelerationDuration)/$ballMass;
}

//The ball goes from altitude 0 to 0 altitude wherever it lands
function calculateAirTime($ballMass, $initialVelocity, $azimuth)
{
    if($azimuth <= 0) {
        $azimuth = 1;
    }
    $verticalVelocity = $initialVelocity*sin($azimuth);
    $accelerationY = -9.80665;
    return  ($verticalVelocity*2)/$accelerationY;
}

function calculateHorizontalDistance($initialVelocity, $azimuth, $airTime)
{
    $horizontalVelocity = $initialVelocity*cos($azimuth);
    return ($horizontalVelocity * $airTime);
}

?>
