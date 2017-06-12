<?php
require_once("location.inc");
require_once("Validator.php");

class BallSpecificationHandler
{
    function findAll()
    {
        $dbLink = connect_to_RO_location_db();
        return findAllBallSpecifications($dbLink);
    }

    function saveBallSpecification($ballSpecification, $fbParamString, $fbSig)
    {
        $validator = new Validator();
        if($validator->validateFacebook($fbParamString, $fbSig)) {
            $dbLink = connect_to_RW_location_db();
            saveBallSpecification($dbLink, $ballSpecification);
        }
    }
}
?>
