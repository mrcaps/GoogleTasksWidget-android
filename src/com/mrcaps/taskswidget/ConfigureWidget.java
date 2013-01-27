package com.mrcaps.taskswidget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.mrcaps.taskswidget.pojo.ListID;
import com.mrcaps.taskswidget.pojo.TasksList;

public class ConfigureWidget extends Activity {
	private static final String TAG = "ConfigureWidget";
	
    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
	
    private Handler handl;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		handl = new Handler();
		
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        
        setTitle(R.string.config_title_text);
        setContentView(R.layout.configure);
        
        findViewById(R.id.configure_add).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishConfigure();
			}
		});
        
        // Find the widget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
        	widgetID = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        
        // Load up the list of lists
        final Context ctx = this;
        TasksHelper.prepareUserAgent(ctx);
		TasksHelper.refreshAuthToken(ctx, false, 
	        new Runnable() {
	        	public void run() {
	        		tryParsingTasks(ctx, 0);
	        	}
	        }); 
	}
	
	private void tryParsingTasks(final Context ctx, final int attempts) {
		try {
    		final TasksList def = TasksHelper.downloadDefaultList();
    		handl.post(new Runnable() {
    			public void run() {
    				if (def == null) {
    					if (attempts == 0) {
    						//try invalidating the auth token if we fail.
    						TasksHelper.refreshAuthToken(ctx, true, 
    						        new Runnable() {
    						        	public void run() {
    						        		tryParsingTasks(ctx, 1);
    						        	}
    						        });
    					} else {
    						Toast.makeText(ctx, R.string.config_download_error, Toast.LENGTH_LONG).show();
    					}
    					finish();
    				} else {
    					finishLoad(def);
    				}
    			}
    		});
		} catch (IOException ex) {
			handl.post(new Runnable() {
				public void run() {
					Toast.makeText(ctx, R.string.config_network_error, Toast.LENGTH_LONG).show();
					finish();
				}
			});
		}	
	}
	
	private HashMap<String, String> nameToID = new HashMap<String, String>();
	private String defaultID;
	
	private void finishLoad(TasksList list) {
		defaultID = list.getRequested_list_id();
		for (ListID lid : list.getLists()) {
			nameToID.put(lid.getName(), lid.getId());
		}
		
		int defPos = 0;
		ArrayList<String> titles = new ArrayList<String>(nameToID.keySet());
		for (int x = 0; x < titles.size(); ++x) {
			String title = titles.get(x);
			if (nameToID.get(title).equals(defaultID)) {
				defPos = x;
			}
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				this, 
				android.R.layout.simple_spinner_item,
				titles
				);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spin = ((Spinner) findViewById(R.id.configure_lists));
		spin.setAdapter(adapter);
		spin.setSelection(defPos);
		
	    Gallery g = (Gallery) findViewById(R.id.configure_gallery);
	    g.setAdapter(new BackgroundImageAdapter(this));
		
		((ViewSwitcher) findViewById(R.id.configure_switcher)).showNext();
	}
	
	private void finishConfigure() {
        final Context context = this;
        
        //todo: actually do the configuration
        Spinner spin = ((Spinner) findViewById(R.id.configure_lists));
        String listName = (String) spin.getSelectedItem();
        if (!nameToID.containsKey(listName)) {
        	Log.e(TAG, "Consistency error in list selection");
        }
        savePrefListID(this, widgetID, nameToID.get(listName));
        
        Gallery g = (Gallery) findViewById(R.id.configure_gallery);
        savePrefStyle(this, widgetID, (int) g.getSelectedItemId());
        
        //Push the widget update
        TasksWidget.doUpdate(context, widgetID);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_OK, resultValue);
        finish();
	}
	
    private static final String PREFS_NAME = 
    	"com.mrcaps.taskswidget.TasksWidget";
	private static final String PREF_KEY_LISTID = "listid_";
	private static final String PREF_KEY_STYLE = "style_";
	
	private static SharedPreferences.Editor getPrefsEditor(Context context) {
		return context.getSharedPreferences(PREFS_NAME, 0).edit();
	}
	private static SharedPreferences getPrefs(Context context) {
		return context.getSharedPreferences(PREFS_NAME, 0);
	}
	
	public static void savePrefListID(Context context, int widget, String listid) {
        SharedPreferences.Editor prefs = getPrefsEditor(context);
        prefs.putString(PREF_KEY_LISTID + widget, listid);
        prefs.commit();
    }
	public static String loadPrefListID(Context context, int widget) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(PREF_KEY_LISTID + widget, null);
	}
	
	public static void savePrefStyle(Context context, int widget, int style) {
		SharedPreferences.Editor prefs = getPrefsEditor(context);
		prefs.putInt(PREF_KEY_STYLE + widget, style);
		prefs.commit();
	}
	public static int loadPrefStyle(Context context, int widget) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getInt(PREF_KEY_STYLE + widget, R.layout.widget_tasks_dark);		
	}
}
