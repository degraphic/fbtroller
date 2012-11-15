package com.mihaibojin.fbtroller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.*;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.mihaibojin.model.Results;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultJsonMapper;
import com.restfb.FacebookClient;
import com.restfb.json.JsonObject;

@SuppressWarnings("serial")
public class TimelineSpiderServlet extends HttpServlet {
	private String access_token;
	FacebookClient facebookClient;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		access_token = req.getParameter("token");
		facebookClient = new DefaultFacebookClient(access_token);		
	
		// get user's UID and NAME
		String query = "SELECT uid, name FROM user WHERE uid=me()";
		List<JsonObject> queryResults = facebookClient.executeQuery(query, JsonObject.class);

		if ( 0 < queryResults.size() ) {
			JsonObject u = queryResults.get(0);
			
	        Key usersKey = KeyFactory.createKey("Users", "user");
			Entity userData = new Entity("Greeting", usersKey);
//			userData.setProperty("user", user);
		}
		
		long currentTime = (new java.util.Date()).getTime();	
		String query2 = String.format("SELECT message, permalink, type, created_time, actor_id, target_id, privacy, description FROM stream WHERE source_id=me() and created_time <= %s order by created_time DESC limit 100", currentTime);
		List<JsonObject> queryResults2 = facebookClient.executeQuery(query2, JsonObject.class);
		
		JsonObject lastTime = queryResults2.get(queryResults2.size()-1);
		Integer str = (Integer)lastTime.get("created_time");
		
		
			
//		for (JsonObject obj : queryResults2) {
//			
//		}
		
		Results combined = new Results();
		combined.setUserInfo(queryResults);
		combined.setTimeline(queryResults2);

		JsonObject obj = new JsonObject();
		obj.put("userInfo", Results.encode(queryResults));
		obj.put("timeline", Results.encode(queryResults2));

		// return json of user response
		resp.setContentType("application/json");
		resp.getWriter().println(obj.toString());
		
		
		//resp.getWriter().println("Users: " + queryResults.get(0).getString("name"));
	}

}
