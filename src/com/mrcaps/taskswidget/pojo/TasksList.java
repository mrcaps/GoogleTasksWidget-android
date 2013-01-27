package com.mrcaps.taskswidget.pojo;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class TasksList implements Iterable<Task> {
	private static String TAG = "TasksList";
	
	long latest_sync_point = 0;
	long response_time = 0;
	ArrayList<Task> tasks = new ArrayList<Task>();
	ArrayList<ListID> lists = new ArrayList<ListID>();
	public ArrayList<ListID> getLists() { return lists; }
	
	String requested_list_id;
	public String getRequested_list_id() {
		return requested_list_id;
	}
	String name = "Tasks";
	
	public TasksList(JSONObject lst) throws JSONException {
		latest_sync_point = lst.getLong("latest_sync_point");
		response_time = lst.getLong("response_time");
		
		JSONArray alltasks = lst.getJSONArray("tasks");
		for (int x = 0; x < alltasks.length(); ++x) {
			tasks.add(new Task(alltasks.getJSONObject(x)));
		}
		
		if (lst.has("lists")) {
			JSONArray alllists = lst.getJSONArray("lists");
			for (int x = 0; x < alllists.length(); ++x) {
				lists.add(new ListID(alllists.getJSONObject(x)));
			}
		}
		
		if (lst.has("user")) {
			JSONObject user = lst.getJSONObject("user");
			requested_list_id = user.getString("requested_list_id");
			//Log.v(TAG, "requested listid was " + requested_list_id);
			
			//try to reference the name
			for (ListID list : lists) {
				//Log.v(TAG, "listid was " + list.id);
				
				if (list.id.equals(requested_list_id)) {
					//Log.v(TAG, "Found name:" + list.name);
					this.name = list.name;
				}
			}
		} else {
			Log.v(TAG, "No user object found");
		}
		
		findChildren();
	}
	
	private void findChildren() {
		//don't bother building a map here.
		for (Task t : tasks) {
			if (t.shouldHaveChildren()) {
				t.findChildren(tasks);
			}
		}
	}
	
	public int size() {
		return tasks.size();
	}

	@Override
	public Iterator<Task> iterator() {
		return tasks.iterator();
	}
	
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TasksList [latest_sync_point=");
		builder.append(latest_sync_point);
		builder.append(", lists=");
		builder.append(lists);
		builder.append(", response_time=");
		builder.append(response_time);
		builder.append(", tasks=");
		builder.append(tasks);
		builder.append("]");
		return builder.toString();
	}
}
