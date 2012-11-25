var Troller = function($, FB, fbProxy, perms) {
    var self = {},
    	userOffset = 0,
    	perPage = 100;

    // send token to server and get back response
    self.sendToken = function(access_token, cb) {
	$.ajax({
	    url : '/login',
	    data : {
		token : access_token
	    },
	    success : cb,
	    dataType : 'json'
	});
    }

    // send token to server and get back response
    self.getData = function(access_token, cb) {
	$.ajax({
	    url : '/data',
	    data : {
		token : access_token,
		offset : userOffset,
		limit : perPage
	    },
	    success : function (data) {
		// if there's still data to retrieve, keep on requesting it
		if (data.length > 0 && userOffset < 500) {
		    	userOffset += data.length;
			self.getData(access_token, cb);
		}
		
		// call data callback
		cb(data);
	    },
	    dataType : 'json'
	});
    }

    // sets correct button labels and links for login
    var setLogin = function () {
	$("#login_user a").attr("href", "#").html("stranger");
	$("#login").html("Login");
	$("#reload").addClass("disabled");
    }
    
    // sets correct button labels and links for logout
    var setLogout = function (uid, name) {
	$("#login_user a").attr("href", "http://www.facebook.com/" + uid).html(name);
	$("#login").html("Logout");
	$("#reload").removeClass("disabled");
    }
    
    // bind buttons actions
    self.bind = function (FB, response) {
        fbProxy.isLoggedIn(function (accessToken) {
            //if logged in, do logout
            if (accessToken) {
        	FB.api('/me', function(response) {
        	    setLogout(response.id, response.first_name);
        	});
        	
            } else {
        	setLogin();
            }
        });
	
	// bind login button
	$("#login").click(function() {
	    
	    fbProxy.isLoggedIn(function (accessToken) {
		//if logged in, do logout
		if (accessToken) {
		    FB.logout(function(response) {
			setLogout();
		    });
			
		//if not logged in
		} else {
		    FB.login(function(response) {
			if (response.authResponse) {
			    FB.api('/me', function(response) {
				setLogin(response.uid, response.first_name);
			    });

			} else {
			    console.log('User cancelled login or did not fully authorize.');
			}
		    }, {scope : perms});
		}		
	    })
	});
    }
        
    // execute on login
    self.login = function(response) {
	var accessToken = fbProxy.getAccessToken(response);

	// bind login button
	self.bind(FB, response);

	// if logged in
	if (accessToken) {
	    // send user's access token to server
	    self.sendToken(accessToken, self.logger);

	} else {
	    console.log("User facebook session not valid... Please use link above to login / give permissions!");
	}
    }

    // execute after login, loads user data and displays with knockoutjs
    self.afterLogin = function (response, cb) {
	var accessToken = fbProxy.getAccessToken(response);

	// bind login button
	self.bind(FB, response);

	// if logged in
	if (accessToken) {
	    // get user data
	    self.getData(accessToken, function(data) {
		cb(data);
	    });

	} else {
	    console.log("User facebook session not valid... Reauth!");
	}
    }

    // log response to console
    self.logger = function(data) {
	console.log("Got response:", data);
    }

    return self;
}