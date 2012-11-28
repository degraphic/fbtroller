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
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.mihaibojin.ds.Memcache;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.exception.FacebookResponseStatusException;
import com.restfb.json.JsonObject;

@SuppressWarnings("serial")
public class TimelineSpiderServlet extends HttpServlet {
	private String access_token;
	private String uid;
	FacebookClient facebookClient;
	private final String EntityKey = "Posts";
	private final Integer MAX_RECORDS = new Integer(300);
	
	private static final Logger log = Logger.getLogger(FacebookLoginServlet.class.getName());
	private static final Memcache cache = Memcache.getInstance();
	
	/**
	 * Store list of user posts to the database
	 * @param queryResults
	 * @param fetchTime
	 */
	private void storeData(List<JsonObject> queryResults, Long fetchTime)
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		for (JsonObject row : queryResults) {
			// define unique key and create new entity for storing it in the database
			String postId = (String)row.get("post_id");
			String recordKey = "post-" + uid + "-" + postId;
	        Key datastoreKey = KeyFactory.createKey(EntityKey, recordKey);
			Entity postData = new Entity(EntityKey, recordKey);
			
			// set entity values
			postData.setProperty("record_time", (new java.util.Date()).getTime()); // record creation time
			postData.setProperty("uid", uid);
			postData.setProperty("post_id", postId);
			postData.setProperty("created_time", row.get("created_time"));
			postData.setProperty("actor_id", row.get("actor_id"));
			postData.setUnindexedProperty("data", new Text(row.toString(4)));
			postData.setProperty("fetch_time", fetchTime.toString());

			// add post to database
			Transaction txn = datastore.beginTransaction();
			try {
				try {
					// try to find key in database
					datastore.get(datastoreKey);

					// @TODO: persist values between records / retrieve from above and save into postData
					
					// if key was found, exception is not thrown
					datastore.delete(datastoreKey);
				} catch (EntityNotFoundException e) {
					// key not already in database, new user
					log.info(String.format("Post %d for %d already in datastore", postId, uid));
				} 
				
				// save user data to data store
		        datastore.put(postData);

		        // commit transaction to DB
				txn.commit();
				
			} finally {
				// cleanup
			    if (txn.isActive()) {
			        txn.rollback();
			    }
			}			
		}
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		access_token = req.getParameter("access_token");
		uid = req.getParameter("uid");
		Long endTime = Long.valueOf(req.getParameter("endTime"));
		Long startTime = Long.valueOf(req.getParameter("startTime"));
		Long queryTime = (new java.util.Date()).getTime();

		// check if access_token is allowed to run
		String cacheKey = "token_" + access_token;
	    byte[] value = cache.get(cacheKey);
	    if (value == null) {
	    	// access_token is not valid anymore; stop execution
	    	log.warning("Access token invalid... stopping timeline map process!");
	    	
			return;
	    }	

	    // define data retrieval query
		facebookClient = new DefaultFacebookClient(access_token);
		String query = String.format("SELECT post_id, app_id, app_data, attachment, impressions, place, tagged_ids, message_tags, attribution, filter_key, attribution, action_links, message, permalink, type, created_time, actor_id, target_id, privacy, description, description_tags, comments, likes FROM stream WHERE source_id=me() AND created_time >= %s AND created_time <= %s LIMIT 500", String.valueOf(startTime), String.valueOf(endTime));
		List<JsonObject> queryResults = null;
		
		try {
			queryResults = facebookClient.executeQuery(query, JsonObject.class);
			log.info(String.format("Retrieved data for %d between: %d - %d; got %d records", uid, startTime, endTime, queryResults.size()));
			
		} catch (FacebookResponseStatusException e) {
			// facebook query failed, log error - probably a throttling issue
			log.severe(String.format("Facebook exception for %d - %d: %s", startTime, endTime, e.toString()));

			// set response status to 404 so query can be retried
			resp.setStatus(404);
			return;
			
		} catch (FacebookOAuthException e) {
			// query failed because of invalid 
			if (190 == e.getErrorCode()) {
				// remove access_token from memcache, preventing jobs to run
			    cache.delete(cacheKey);
			}			
		}

		// if querySize is at least MAX_RECORDS, response might have been limited
		// additional split is required; if time difference is smaller than 3 seconds, can't split anymore 
		if (MAX_RECORDS < queryResults.size() && endTime - startTime > 2) {
			// split into half intervals
			long split = endTime - (endTime - startTime)/2;
			log.info(String.format("Subsplitting %d records for %d: %d - %d at %d", queryResults.size(), uid, startTime, endTime, split));
			
			// add time chunks to timeline map queue
			Queue queue = QueueFactory.getQueue("timeline-map");
			
		    queue.add(withUrl("/timeline-spider").param("access_token", access_token)
		    									 .param("uid", uid)
		    									 .param("endTime", endTime.toString())
		    								  	 .param("startTime", Long.toString(split+1))
		    								  	 .method(Method.GET));
		    
		    queue.add(withUrl("/timeline-spider").param("access_token", access_token)
												 .param("uid", uid)
												 .param("endTime", Long.toString(split))
											  	 .param("startTime", startTime.toString())
											  	 .method(Method.GET));
		    
		    // set correct response
			resp.setContentType("application/json");
			resp.getWriter().println("{\"result\": \"subsplit\"}");
			
		// if response size is less than MAX_RECORDS, add data to the datastore
		} else {
			log.info(String.format("Saving %d records for %d to datastore (%d, %d)", queryResults.size(), uid, startTime, endTime));
			
			// store data to database
			storeData(queryResults, queryTime);
			
		    // set correct response
			resp.setContentType("application/json");
			resp.getWriter().println("{\"result\": \"ok\"}");
		}

	}

}
