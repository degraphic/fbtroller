package com.mihaibojin.model;

import java.io.Serializable;
import java.util.List;

import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.restfb.json.JsonObject;

import com.restfb.DefaultJsonMapper;
import com.restfb.Facebook;

public class Results implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 112312113212L;

	@Facebook
	private
	JSONArray userInfo;
	
	@Facebook
	private
	JSONArray timeline;

	public static JSONArray encode(List<JsonObject> data) {
		JSONArray arr=new JSONArray();
		
		for (JsonObject json : data) {
			arr.put(json);
		}
		
		return arr;
	}
	public JSONArray getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(List<JsonObject> userInfo) {
		this.userInfo = encode(userInfo);
	}
	public JSONArray getTimeline() {
		return timeline;
	}
	public void setTimeline(List<JsonObject> timeline) {
		this.timeline = encode(timeline);
	}
	
	public String toString()
	{
		DefaultJsonMapper jsonMapper = new DefaultJsonMapper();
		return jsonMapper.toJson(this);
	}
}

