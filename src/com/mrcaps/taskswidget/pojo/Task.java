package com.mrcaps.taskswidget.pojo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Task {
	private static final String TAG = "POJO-Task";
	
	String id = null;
	long last_modified = 0;
	String name = null;
	String notes = "";
	String type = "TASK";
	boolean deleted = false;
	ArrayList<String> list_id = new ArrayList<String>();
	ArrayList<String> child_id = new ArrayList<String>();
	boolean completed = false;
	
	//not serialized
	//is this task a child of another task?
	ArrayList<Task> children = null;
	int childDepth = 0;
	
	public Task(JSONObject obj) throws JSONException {
		id = obj.getString("id");
		last_modified = obj.getLong("last_modified");
		name = obj.getString("name");
		if (obj.has("notes")) {
			notes = obj.getString("notes");
		}
		if (obj.has("child_id")) {
			JSONArray jsch = obj.getJSONArray("child_id");
			for (int x = 0; x < jsch.length(); ++x) {
				child_id.add(jsch.getString(x));
			}
		}
		type = obj.getString("type");
		deleted = obj.getBoolean("deleted");
		
		JSONArray listids = obj.getJSONArray("list_id");
		for (int x = 0; x < listids.length(); ++x) {
			list_id.add(listids.getString(x));
		}
		
		completed = obj.getBoolean("completed");
	}
	
	public void findChildren(List<Task> candidates) {
		if (children == null && shouldHaveChildren()) {
			children = new ArrayList<Task>();
			
			for (String subid : child_id) {
				for (Task c : candidates) {
					if (c.id.equals(subid)) {
						children.add(c);
						c.childDepth = this.childDepth + 1;
					}
				}
			}
			
			if (children.size() != child_id.size()) {
				Log.e(TAG, "Did not find all children of task " + id);
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public boolean shouldHaveChildren() {
		return child_id.size() != 0;
	}
	
	public int getChildDepth() {
		return childDepth;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Task [childDepth=");
		builder.append(childDepth);
		builder.append(", child_id=");
		builder.append(child_id);
		builder.append(", children=");
		builder.append(children);
		builder.append(", completed=");
		builder.append(completed);
		builder.append(", deleted=");
		builder.append(deleted);
		builder.append(", id=");
		builder.append(id);
		builder.append(", last_modified=");
		builder.append(last_modified);
		builder.append(", list_id=");
		builder.append(list_id);
		builder.append(", name=");
		builder.append(name);
		builder.append(", notes=");
		builder.append(notes);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}
}
