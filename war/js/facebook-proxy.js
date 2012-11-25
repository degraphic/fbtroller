var FacebookProxy = function (FB) {
	var self = {};
	
	// retrieves access token from Facebook or returns false if session not auth
	self.getAccessToken = function(response) {
		if (response.status === 'connected') {
			var uid = response.authResponse.userID;
			var accessToken = response.authResponse.accessToken;
			return accessToken;
		}

		return false;
	}

	// check if user is logged in
	self.isLoggedIn = function (cb) {
	    FB.getLoginStatus(function(response) {
		var access_token = self.getAccessToken(response);
		if (access_token) {
		    cb(access_token);		    
		} else {
		    cb(null);
		}
	    });
	}
	
	return self;
	
}
