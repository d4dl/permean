<?php
require_once("location.inc");
require_once("Validator.php");

class UserObjectHandler
{
    function getGlobalPermissions($userID)
    {
        $dbLink = connect_to_RO_location_db();
        return globalUserPermissions($dbLink, $userID, $dbLink);
    }

    /**
     * For testing send:
     * fbSig:
     *   cc79e2dabf8c3aaa45bb3e9a88198e1a
     * paramString:
     *   added=0api_key=952cb5dceeb9b25c0257a0105dae7deein_new_facebook=1locale=en_UStime=1231098474.0927
     */
    function getUserInfoForFacebookUser($facebookUID, $fbParamString, $fbSig) {
        $dbLink = connect_to_RW_location_db();
        $validator = new Validator();
        return $validator->getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig);
    }

    function findBalls($latitude = 30.2158, $longitude = -97.856, $distance = 100)
    {
        $dbLink = connect_to_RO_location_db();
        return findFreeBalls($dbLink, $latitude, $longitude, $distance);
    }

    function findUserBalls($facebookUID, $fbParamString, $fbSig)
    {
        $dbLink = connect_to_RO_location_db();
        $validator = new Validator();
        $userID = $validator->getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig);
        return getUserPossessedBalls($dbLink, $userID);
    }

    function findPastBalls($facebookUID, $fbParamString, $fbSig)
    {
        $dbLink = connect_to_RW_location_db();
        $validator = new Validator();
        $userID = $validator->getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig);
        return findPastBalls($dbLink, $userID);
    }
}
?>
