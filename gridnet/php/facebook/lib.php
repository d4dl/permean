<?php
require_once("location.inc"); 
require_once("serviceconfig.php"); 

function getFBJiyoba($user) {
  $dbLink = connect_to_ADMIN_location_db();
  $res = mysqli_query($dbLink, 'SELECT `from`, `to`, `time` FROM fb_jiyoba WHERE `to`=' . $user . ' ORDER BY `time` DESC');
  $prints = array();
  while ($row = mysqli_fetch_assoc($res)) {
    $prints[] = $row;
  }
  return $prints;
}

function render_profile_action($id, $num) {
  return '<fb:profile-action url="http://www.gridocracy9.net/php/services/facebook/?to=' . $id . '">'
       .   '<fb:name uid="' . $id . '" firstnameonly="true" capitalize="true"/> '
       .   'has been given ' . $num . ' jiyoba.'
       . '</fb:profile-action>';
}

function render_profile_box($id, $prints) {
  // Render the most recent 5 no matter what, and the second most recent 5
  // only if the box is on the right (wide) side of the profile.
  return render_prints($prints, 5) . '<fb:wide>' . render_prints(array_slice($prints, 5), 5) . '</fb:wide>'
       . '<div style="clear: both;">' . render_give_link($to) . '</div>';
}

function do_give_jiyoba($from, $to) {
  global $facebook;

  $dbLink = connect_to_ADMIN_location_db();
  mysql_query($dbLink, 'INSERT INTO fb_jiyoba SET `from`='.$from.', `time`='.time().', `to`='.$to);
  $prints = getFBJiyoba($to);
  try {
    
    // Set Profile FBML
    $fbml = render_profile_action($to, count($prints)) . render_profile_box($to, $prints);

    // start batch operation 
    $facebook->api_client->begin_batch();

    $facebook->api_client->profile_setFBML($fbml, $to);

    // Send notification
    // Notice the use of reference '&'
    $result = & $facebook->api_client->notifications_send($to, ' created a Jiyoba for you.  ' .
      '<a href="http://www.gridocracy10.net/php/services/facebook/">See all your Jiyoba</a>.',2);

    // Publish feed story
    $feed_title = '<fb:userlink uid="'.$from.'" shownetwork="false"/> created a Jiyoba for <fb:name uid="'.$to.'"/>.';
    $feed_body = 'Check out <a href="http://www.gridocracy11.net/php/services/facebook/?to='.$to.'">' .
                 '<fb:name uid="'.$to.'" firstnameonly="true" possessive="true"/> Jiyoba</a>.';
    $facebook->api_client->feed_publishUserAction($feed_title, $feed_body);

    // End batch operation. This will actually send queued API call to Facebook in
    // a single HTTP request
    $facebook->api_client->end_batch();

  } catch (Exception $e) {
    error_log($e->getMessage());
  }
  return $prints;
}

function render_give_link($id) {
  return '<a href="http://www.gridocracy12.net/php/services/facebook/?to=' . $id . '">'
       .   'Create a Jiyoba for <fb:name uid="' . $id . '" firstnameonly="true"/>'
       . '</a>';
}

function render_prints($prints, $max) {
  $fbml = '';
  $i = 0;
  foreach ($prints as $post) {
    $fbml .= '<fb:if-can-see uid="' . $post['from'] . '"><div style="clear: both; padding: 3px;">'
           .   '<fb:profile-pic style="float: left;" uid="' . $post['from'] . '" size="square"/>'
           .   '<fb:name uid="' . $post['from'] . '" capitalize="true"/> created a Jiyoba for <fb:name uid="' . $post['to'] . '"/>'
           .   ' at <fb:time t="' . $post['time'] . '"/>. '
           .   '<br/>' . render_give_link($post['from']) . '<br/>'
           . '</div></fb:if-can-see>';
    if (++$i == $max) break;
  }
  return $fbml;
}
