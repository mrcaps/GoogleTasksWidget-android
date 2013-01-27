package com.mrcaps.taskswidget.pojo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ActionList {
	private static final String TAG = "ActionList";
	
	JSONArray actions = new JSONArray();
	private static long action_id = 0;
	private static final long client_version = 12743913;
	
	public ActionList() {
		
	}
	
	public JSONObject toJSON() {
		JSONObject lst = new JSONObject();
		try {
			lst.put("action_list", actions);
		} catch (JSONException ex) {
			Log.e(TAG, "Couldn't convert final ActionList to JSON", ex);
		}
		return lst;
	}
	
	private JSONObject getAction(String type) {
		JSONObject action = new JSONObject();
		try {
			action.put("action_type", type);
			action.put("action_id", ++action_id);
		} catch (JSONException ex) {
			Log.e(TAG, "Couldn't create action", ex);
		}
		return action;
	}
	
	public void addGetAll(String listid) {
		//{"action_type":"get_all","action_id":"2","list_id":"08738438757279196547:0:0","get_deleted":false}
		JSONObject action = getAction("get_all");
		try {
			action.put("list_id", listid);
			action.put("get_deleted", false);
		} catch (JSONException ex) {
			Log.e(TAG, "Couldn't add action get_all", ex);
		}
		actions.put(action);
	}
}