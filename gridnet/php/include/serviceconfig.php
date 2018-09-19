<?php
$GMAPS_KEY="ABQIAAAAcOWtlhpvSVzxAf3MZEef-BTC6ULEjUfdNjinQKe-zVyg3H9EfxSQivi9jSg0KiZ-Bhny6eMT-RNY5g";
$GMAPS_URL="http://maps.google.com/staticmap";
$GMAPS_FILE_CACHE="/usr/www/users/jdeford/GMAP_CACHE/";

#mysql -hdb119c.pair.com -ujdeford_27 -pZcsVhkeb jdeford_gridnet
# Add backer tokens
# INSERT INTO kickstarter_backer_token (backer_token, geo_quadrangle_allotment, geo_quadrangle_allotment_used) VALUES('jdeford@troux.com', 4, 0);

define("DB_NAME", "jdeford_gridnet");
define("DB_HOST", "db119c.pair.com");
define("DB_ADMIN_USER","jdeford_27");
define("DB_ADMIN_PASS","ZcsVhkeb");

define("DB_WRITE_USER","jdeford_27_w");
define("DB_WRITE_PASS","cBv7C8SH");

define("DB_READ_USER", "jdeford_27_r");
define("DB_READ_PASS", "6ud3c9cC");
$URL_ROOT= "http://www.gridocracy13.net/php/facebook";
$ROOT_LOCATION= "http://www.gridocracy14.net/";

#The public key for the facebook jiyoba watcher app
$FB_SATCH_PK="952cb5dceeb9b25c0257a0105dae7dee";
#The private key for the facebook jiyoba watcher app
$FB_SATCH_SECRET="0a1b65271a75800a7a3bfd3fde72b8e8";

#######################################################################################
########### Settings for the Facebook API #############################################
#######################################################################################
// Get these from http://developers.facebook.com
$api_key = $FB_SATCH_PK;
$secret  = $FB_SATCH_SECRET;
define("FB_SATCH_SECRET",$secret);
define("FB_JIYOBA_EXT_ID",1);
/* While you're there, you'll also want to set up your callback url to the url
 * of the directory that contains Footprints' index.php, and you can set the
 * framed page URL to whatever you want.  You should also swap the references
 * in the code from http://apps.facebook.com/footprints/ to your framed page URL. */

// The IP address of your database
$db_ip = $DB_HOST;

$db_user = $DB_WRITE_USER;
$db_pass = $DB_WRITE_PASS;

// the name of the database that you create for footprints.
$db_name = $DB_NAME;

/* create this table on the database:
CREATE TABLE `fb_jiyoba` (
  `from` int(11) NOT NULL default '0',
  `to` int(11) NOT NULL default '0',
  `time` int(11) NOT NULL default '0',
  KEY `from` (`from`),
  KEY `to` (`to`)
);

 */
#######################################################################################
#######################################################################################
#######################################################################################

#mysql -hdb119c.pair.com -ujdeford_27_r -p6ud3c9cC jdeford_gridnet
#mysql -hdb119c.pair.com -ujdeford_27 -pZcsVhkeb jdeford_gridnet
#mysql -hdb119c.pair.com -ujdeford_27 -pZcsVhkeb jdeford_gridnet
?>
