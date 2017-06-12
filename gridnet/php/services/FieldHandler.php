<?php
require_once("location.inc");
require_once("Validator.php");

class FieldHandler
{
    /**
     * Looks up the quadrangles from the database given the specified bounds.
     */
    function findQuadrangles($topLatitude, $bottomLatitude, $westLongitude, $eastLongitude)
    {
        $dbLink = connect_to_RO_location_db();
        return findGeoQuadrangles($dbLink, $topLatitude, $bottomLatitude, $westLongitude, $eastLongitude);
    }

    function setStewardshipOnQuadrangles($quadrangles, $facebookUID, $fbParamString, $fbSig) {
        $validator = new Validator();
        $dbLink = connect_to_RW_location_db();
        $userID = $validator->getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig);
        return setStewardshipOnQuadrangles($quadrangles, $userID);
    }
    function getQuadranglesForKickstarterBacker($backerTokenId) {
        $dbLink = connect_to_RW_location_db();
        return getQuadranglesForKickstarterBacker($dbLink, $backerTokenId);
    }

    /**
     * @param sliceSize slice size in meters.  Default is 7200
     */
    function calculateFields($topBound, $bottomBound, $westBound, $eastBound, $sliceSize=7200, $fieldIdsToSkip) {
        $quadrangles = array();
        $index = 0;

        $lonArcLength = calculateLocation(0, 90, 0, $sliceSize/2)->latitude;//arc length is from equator to first slice.
        $slicesToSkip = floor($bottomBound / $lonArcLength);
        $numSlices = ceil(abs(($topBound - $bottomBound)/$lonArcLength));

        $GEObottomLatitude = $lonArcLength * ($slicesToSkip-1);
        for ($sliceNum = $slicesToSkip; $sliceNum <= ($numSlices + $slicesToSkip + 1); $sliceNum++) {
            $GEOtopLatitude = $lonArcLength * $sliceNum;
            if($GEObottomLatitude > 84.5 || $GEObottomLatitude < -85.1) {
                break;
            }

            $midwayLatitude = $GEOtopLatitude - (($GEOtopLatitude - $GEObottomLatitude)/2);
            $midwayCircumference = calculateDistance($midwayLatitude, $midwayLatitude, 0, 90)*2;
            $availablePieces = ceil($midwayCircumference / ($sliceSize/4));

            $latArcLength = 360 / ($availablePieces+1);
            $piecesToSkip = floor($westBound / $latArcLength);

            $GEOwestLongitude = $latArcLength * ($piecesToSkip - 1);
            $numPieces = ceil(($eastBound - $westBound)/$latArcLength);

            for($pieceNum = $piecesToSkip; $pieceNum <= ($numPieces + $piecesToSkip + 1); $pieceNum++) {
                $GEOeastLongitude = $latArcLength * $pieceNum;
                $fieldID = $this->calculateFieldID($GEOtopLatitude,
                                                   $GEObottomLatitude,
                                                   $GEOwestLongitude,
                                                   $GEOeastLongitude);
                if(!in_array($fieldID, $fieldIdsToSkip))  {
                    $quadrangles[$index++] = new GeoQuadrangle(null, $fieldID,
                                                               $GEOtopLatitude,
                                                               $GEObottomLatitude,
                                                               $GEOwestLongitude,
                                                               $GEOeastLongitude, null);
                }
                $GEOwestLongitude = $GEOeastLongitude;
                if($GEOeastLongitude > 180) {
                    $GEOeastLongitude = -(180 - ($GEOeastLongitude - 180));
                }
            }
            $GEObottomLatitude = $GEOtopLatitude;
        }
        return $quadrangles;
    }

    function countCalculatedFields($topBound, $bottomBound, $westBound, $eastBound, $sliceSize=7200) {
        $index = 0;
        echo "counting";

        $lonArcLength = calculateLocation(0, 90, 0, $sliceSize/2)->latitude;//arc length is from equator to first slice.
        $slicesToSkip = floor($bottomBound / $lonArcLength);
        $numSlices = ceil(abs(($topBound - $bottomBound)/$lonArcLength));

        $GEObottomLatitude = $lonArcLength * ($slicesToSkip-1);
        for ($sliceNum = $slicesToSkip; $sliceNum <= ($numSlices + $slicesToSkip + 1); $sliceNum++) {
            $GEOtopLatitude = $lonArcLength * $sliceNum;
            if($GEObottomLatitude > 84.5 || $GEObottomLatitude < -85.1) {
                break;
            }

            $midwayLatitude = $GEOtopLatitude - (($GEOtopLatitude - $GEObottomLatitude)/2);
            $midwayCircumference = calculateDistance($midwayLatitude, $midwayLatitude, 0, 90)*2;
            $availablePieces = ceil($midwayCircumference / ($sliceSize/4));

            $latArcLength = 360 / ($availablePieces+1);
            $piecesToSkip = floor($westBound / $latArcLength);

            $numPieces = ceil(($eastBound - $westBound)/$latArcLength);

            for($pieceNum = $piecesToSkip; $pieceNum <= ($numPieces + $piecesToSkip + 1); $pieceNum++) {
                $index++;
            }
            $GEObottomLatitude = $GEOtopLatitude;
        }
        echo "counted";
        return $index;
    }

    /**
     * For all the fields that are not a part of another transaction
     * this inserts the fieldIDs into the transaction table
     * @returns the fieldsIDs that can't be part of another transaction
     */
    function addFieldsToTransaction($transactionID, $fieldIDs, $facebookUID, $fbParamString, $fbSig) {
       $dbLink = connect_to_RW_location_db();
       $validator = new Validator();
       $userID = $validator->getUserIDFromFBJiyobaWatcherUserId($dbLink, $facebookUID, $fbParamString, $fbSig);

       mysqli_query($dbLink, "BEGIN");
       $transactionID = getOrCreateTransaction($dbLink, $transactionID, $userID);
       $restrictedFieldIDs = getTransactionRestrictedFieldIDs($dbLink, $transactionID, $fieldIDs);
       $fieldIDs = array_values(array_diff($fieldIDs, $restrictedFieldIDs));//Take the restricted ids out
       if(count($fieldIDs) > 0) {
           addFieldsToStewardTransaction($dbLink, $transactionID, $fieldIDs, $userID);
       }
       mysqli_query($dbLink, "COMMIT");
       return $restrictedFieldIDs;
    }

    function calculateFieldID($top, $bottom, $west, $east) {
       $stringRep = "$top,$bottom,$west,$east";
       return "0X".md5($stringRep);
    }
}
?>
