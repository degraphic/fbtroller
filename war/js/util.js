function Util () {
    var self = {};

    // map a type to Facebook's definitions
    self.mapStreamType = function (type) {
	var ret = "";
	
	switch (type) {
		case 11: 
		    ret = "Group created";
		    break;
		    
		case 12: 
		    ret = "Event created";
		    break;
		    
		case 46: 
		    ret = "Status update";
		    break;
		    
		case 56: 
		    ret = "Post on wall from another user";
		    break;
		    
		case 66: 
		    ret = "Note created";
		    break;
		    
		case 80: 
		    ret = "Link posted";
		    break;
		    
		case 128: 
		    ret = "Video posted";
		    break;
		    
		case 247: 
		    ret = "Photos posted";
		    break;
		    
		case 237: 
		    ret = "App story";
		    break;
		    
		case 257: 
		    ret = "Comment created";
		    break;
		    
		case 272: 
		    ret = "App story";
		    break;
		    
		case 285: 
		    ret = "Checkin to a place";
		    break;
		    
		case 308: 
		    ret = "Post in Group";
		    break;
		    
		default:
	}
	
	return ret;
    }    
    
    // format two number values
    self.numberFormat = function (val) {
	if (10 > val) {
	    return "0" + val;
	}
	
	return val;
    }

    //return formatted time
    self.formatTime = function (time) {
	var date = new Date(time*1000),
		day = self.numberFormat(date.getDate()),
		month = self.numberFormat(date.getMonth()+1),
		year = date.getFullYear(),
		hour = self.numberFormat(date.getHours()),
		minute = self.numberFormat(date.getMinutes()),
		second = self.numberFormat(date.getSeconds());
	
	return day + "/" + month + "/" + year + " " + hour + ":" + minute + ":" + second;
    } 
    
    // format privacy
    self.formatPrivacy = function (privacy) {
	return privacy.description;
    }

    // create facebook link
    self.generateIdLink = function (owner) {
	return "<a href='http://www.facebook.com/" + owner + "' target='_blank'>" + owner + "</a>";
    }
    
    // create a list of tag links
    self.formatTags = function (tags) {
	var ret = "";
	for (var i in tags) {
	    for (var j in tags[i]) {
        	    ret += "<a href='http://www.facebook.com/" + tags[i][j].id + "' target='_blank'>" + tags[i][j].name + "</a>, ";
	    }
	}
	
	// remove last two chars
	if (ret.length) {
	    ret = ret.substring(0, ret.length - 2);
	}
	
	return ret;
    }
    
    // format list of comments
    self.formatComments = function (comments) {
	var ret = "<ul>";
	
	if (comments && "undefined" !== typeof comments.count && comments.count > 0) {
	    for (var i in comments.comment_list) {
		ret += "<li><a href='http://www.facebook.com/" + comments.comment_list[i].id + "' target='_blank'>" + comments.comment_list[i].text + "</a></li>";
	    }
	}
	
	ret += "</ul>";
	
	return ret;
    }
    
    
    // format likes
    self.formatLikes = function (likes) {
	var ret = "";
	
	if (likes && "undefined" !== typeof likes.count && likes.count > 0) {
	    ret += "<a href='" + likes.href + "' target='_blank'>" + likes.count + " likes</a>";
	}
	
	return ret;
    }
    
    // format action links
    self.formatActionLinks = function (links) {
	var ret = "";
	for (var i in links) {
    	    ret += "<a href='" + links[i].href + "' target='_blank'>" + links[i].text + "</a>, ";
	}
	
	// remove last two chars
	if (ret.length) {
	    ret = ret.substring(0, ret.length - 2);
	}
	
	return ret;
    }
    
    // format attachments
    self.formatAttachment = function (attachment) {
	var ret = "";
	
	if (attachment && "undefined" !== typeof attachment.media && attachment.media.length > 0) {
		for (var i in attachment.media) {
		    var row = attachment.media[i];
	    	    ret += "<a href='" + row.href + "' target='_blank'><img src='" + row.src + "' border='0' style='max-width:98px; max-height: 98px' /></a>";
		}
	}
	
	return ret;
    }
    
    return self;
}