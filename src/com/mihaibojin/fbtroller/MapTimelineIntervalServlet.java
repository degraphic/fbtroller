package com.mihaibojin.fbtroller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.mihaibojin.model.Results;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.json.JsonObject;

@SuppressWarnings("serial")
public class MapTimelineIntervalServlet extends HttpServlet {
	private String access_token;
	FacebookClient facebookClient;
	private final String EntityKey = "Users";
	
	private static final Logger log = Logger.getLogger(FacebookLoginServlet.class.getName());

	// returns true if timeline events still exist
	private boolean hasData(long time)
	{
		boolean result = false;
		
		// check if user has data for specified time
		String query = String.format("SELECT post_id, filter_key, attribution, action_links, message, permalink, type, created_time, actor_id, target_id, privacy, description, description_tags, comments, likes FROM stream WHERE source_id=me() and created_time <= %s limit 10", String.valueOf(time));
		//log.severe("Executing: " + query);
		List<JsonObject> queryResults = facebookClient.executeQuery(query, JsonObject.class);
		//log.severe(String.format("size: %d", queryResults.size()));
		
		if (0 < queryResults.size()) {
			result = true;
		}
		
		return result;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String key = req.getParameter("key");
		String access_token;
		Long uid;

		try {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	        Key usersKey = KeyFactory.createKey(EntityKey, key);
			Entity user = datastore.get(usersKey);
			
			// get access token and uid
			access_token = (String)user.getProperty("access_token");
			uid = (Long)user.getProperty("uid");
			facebookClient = new DefaultFacebookClient(access_token);		

			long currentTime = (new java.util.Date()).getTime()/1000;
			int interval = 31104000;
			long min = 0;
			long max = 311040000; // 10 years
			long lastTime=0;
			
			while (true) {
				long i = min;
				while(true) {
					i=i+interval;
					long checkTime = currentTime - i;
					boolean ok = hasData(checkTime);
					
					if (ok) {
						min = i;
						//log.severe(String.format("Has data for %d", checkTime));
					} else {
						max = i;
						interval = interval/2;
						lastTime = checkTime;
						//log.severe(String.format("Does not have data for %d", checkTime));
						break;
					}
				}
				
				if (interval <= 1) {
					break;
				}
			}
			
			// minimum timestamp for events
			log.severe(String.format("Last timestamp %d", lastTime));
			
		} catch (EntityNotFoundException e) {
			// user not already there
		} 
		
		resp.setContentType("text/plain");
		resp.getWriter().println(key);

		
/*		
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

		 */
	}

}
