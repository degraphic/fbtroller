package com.mihaibojin.fbtroller;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.JsonObject;

@SuppressWarnings("serial")
public class MapTimelineIntervalServlet extends HttpServlet {
	private String access_token;
	private Long uid;
	FacebookClient facebookClient;
	private final String EntityKey = "Users";
	
	private static final Logger log = Logger.getLogger(FacebookLoginServlet.class.getName());

	// returns true if timeline events still exist
	private boolean hasData(long time)
	{
		boolean result = false;
		
		try {
			// check if user has data for specified time
			String query = String.format("SELECT post_id, filter_key, attribution, action_links, message, permalink, type, created_time, actor_id, target_id, privacy, description, description_tags, comments, likes FROM stream WHERE source_id=me() and created_time <= %s limit 1", String.valueOf(time));
			//log.severe("Executing: " + query);
			List<JsonObject> queryResults = facebookClient.executeQuery(query, JsonObject.class);
			//log.severe(String.format("size: %d", queryResults.size()));
			
			if (0 < queryResults.size()) {
				result = true;
			}
		} catch (FacebookOAuthException e) {
			// query failed
			if (190 == e.getErrorCode()) {
				// remove access_token from memcache, preventing jobs to run
				MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
				String key = "token_" + access_token;
				syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
			    syncCache.delete(key);
			}
		}
		
		return result;
	}
	
	// find first post of user using a breadth first search
	private long process(Entity user) throws Exception
	{
		// get access token and uid
		access_token = (String)user.getProperty("access_token");
		uid = (Long)user.getProperty("uid");
		
		MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		String key = "token_" + access_token;
		syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
	    byte[] value = (byte[]) syncCache.get(key); // read from cache
	    if (value == null) {
	    	// access_token is not valid anymore; stop execution
	    	throw new Exception("Access token error");
	    }	
	    
	    
		// initialize facebook client
		facebookClient = new DefaultFacebookClient(access_token);		
		
		long currentTime = (new java.util.Date()).getTime()/1000;
		int interval = 31104000;
		long min = 0;
		long max = 311040000; // 10 years
		long lastTime = currentTime;
		long lastSend = currentTime;
		
		while (true) {
			long i = min;
			
			while(true) {
				i=i+interval;
				long checkTime = currentTime - i;
				boolean ok = hasData(checkTime);
				
				if (ok) {
					// last substract
					min = i;
					
					// start feeding parsing events into queue
					feedData(lastSend, checkTime+1);
					lastSend = checkTime;
					
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
	
		// feed last interval into queue
		feedData(lastSend, lastTime);
		
		// minimum time stamp for events
		log.severe(String.format("Last timestamp %d", lastTime));
		
		return lastTime;
	}
	
	// feed intervals of data into spider
	private void feedData(long startTime, long endTime)
	{
		long timeCounter = startTime;
		long newTime = timeCounter;
		log.severe(String.format("Feeding data for %d - %d", startTime, endTime));
		
		while (timeCounter > endTime) {
			newTime = timeCounter - 86400*21; // initial split of 21 day chunks
			
			// add time chunk to timeline map queue
			Queue queue = QueueFactory.getQueue("timeline-map");
		    queue.add(withUrl("/timeline-spider").param("access_token", access_token)
		    									 .param("uid", Long.toString(uid))
		    									 .param("endTime", Long.toString(timeCounter))
		    								  	 .param("startTime", Long.toString(newTime))
		    								  	 .method(Method.GET));
			// decrement timeCounter
			timeCounter = newTime - 1;
		}
	}
	
	
	// GET request endpoint
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String key = req.getParameter("key");

		try {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	        Key usersKey = KeyFactory.createKey(EntityKey, key);
			Entity user = datastore.get(usersKey);
			
			// process user's wall posts
			process(user);
						
		} catch (EntityNotFoundException e) {
			// user not already there
		} catch (Exception e) {
			// access_token error
			log.severe("Access token error");
		}
		
		resp.setContentType("text/plain");
		resp.getWriter().println(key);

	}

}
