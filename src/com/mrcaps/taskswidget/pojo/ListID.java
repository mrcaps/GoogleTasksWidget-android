package com.mrcaps.taskswidget.pojo;

import org.json.JSONException;
import org.json.JSONObject;

public class ListID {
	/*
	{
        "id": "08738438757279196547:1:0",
        "last_modified": 1282786982509,
        "list_metadata": {
            "1": 1282090656680
        },
        "name": "To Get",
        "type": "GROUP"
    },
	*/
	String id;
	public String getId() {
		return id;
	}
	
	long last_modified;
	
	String name;
	public String getName() {
		return name;
	}
	
	public ListID(JSONObject obj) throws JSONException {
		id = obj.getString("id");
		last_modified = obj.getLong("last_modified");
		name = obj.getString("name");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ListID [id=");
		builder.append(id);
		builder.append(", last_modified=");
		builder.append(last_modified);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}
}
