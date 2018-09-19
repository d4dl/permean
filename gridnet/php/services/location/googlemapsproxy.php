<?php require_once("serviceconfig.php");
    $center = $_REQUEST['center'] ? $_REQUEST['center'] : "02.1,02.1";


    if($_REQUEST['span'])
    {
      $range = $_REQUEST['span'];
      $rangeParam = "span";
    }

    if($_REQUEST['zoom'] || !$range)
    {
      $range = $_REQUEST['zoom'] ? $_REQUEST['zoom'] : "3";
      $rangeParam = "zoom";
    }

    $size = $_REQUEST['size'] ? $_REQUEST['size'] : "256x256";
    $maptype = $_REQUEST['maptype'] ? $_REQUEST['maptype'] : "hybrid";
    $markers = $_REQUEST['markers'];
    $sensor =  $_REQUEST['sensor'] ? $_REQUEST['sensor'] : "true";
    $format = "jpg";
    $url =  $GMAPS_URL.
            "?center=".$center.
            "&$rangeParam=".$range.
            "&size=".$size.
            "&maptype=".$maptype.
            "&markers=".$markers.
            "&format=".$format.
            "&key=".$GMAPS_KEY.
            "&sensor=".$sensor;
$filename = $GMAPS_FILE_CACHE.md5($url).".".$format;

if (!file_exists($filename)) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_HEADER, 0);
    curl_setopt($ch, CURLOPT_BINARYTRANSFER, 1);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);

    // grab URL and pass it to the browser
    $data = curl_exec($ch);

    if($data) {
        //echo "<br>".$filename;
        $fh = fopen($filename, "w");
        fwrite($fh, $data);
        fclose($fh);
        chmod($filename, 0777);
    }
    curl_close($ch);
}
if (file_exists($filename)) {
    ob_clean();
    flush();
    readfile($filename);
    exit;
}

?>
