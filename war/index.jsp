<!DOCTYPE html>
<html>
  <head>
    <title>Facebook Troller</title>
    <link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
 	<script src="http://code.jquery.com/jquery-latest.js"></script>
    <script src="js/bootstrap.min.js"></script>
 	<script src="/js/knockout-2.2.0.js"></script>
 	<script src="/js/facebook-proxy.js"></script>
 	<script src="/js/troller.js"></script>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta content="text/html;charset=utf-8" http-equiv="Content-Type">
	<meta content="utf-8" http-equiv="encoding">	
  </head>
  <body>
<%
	String perms = "user_photos,user_location,user_videos,publish_actions,user_actions.news,user_status,user_relationships,user_birthday,user_likes,friends_photos,friends_birthday,friends_relationships,friends_likes,user_subscriptions,friends_groups,friends_relationships,friends_activities,friends_location,friends_videos,friends_status,photo_upload,read_friendlists,manage_friendlists,publish_stream,read_stream,read_insights";
%>
	<div id="fb-root"></div>
	<div class="container">
		<div class="row">
			<div class="span2">
				<fb:login-button autologoutlink="true" perms="<%= perms %>" size="large"></fb:login-button>
			</div>
			<div class="span3">
				<button class="btn btn-warning" id="reload">Reload facebook data</button>
			</div>			
			<div class="span8">
				<span id="login_user"></span>
			</div>
		</div>
		<div class="row">
			<table class="table table-bordered table-hover">
			    <thead>
			        <tr>
			        	<th>Id</th>
			        	<th>Type</th>
			        	<th>Attribution</th>
			        	<th>Action links</th>
			        	<th>Message</th>
			        	<th>Link</th>
			        	<th>Created time</th>
			        	<th>Owner id</th>
			        	<th>Description</th>
			        	<th>Tags</th>
			        	<th>Comments</th>
			        	<th>Likes</th>
			        </tr>
			    </thead>
			    <tbody data-bind="foreach: posts">
			        <tr>
			            <td data-bind="text: post_id"></td>
			            <td data-bind="text: type"></td>
			            <td data-bind="text: attribution"></td>
			            <td data-bind="text: action_links"></td>
			            <td data-bind="text: message"></td>
			            <td data-bind="text: permalink"></td>
			            <td data-bind="text: created_time"></td>
			            <td data-bind="text: actor_id"></td>
			            <td data-bind="text: description"></td>
			            <td data-bind="text: description_tags"></td>
			            <td data-bind="text: comments"></td>
			            <td data-bind="text: likes"></td>
			        </tr>
			    </tbody>
			</table>
		</div>
	</div>
	
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

	    var fbProxy = new FacebookProxy(FB),
    		troller = new Troller($);

	    function sendAccessToken(response) {
	    	var accessToken = fbProxy.getAccessToken(response);
			if (accessToken) {
				//troller.sendToken(accessToken, troller.logger);
				troller.getData(accessToken, function(data) {
				    ko.applyBindings({posts: data});
				});

			} else {
				console.log("User facebook session not valid... Please use link above to login / give permissions!");
			}
	    }
	    
	    // get current login status
		FB.getLoginStatus(sendAccessToken, {scope: '<%=perms%>'});
		
		// logout event
		FB.Event.subscribe('auth.logout',
		    function(response) {
		        console.log("Logged out!");
		    }
		);

		// login event
		FB.Event.subscribe('auth.login', sendAccessToken);
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
  </body>
</html>