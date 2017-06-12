<?php
require_once("location.inc");
include_once 'facebook/facebook.php';

/**
 * Wrapper for the facebook api to be accessed via amfphp.
 * When the peanut butter gets stale sometimes you just gotta
 * eat the jelly.
 */
class FacebookWrapper
{
    function getNonAppUserFriends($userID) 
    {
        $params = array();
        $dbLink = connect_to_RO_location_db();
        $allFriendsFBIDs = $this->getFacebookFriends($userID,$dbLink);
        //return $allFriendsFBIDs;
        if($allFriendsFBIDs) {
            $usersWithBases = getUsersHavingBases($dbLink, $allFriendsFBIDs);
            if(sizeof($usersWithBases) > 0) {
                $facebookIDsHavingBases = getFacebookIDsForUserIDs($dbLink, $usersWithBases);
                $facebookUserIDs = array_diff($allFriendsFBIDs, $facebookIDsHavingBases);
                //return $facebookIDsHavingBases;
                //return $usersWithBases;
                //return $facebookUserIDs;
                return $this->getFacebookUserInfos($facebookUserIDs);
            } else {
                return $this->getFacebookUserInfos($allFriendsFBIDs);
            }
        }
    }

    /**
     * Retrieves all the users that are friends of the specified user id.
     */
    function getAppUserFriends() 
    {
        include 'serviceconfig.php';
        $params = array();
        $facebook = new Facebook($api_key, $secret);
        $client = $facebook->api_client;

        return $client->call_method('facebook.friends.getAppUsers', $params);
    }

    function getFacebookFriends($userID,$dbLink) 
    {
        include 'serviceconfig.php';
        $params = array();
        $facebookID = getFacebookIDForUserID($dbLink, $userID);
        if($facebookID) {
            $params['uid'] = $facebookID;
            $facebook = new Facebook($api_key, $secret);
            $client = $facebook->api_client;

            return $client->call_method('facebook.friends.get', $params);
        } else {
            return null;
        }
    }

    /**
     */
    function callFacebook($method, $params) {
        $facebook = new Facebook($api_key, $secret);
        $client = $facebook->api_client;

        return $client->call_method($method, $params);
    }

    function getFacebookUserInfo($userID) {//, $fbParamString, $fbSig)
        $fbUIDs = array();
        $dbLink = connect_to_RO_location_db();
        $fbUIDs[0] = getFacebookIDForUserID($dbLink, $userID);
        $infoArray = $this->getFacebookUserInfos($fbUIDs);
        return $infoArray[0];
    }

    function getFacebookUserInfos($userFBIDs) {//, $fbParamString, $fbSig)
        include 'serviceconfig.php';
        //include 'Validator.php';
        //$validator = new Validator();
        //$userFBIDs = $validator->validateFacebok($fbParamString, $fbSig);
        $dbLink = connect_to_RO_location_db();
        $params = array();
        $params['uids'] = $userFBIDs;
        $params['fields'] = "first_name,name,last_name,pic_square";
        $facebook = new Facebook($api_key, $secret);
        $client = $facebook->api_client;

        $infoArray = $client->call_method("facebook.Users.getInfo", $params);
        return $infoArray;
    }
}
?>
