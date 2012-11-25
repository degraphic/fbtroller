package com.mihaibojin.fbtroller;

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
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookException;
import com.restfb.json.JsonObject;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;

@SuppressWarnings("serial")
public class FacebookLoginServlet extends HttpServlet {
	private String access_token;
	FacebookClient facebookClient;
	private final String EntityKey = "Users";
	private static final Logger log = Logger.getLogger(FacebookLoginServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		access_token = req.getParameter("token");
		
		try {
			facebookClient = new DefaultFacebookClient(access_token);		
			
			// get user's UID and NAME
			String query = "SELECT uid, username, pic_square, birthday_date, sex, name FROM user WHERE uid=me()";
			List<JsonObject> queryResults = facebookClient.executeQuery(query, JsonObject.class);

			if ( 0 < queryResults.size() ) {
				JsonObject u = queryResults.get(0);
				
				String uKey = "user-" + String.valueOf(u.get("uid"));
		        log.severe("User key " + uKey);
				Entity userData = new Entity(EntityKey, uKey);
				
				// save all keys from FQL query to DB
				Iterator iter = u.keys();
				while (iter.hasNext()) {
					String key = (String)iter.next();
					userData.setProperty(key, u.get(key));
				}
				userData.setProperty("access_token", access_token);
				userData.setProperty("login_time", (new java.util.Date()).getTime());
				userData.setProperty("parsed", 0);

				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Transaction txn = datastore.beginTransaction();
				
				try {
					try {
						// try to find user in database
				        Key usersKey = KeyFactory.createKey(EntityKey, uKey);
						datastore.get(usersKey);
						
						datastore.delete(usersKey);
					} catch (EntityNotFoundException e) {
						// user not already there
					} 
					
					// save user data to data store
			        datastore.put(userData);
			        
					txn.commit();
					
				} finally {
					// cleanup
				    if (txn.isActive()) {
				        txn.rollback();
				    }
				}
		        
				// add user data requests to queue
				Queue queue = QueueFactory.getQueue("timeline-feeder");
			    queue.add(withUrl("/map-timeline").param("key", uKey).method(Method.GET));
			    
			    // correct response
				resp.setContentType("application/json");
				resp.getWriter().println("{\"result\": \"ok\"}");
			}
		} catch (FacebookException e) {
			// error
			resp.setContentType("application/json");
			resp.getWriter().println("{\"result\": \"error\", \"message\": \"Could not retrieve Facebook user data!\"}");
		}
	}

}
