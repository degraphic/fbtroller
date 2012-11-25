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
 	<script src="/js/util.js"></script>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta content="text/html;charset=utf-8" http-equiv="Content-Type">
	<meta content="utf-8" http-equiv="encoding">	
  </head>
  <body>
<%
	String perms = "user_photos,user_location,user_videos,publish_actions,user_actions.news,user_status,user_relationships,user_birthday,user_likes,friends_photos,friends_birthday,friends_relationships,friends_likes,user_subscriptions,friends_groups,friends_relationships,friends_activities,friends_location,friends_videos,friends_status,photo_upload,read_friendlists,manage_friendlists,publish_stream,read_stream,read_insights";
%>
	<div id="fb-root"></div>
	
	<div class="container-fluid" style="margin-top: 10px">
		<div class="row-fluid">
			<div class="span1 offset1"><button class="btn btn-primary" id="login">Login</button></div>
			<div class="span2"><button class="btn btn-warning disabled" id="reload">Reload facebook data</button></div>
			<div class="span6"><p id="login_user">Welcome <a href="#" target="_blank" class="navbar-link">stranger</a></p></div>
		</div>	
	</div>
	    
	<div class="container-fluid" style="margin-top: 10px" data-bind="">
		<div class="row-fluid">
			<table class="table table-bordered table-hover">
			    <thead>
			        <tr>
			        	<th>Id</th>
			        	<th>Attachments</th>
			        	<th>Created time</th>
			        	<th>Privacy</th>
			        	<th>Type</th>
			        	<th>Owner id</th>
			        	<th>Message</th>
			        	<th>Description</th>
			        	<th>Tags</th>
			        	<th>Comments</th>
			        	<th>Likes</th>
			        	<th>Attribution</th>
			        	<th>Action links</th>
			        </tr>
			    </thead>
			    <tbody data-bind="foreach: items">
			        <tr>
			            <td data-bind="html: $root.getId(permalink, post_id)"></td>
			            <td data-bind="html: util.formatAttachment(attachment)"></td>
			            <td data-bind="text: util.formatTime(created_time)"></td>
			            <td data-bind="text: util.formatPrivacy(privacy)"></td>
			            <td data-bind="text: util.mapStreamType(type)"></td>
			            <td data-bind="html: util.generateIdLink(actor_id)"></td>
			            <td style="max-width: 400px" data-bind="text: message"></td>
			            <td data-bind="text: description"></td>
			            <td data-bind="html: util.formatTags(description_tags)"></td>
			            <td data-bind="html: util.formatComments(comments)"></td>
			            <td data-bind="html: util.formatLikes(likes)"></td>
			            <td data-bind="text: attribution"></td>
			            <td data-bind="html: util.formatActionLinks(action_links)"></td>
			        </tr>
			    </tbody>
			</table>		
		</div>
	</div>
	
	<script>
		var util = new Util();
		
		// Knockout events
		var PostsViewModel = function() {
		    var self = {};
		    
		    // data array
		    self.items = ko.observableArray([]);
		 
		    // sort items
		    self.sortByName = function() {
		        this.items.sort(function(a, b) {
		            return a.name < b.name ? -1 : 1;
		        });
		    };
		 	
		    // create link html
		    self.getId = function (permalink, post_id) {
				return "<a href='" + permalink + "' target='_blank'>" + post_id + "</a>";
		    }
		    
		    
		    return self;
		};
		
		var viewModel = new PostsViewModel();
		$(document).ready(function () {
			ko.applyBindings(viewModel);	
		})
		
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
	    		troller = new Troller($, FB, fbProxy, '<%=perms%>');
	        
		    // get current login status
			FB.getLoginStatus(function (response) {
			    // call afterLogin event and add result to array
			    troller.afterLogin(response, function (data) {
					for (var i in data) {
					    viewModel.items.push(data[i]);
					}
			    });
			}, {scope: '<%=perms%>'});
			
			// logout event
			FB.Event.subscribe('auth.logout',
			    function(response) {
			        console.log("Logged out!");
			    }
			);

			// login event
			FB.Event.subscribe('auth.login', troller.login);
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