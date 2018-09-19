<?php
require_once("location.inc");

class Validator
{
    /**
     * Per facebook documentation.
     * 1. Get all the parameters whose names start with fb_sig. 
     *    (Do not include the fb_sig parameter itself.) In Flex use 
     *    Application.application.parameters to do this.
     * 2. Strip the fb_sig_ prefix from all parameters, 
     *    and make sure the keys are lowercase.
     * 3. Create a string of the form param1=value1param2=value2param3=value3, 
     *    etc., sorted by the names (not the values) of the parameters. 
     *    Note: Do not use ampersands between the parameters.
     * 4. Separately pass this string and the fb_sig parameter 
     *    itself to your server, where your secret key is stored.
     * 5. On your server, append your application secret key to 
     *    the string that was passed in. The following is returned: 
     *    param1=value1param2=value2param3=value3myappsecret
     * 6. On your server, create an MD5 hash of this string.
     * 7. On your server, compare the generated hash with the 
     *    fb_sig parameter that was passed in. If they are equal, 
     *    then your Flash object was loaded by Facebook. 
     *    (Or by someone who stole your secret key.) 
     * In this case respond to the flash object with VALID or a similar code. 
     * If the signature is not valid, respond with INVALID. 
     * For testing send:
     * fbSig:
     *   cc79e2dabf8c3aaa45bb3e9a88198e1a
     * paramString:
     *   added=0api_key=952cb5dceeb9b25c0257a0105dae7deein_new_facebook=1locale=en_UStime=1231098474.0927
     */
    function validateFacebook($paramString, $fbSig)
    {
        $testMD5 = md5($paramString.FB_SATCH_SECRET);
        if($fbSig == $testMD5) {
            return "YOUWIN";
        } else {
            return "The facebook parameters are invalid";
        }
    }

    /**
     * This looks up the user id corresponding to the facebook uid.  It will validate that the request
     * has actually come from the facebook site and will create a user id if one does not exist.
     * For testing send:
     * fbSig:
     *   cc79e2dabf8c3aaa45bb3e9a88198e1a
     * paramString:
     *   added=0api_key=952cb5dceeb9b25c0257a0105dae7deein_new_facebook=1locale=en_UStime=1231098474.0927
     */
    function getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig)
    {
        $userID = null;
        if($this->validateFacebook($fbParamString, $fbSig)) {
            $userID = getUserIDForExternalID($dbLink, $facebookUID, FB_JIYOBA_EXT_ID);
            if($userID == null) {
                $userID = createExternalUserForExternalID($dbLink, $facebookUID, FB_JIYOBA_EXT_ID, "facebookJiyobaWatcher_");
            }
            return $userID;
        }
        return null;
    }

    /**
     * For testing send:
     * fbSig:
     *   cc79e2dabf8c3aaa45bb3e9a88198e1a
     * paramString:
     *   added=0api_key=952cb5dceeb9b25c0257a0105dae7deein_new_facebook=1locale=en_UStime=1231098474.0927
     */
    function getFacebookUserPermissions($facebookUID, $fbParamString, $fbSig) {
        $dbLink = connect_to_RW_location_db();
        $userID = $this->getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig);
        return globalUserPermissions($dbLink, $userID);
    }
}
?>
