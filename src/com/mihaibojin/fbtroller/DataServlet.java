package com.mihaibojin.fbtroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Text;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookException;
import com.restfb.json.JsonObject;

@SuppressWarnings("serial")
public class DataServlet extends HttpServlet {
	private String access_token;
	FacebookClient facebookClient;
	
	private static final Logger log = Logger.getLogger(DataServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		access_token = req.getParameter("token");

		// Get offset / set default offset
		String offset = req.getParameter("offset");
		if (null == offset) {
			offset = "0";
		}

		// Get limit / set default limit to 10
		String limit = req.getParameter("limit");
		if (null == limit || Integer.valueOf(limit) > 50) {
			limit = "10";
		}

		// create datastore query
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		FetchOptions fetchOptions = FetchOptions.Builder.withOffset(Integer.valueOf(offset)).limit(Integer.valueOf(limit));
		
		try {
			facebookClient = new DefaultFacebookClient(access_token);		
			
			// get user's UID and NAME
			String query = "SELECT uid, username, pic_square, birthday_date, sex, name FROM user WHERE uid=me()";
			List<JsonObject> queryResults = facebookClient.executeQuery(query, JsonObject.class);

			if ( 0 < queryResults.size() ) {
				JsonObject u = queryResults.get(0);
				Integer uid = (Integer)u.get("uid");
				
				// create query to retrieve user's posts and sort by creation time descending 
				Filter filter = new FilterPredicate("uid", Query.FilterOperator.EQUAL, Integer.toString(uid));
				Query q = new Query("Posts")
						.setFilter(filter)
						.addSort("created_time", SortDirection.DESCENDING);
				PreparedQuery pq = datastore.prepare(q);
			    QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
			    
			    // container to store response
			    List<JsonNode> data = new ArrayList<JsonNode>();

			    // parse results and add to response
				ObjectMapper mapper = new ObjectMapper();
				for (Entity entity : results) {
					JsonFactory factory = mapper.getFactory();
					Text rowData = (Text)entity.getProperty("data");
					JsonParser jp = factory.createJsonParser(rowData.getValue());
					JsonNode actualObj = mapper.readTree(jp);
					data.add(actualObj);
				}

			    // send json object to client
				resp.setContentType("application/json");
				resp.getWriter().println(mapper.writeValueAsString(data));
			}
		} catch (FacebookException e) {
			// error
			resp.setContentType("application/json");
			resp.getWriter().println("{\"result\": \"error\", \"message\": \"Cannot retrieve user data\"}");
		}
	}

}
