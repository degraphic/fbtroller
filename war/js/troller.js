var Troller = function ($) {
	var self = {};
	
	// send token to server and get back response
	self.sendToken = function (access_token, cb) {
		$.ajax({
		  url: '/login',
		  data: {token: access_token},
		  success: cb,
		  dataType: 'json'
		});
	}
	
	// send token to server and get back response
	self.getData = function (access_token, cb) {
		$.ajax({
		  url: '/data',
		  data: {token: access_token, offset: 0, limit: 10},
		  success: cb,
		  dataType: 'json'
		});
	}
	
	// log response to console
	self.logger = function(data) {
		console.log("Got response:", data);
	}
	
	return self;
}