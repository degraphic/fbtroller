<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
  <head>
	<link href="//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.1/css/bootstrap-combined.min.css" rel="stylesheet">
	<script src="//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.1/js/bootstrap.min.js"></script>    
 	<script src="http://code.jquery.com/jquery-latest.js"></script>
 	<script src="/js/knockout-2.2.0.js"></script>
  </head>

  <body>
	<div id="fb-root"></div>

	<script>
	  window.fbAsyncInit = function() {
	    // init the FB JS SDK
	    FB.init({
	      appId      : '168437049966549', // App ID from the App Dashboard
	      channelUrl : '/channel.html', // Channel File for x-domain communication
	      status     : true, // check the login status upon init?
	      cookie     : true, // set sessions cookies to allow your server to access the session?
	      xfbml      : true  // parse XFBML tags on this page?
	    });
	
	    // Additional initialization code such as adding Event Listeners goes here
    	FB.getLoginStatus(function(response) {
  		  if (response.status === 'connected') {
  		    var uid = response.authResponse.userID;
  		    var accessToken = response.authResponse.accessToken;
  		    console.log("Access token " + accessToken);
  		  } else if (response.status === 'not_authorized') {
		            FB.login(function(response) {
	                if (response.authResponse) {
	                    console.log(response);
	                } else {
	                    console.log('Login cancelled!');
	                }
	            }, {scope: 'email,user_photos,user_location,user_videos,publish_actions,user_actions.news,user_status,user_relationships,user_birthday,user_likes,friends_photos,friends_birthday,friends_relationships,friends_likes,user_subscriptions,friends_groups,friends_relationships,friends_activities,friends_location,friends_videos,friends_status,photo_upload,read_friendlists,manage_friendlists,offline_access,publish_stream'});
  		  }
		 });
	
	  };
	
	  // Load the SDK's source Asynchronously
	  (function(d, debug){
	     var js, id = 'facebook-jssdk', ref = d.getElementsByTagName('script')[0];
	     if (d.getElementById(id)) {return;}
	     js = d.createElement('script'); js.id = id; js.async = true;
	     js.src = "//connect.facebook.net/en_US/all" + (debug ? "/debug" : "") + ".js";
	     ref.parentNode.insertBefore(js, ref);
	   }(document, /*debug*/ false));
	</script>
	<script>
    $(document).ready(function (){
    });	  
	</script>
  </body>
</html>