<?php

// the facebook client library
include_once 'facebook/facebook.php';

// some basic library functions
include_once 'lib.php';

// this defines some of your basic setup
include_once 'serviceconfig.php';

$facebook = new Facebook($api_key, $secret);
$facebook->require_frame();
$user = $facebook->require_login();

if (isset($_POST['to'])) {
  $prints_id = (int)$_POST['to'];
  $prints = do_give_jiyoba($user, $prints_id);
} else {
  if (isset($_GET['to'])) {
    $prints_id = (int)$_GET['to'];
  } else {
    $prints_id = $user;
  }
  $prints = getFBJiyoba($prints_id);
}

?>
<fb:tabs>  
  <fb:tab-item href='mainSWF.php' title='Planet Viewer' />  
  <fb:tab-item href='http://apps.facebook.com/yourapp/myphotos.php' title='Create Jiyoba' selected='true'/>  
  <fb:tab-item href='http://apps.facebook.com/yourapp/recentalbums.php' title='Status' />  
</fb:tabs>

<div >
  <h2>Hi <fb:name firstnameonly="true" uid="<?=$user?>" useyou="false"/>!</h2><br/>
  <a href="<?= $facebook->get_add_url() ?>">Put Jiyoba Watcher in your profile</a>, if you haven't already!
  <br>Select the 'Planet Viewer' tab to view the Jiyoba.
    <form method="post" >
<?php
      if ($prints_id != $user) {
        echo 'Do you want to create a Jiyoba for <fb:name uid="' . $prints_id . '"/>?';
        echo '<input type="hidden" name="to" value="' . $prints_id . '"/>';
      } else {
        echo '<br/>Check out your friends\' Jiyoba: ';
        echo '<fb:friend-selector idname="to"/>';
      }
?>
      <input value="Create Jiyoba" type="submit"/>
    </form>
  <hr/>
  These are <fb:name uid="<?= $prints_id ?>" possessive="true"/> Jiyoba:<br/>
  <?php echo render_prints($prints, 10); ?>
  <div style="clear: both;"/>
</div>
