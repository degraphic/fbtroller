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

	return self;
	
}
