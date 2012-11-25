package com.mihaibojin.fbtroller;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.util.Iterator;
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
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookResponseStatusException;
import com.restfb.json.JsonObject;

@SuppressWarnings("serial")
public class TimelineSpiderServlet extends HttpServlet {
	private String access_token;
	private String uid;
	FacebookClient facebookClient;
	private final String EntityKey = "Posts";
	
	private static final Logger log = Logger.getLogger(FacebookLoginServlet.class.getName());
	
	private void storeData(List<JsonObject> queryResults, Long fetchTime)
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		for (JsonObject row : queryResults) {
			// define unique key and create new entity for storing it in the database
			String recordKey = "post-" + uid + "-" + row.get("post_id");
	        Key datastoreKey = KeyFactory.createKey(EntityKey, recordKey);
			Entity postData = new Entity(EntityKey, recordKey);
			
			postData.setProperty("uid", uid);
			postData.setProperty("post_id", row.get("post_id"));
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

					// if key was found, exception is not thrown
					datastore.delete(datastoreKey);
				} catch (EntityNotFoundException e) {
					// key not already in database
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
		facebookClient = new DefaultFacebookClient(access_token);
		Long endTime = Long.valueOf(req.getParameter("endTime"));
		Long startTime = Long.valueOf(req.getParameter("startTime"));
		Long queryTime = (new java.util.Date()).getTime();


		String query = String.format("SELECT post_id, app_id, app_data, attachment, impressions, place, tagged_ids, message_tags, attribution, filter_key, attribution, action_links, message, permalink, type, created_time, actor_id, target_id, privacy, description, description_tags, comments, likes FROM stream WHERE source_id=me() AND created_time >= %s AND created_time <= %s LIMIT 500", String.valueOf(startTime), String.valueOf(endTime));
		
		List<JsonObject> queryResults = null;
		try {
			queryResults = facebookClient.executeQuery(query, JsonObject.class);
			log.severe(String.format("Retrieving data between: %d - %d; %d records", startTime, endTime, queryResults.size()));
			
		} catch (FacebookResponseStatusException e) {
			log.severe(String.format("Facebook exception for %d - %d: %s", startTime, endTime, e.getMessage()));
		}

		// if query failed, do not proceed with saving to datastore
		if (null == queryResults) {
			resp.setStatus(404);
			return;
		}
		
		// if querySize is at least 50, response might have been limited
		// additional split is required; if time difference is smaller than 3, can't split anymore 
		if (300 < queryResults.size() && endTime - startTime > 2) {
			// split into half intervals
			long split = endTime - (endTime - startTime)/2;
			log.severe(String.format("Subsplitting %d records: %d - %d at %d", queryResults.size(), startTime, endTime, split));
			
			// add time chunk to timeline map queue
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
		    
		    // correct response
			resp.setContentType("application/json");
			resp.getWriter().println("{\"result\": \"subsplit\"}");
			
		// add data to the datastore
		} else {
			log.severe(String.format("Saving %d records to datastore (%d, %d)", queryResults.size(), startTime, endTime));
			
			// store data to database
			storeData(queryResults, queryTime);
			
		    // correct response
			resp.setContentType("application/json");
			resp.getWriter().println("{\"result\": \"ok\"}");
		}

	}

}
