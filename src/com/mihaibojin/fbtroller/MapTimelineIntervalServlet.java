package com.mihaibojin.fbtroller;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

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
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.mihaibojin.ds.Memcache;
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
	private final Long MAP_INTERVAL = new Long(86400*21); // maximum of 21 days
	
	private static final Logger log = Logger.getLogger(FacebookLoginServlet.class.getName());
	private static final Memcache cache = Memcache.getInstance();

	// returns true if timeline events still exist
	private boolean hasData(long time)
	{
		boolean result = false;
		
		// check if user has data for specified time
		String query = String.format("SELECT post_id, filter_key, attribution, action_links, message, permalink, type, created_time, actor_id, target_id, privacy, description, description_tags, comments, likes FROM stream WHERE source_id=me() and created_time <= %s limit 1", String.valueOf(time));
		//log.severe("Executing: " + query);
		List<JsonObject> queryResults = facebookClient.executeQuery(query, JsonObject.class);
		//log.severe(String.format("size: %d", queryResults.size()));
		
		if (0 < queryResults.size()) {
			result = true;
		}
		
		return result;
	}
	
	// find first post of user using a breadth first search
	private long process()
	{
		long currentTime = (new java.util.Date()).getTime()/1000;
		int interval = 31104000;
		long min = 0;
		@SuppressWarnings("unused")
		long max = 311040000; // 10 years
		long lastTime = currentTime;
		long lastSend = currentTime;
		
		/**
		 * Split interval into binary trees and start retrieving data as soon as a definite interval is discovered
		 */
		while (true) {
			long i = min;
			
			// check for existence of data
			while(true) {
				i=i+interval;
				long checkTime = currentTime - i;
				boolean ok = hasData(checkTime);
				
				// if has data, start retrieving it
				if (ok) {
					// last substract
					min = i;

					// start feeding parsing events into queue
					feedData(lastSend, checkTime+1);
					lastSend = checkTime;

				// if it doesn't have data, halven the interval step and try again from last existence point
				} else {
					max = i;
					interval = interval/2;
					lastTime = checkTime;
					
					break;
				}
			}
			
			// stop when delta is smaller than 1 second
			if (interval <= 1) {
				break;
			}
		}	
	
		// feed last interval into queue
		feedData(lastSend, lastTime);
		
		// minimum time stamp for events
		log.info(String.format("Last timestamp for %d: %d", uid, lastTime));
		
		return lastTime;
	}
	
	/**
	 * Feed intervals of data to spider queue
	 * @param startTime
	 * @param endTime
	 */
	private void feedData(long startTime, long endTime)
	{
		long timeCounter = startTime;
		long newTime = timeCounter;
		log.info(String.format("Feeding data for user %d between %d and %d", uid, startTime, endTime));
		
		// ensure a maximum number of days is defined so we won't run into facebook's limitations
		while (timeCounter > endTime) {
			// decrement lower limit of interval
			newTime = timeCounter - MAP_INTERVAL;
			
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
		// retrieve user's key
		String key = req.getParameter("key");

		// initialize datastore
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key usersKey = KeyFactory.createKey(EntityKey, key);
	    
		try {
			// retrieve user record from datastore
			Entity user = datastore.get(usersKey);
			
			// get access token and uid
			access_token = (String)user.getProperty("access_token");
			uid = (Long)user.getProperty("uid");

			// determine if access_token is valid
		    byte[] value = cache.get("token_" + access_token);
		    if (value == null) {
		    	// access_token is not valid anymore; stop execution
		    	throw new Exception("Access token invalid... stopping timeline map process!");
		    }	
			
			// initialize facebook client
			facebookClient = new DefaultFacebookClient(access_token);		
			
			// process user's timeline posts
			process();
						
		} catch (EntityNotFoundException e) {
			// user not in database
			log.warning(e.toString());
			
		} catch (FacebookOAuthException e) {
			// facebook query failed, delete token from DB
			log.warning(e.toString());
			
			if (190 == e.getErrorCode()) {
			    cache.delete("token_" + access_token);
			}
			
		} catch (Exception e) {
			// wrong access token
			log.warning(e.toString());
		}

	}

}
