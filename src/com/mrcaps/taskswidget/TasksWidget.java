package com.mrcaps.taskswidget;

import java.io.IOException;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.net.Uri;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.mrcaps.taskswidget.pojo.Task;
import com.mrcaps.taskswidget.pojo.TasksList;

/**
 * Define a simple widget that shows the Wiktionary "Word of the day." To build
 * an update we spawn a background {@link Service} to perform the API queries.
 */
public class TasksWidget extends AppWidgetProvider {
	private static final String TAG = "WIDGET";
	
    private static final String TESTCONTENT = "<script>function _init(){_setup({\"f\":{\"v\":true,\"d\":true,\"e\":true,\"r\":true,\"x\":true},\"v\":12743913,\"bp\":\"/tasks\",\"t\":{\"latest_sync_point\":1282316260151000,\"response_time\":1282351381,\"results\":[],\"tasks\":[{\"id\":\"08738438757279196547:0:21\",\"last_modified\":1282316260026,\"child_id\":[\"08738438757279196547:0:22\",\"08738438757279196547:0:24\"],\"name\":\"Apartment Stuff\",\"notes\":\"\",\"type\":\"TASK\",\"deleted\":false,\"list_id\":[\"08738438757279196547:0:0\"],\"completed\":false},{\"id\":\"08738438757279196547:0:22\",\"last_modified\":1282090641077,\"name\":\"Rent\",\"type\":\"TASK\",\"deleted\":false,\"list_id\":[\"08738438757279196547:0:0\"],\"completed\":false},{\"id\":\"08738438757279196547:0:24\",\"last_modified\":1282316260027,\"name\":\"Kill-a-watt\",\"notes\":\"\",\"type\":\"TASK\",\"deleted\":false,\"list_id\":[\"08738438757279196547:0:0\"],\"completed\":false}],\"user\":{\"id\":\"08738438757279196547\",\"show_tips\":false,\"auto_clear\":false,\"mobile_default_list_id\":\"08738438757279196547:0:0\",\"requested_list_id\":\"08738438757279196547:0:0\",\"default_list_id\":\"08738438757279196547:0:0\"},\"groups\":[],\"lists\":[{\"id\":\"08738438757279196547:0:0\",\"child_entity\":[{\"id\":\"08738438757279196547:0:21\"}],\"last_modified\":1282315132068,\"list_metadata\":{\"1\":1282304937737},\"name\":\"Ilari's list\",\"type\":\"GROUP\"},{\"id\":\"08738438757279196547:1:0\",\"last_modified\":1282315132677,\"list_metadata\":{\"1\":1282090656680},\"name\":\"To Get\",\"type\":\"GROUP\"},{\"id\":\"08738438757279196547:2:0\",\"last_modified\":1282315133226,\"list_metadata\":{\"1\":1282178250766},\"name\":\"Projects\",\"type\":\"GROUP\"}]},\"e\":\"mrcaps@gmail.com\",\"s\":\"res/993453956-gadget.css\",\"i\":false})}</script></head>";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
    	for (int widgetID : appWidgetIds) {
    		doUpdate(context, widgetID);
    	}
    }
    
    public static void doUpdate(Context context, int widgetID) {
    	Intent startIntent = new Intent(context, UpdateService.class);
    	startIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        context.startService(startIntent);    	
    }
    
    public static class UpdateService extends Service {    	
        @Override
        public void onStart(Intent intent, int startId) {
        	Log.v(TAG, ">>> Starting Tasks Content Download");
        	final int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
        			AppWidgetManager.INVALID_APPWIDGET_ID);
        	Log.v(TAG, ">>> This widget id=" + widgetID);
        	final Context ctx = this;
        	
			TasksHelper.prepareUserAgent(ctx);
			TasksHelper.refreshAuthToken(ctx, false, new Runnable() {
				@Override
				public void run() {
					//String taskcont = TasksHelper.getUrlContent("https://mail.google.com/tasks/ig", null);
					//TasksList tasks = TasksHelper.parseTasksFromBase(taskcont);
					
					//?listid=08738438757279196547:1:0
					String listid = ConfigureWidget.loadPrefListID(ctx, widgetID);
					TasksList tasks = null;
					try {
						if (listid == null) {
							tasks = TasksHelper.downloadDefaultList();
						} else {
							tasks = TasksHelper.downloadList(listid);
						}
					} catch (IOException ex) {
						//don't alert on update failure
					}
					
					if (tasks != null) {
						RemoteViews updateViews = buildUpdate(
								ctx, 
								tasks, 
								ConfigureWidget.loadPrefStyle(ctx, widgetID)
							);
			            
			            // Push update for this widget to the home screen
			            //ComponentName thisWidget = new ComponentName(ctx, TasksWidget.class);
			            
						AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
			            manager.updateAppWidget(widgetID, updateViews);
					} else {
						//we might have an expired auth token
						TasksHelper.refreshAuthToken(ctx, true, new Runnable() {
							public void run() {
								
							}
						});
					}
				}
			});
        }

        /**
         * Build a widget update to show the current Wiktionary
         * "Word of the day." Will block until the online API returns.
         */
        public RemoteViews buildUpdate(final Context context, TasksList tasks, int layoutID) {          
			Log.v(TAG, ">>> Building UI Update");
			
			// Build an update that holds the updated widget contents
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), layoutID);
		    
		    //ugh, we can't do this from code in cupcake, since the method isn't tagged with @RemotableViewMethod
			//updateViews.setInt(R.id.widget, "setBackgroundResource", R.drawable.widget_bg_selected);
		    
			int taskColor = 0xff333333;
			if (layoutID == R.layout.widget_tasks_dark) {
				taskColor = 0xffcccccc;
			}
			
		    Bitmap bitmap = getTasksBitmap(context, tasks, taskColor);
		    updateViews.setTextViewText(R.id.tasks_title, tasks.getName());
		    updateViews.setImageViewBitmap(R.id.tasks_list, bitmap);
		    
			// Intent to launch the mobile tasks widget
			String definePage = "https://mail.google.com/tasks/android?listid=" + 
				tasks.getRequested_list_id();
			Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(definePage));
			PendingIntent pendingIntent = PendingIntent.getActivity(context,
			        0 /* no requestCode */, defineIntent, 0 /* no flags */);
			updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);    
			
			return updateViews;
        }
        
        private Bitmap getTasksBitmap(Context ctx, TasksList tasks, int textColor) {
		    Paint p = new Paint();
		    p.setTextAlign(Paint.Align.LEFT);
		    p.setTextSize(16.0f);
		    p.setAntiAlias(true);
		    FontMetrics metrics = p.getFontMetrics();
		    float spacing = p.getFontSpacing();
		    Log.v(TAG, 
		    	"FontMetrics ascent=" + metrics.ascent + 
		    	", leading=" + metrics.leading + 
		    	", top=" + metrics.top +
		    	", spacing=" + spacing);

		    /*
		    DisplayMetrics dm = new DisplayMetrics();
		    WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		    wm.getDefaultDisplay().getMetrics(dm);
		    Log.v(TAG,
		    	"DisplayMetrics density=" + dm.density +
		    	", scaledDensity=" + dm.scaledDensity +
		    	", xdpi=" + dm.xdpi + 
		    	", ydpi=" + dm.ydpi);
		    */
		    
		    p.setColor(textColor);
		    //TODO: compute maximum text bounds
		    float addpad = metrics.ascent + spacing;
		    Bitmap bitmap = Bitmap.createBitmap(
		    		130, 
		    		(int) (spacing * tasks.size() + addpad), 
		    		Bitmap.Config.ARGB_8888);
		    Canvas c = new Canvas(bitmap);
		   
		    float top = -addpad;
		    for (Task t : tasks) {
		    	top += spacing;
		    	c.drawText(
		    		this.getString(R.string.bullet) + t.getName(), 
		    		t.getChildDepth() * 10, top, p);
		    }
		    
		    return bitmap;
        }
        
        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}
