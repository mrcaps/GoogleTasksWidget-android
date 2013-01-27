/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrcaps.taskswidget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.googlelogin.GoogleLoginServiceBlockingHelper;
import com.google.android.googlelogin.GoogleLoginServiceNotFoundException;
import com.google.android.googlelogin.GoogleLoginServiceBlockingHelper.AuthenticationException;
import com.mrcaps.taskswidget.pojo.TasksList;

public class TasksHelper {
    private static final String TAG = "TasksHelper";
    
    /**
     * User-agent string to use when making requests. Should be filled using
     * {@link #prepareUserAgent(Context)} before making any other calls.
     */
    private static String sUserAgent = null;
    
    /**
     * {@link StatusLine} HTTP status code when no server error has occurred.
     */
    private static final int HTTP_STATUS_OK = 200;
    
    /**
     * Shared buffer used by {@link #getUrlContent(String)} when reading results
     * from an API request.
     */
    private static byte[] sBuffer = new byte[512];
    
    private static String account = null;
    private static String authToken = null;
    
    //when was the auth token last invalidated?
    private static long lastInvalidation = 0;
    //a day.
    private static final long invalidationDelta = 1000*60*60*24;
    
    private static final String SERVICE_URL = "https://mail.google.com/tasks/ig";
    private static final String ACCOUNT_TYPE = "com.google";
    private static final String SERVICE_NAME = "goanna_mobile";	
    
    public static TasksList downloadDefaultList() throws IOException {
    	return parseTasksFromBase(getUrlContent(SERVICE_URL, null));
    }
    public static TasksList downloadList(String listid) throws IOException {
    	return parseTasksFromBase(getUrlContent(SERVICE_URL + "?listid=" + listid, null));
    }
    
    private static Handler handl;
    private static Handler getHandler() {
    	if (handl == null) {
    		handl = new Handler();
    	}
    	return handl;
    }
    
    /**
     * Prepare the internal User-Agent string for use. This requires a
     * {@link Context} to pull the package name and version number for this
     * application.
     */
    public static void prepareUserAgent(Context context) {
        try {
            // Read package name and version number from manifest
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            sUserAgent = String.format(context.getString(R.string.template_user_agent),
                    info.packageName, info.versionName);
            
        } catch(NameNotFoundException e) {
            Log.e(TAG, "Couldn't find package information in PackageManager", e);
        }
    }
    
    private static int version() {
    	String version = android.os.Build.VERSION.SDK;
    	int ver = 0;
    	try {
    		ver = Integer.parseInt(version);
    	} catch (Exception ex) {}
    	return ver;
    }
    
    /**
     * Refresh the auth token for the goanna_mobile service and run the continuation
     * if successful.
     * @param context
     * @param invalidate should we invalidate it?
     * @param continuation
     */
    public static void refreshAuthToken(
    		final Context context, 
    		final boolean invalidate, 
    		final Runnable continuation) {
    	
    	//Use AccountManager if it's available.
    	if (version() < 8) {
    		Log.v(TAG, "Using SDK4- auth token");
    		refreshAuthTokenImpl3(context, invalidate, continuation);
    	} else {
    		Log.v(TAG, "Using SDK5+ auth token");
    		refreshAuthTokenImpl5(context, invalidate, continuation);
    	}
    }
    
    public static void refreshAuthTokenImpl5(final Context context, final boolean invalidate, final Runnable continuation) {
    	final Handler handl = getHandler();
    	
    	new Thread(new Runnable() {
    		public void run() {
    	    	AccountManager mgr = AccountManager.get(context);
    	    	Account[] accts = mgr.getAccountsByType(ACCOUNT_TYPE);
    	    	if (accts.length < 1) {
    	    		handl.post(new Runnable() {
    	    			public void run() {
    	    				Toast.makeText(context, 
    	    					"Error: could not find a Google account on this phone",
    	    					Toast.LENGTH_SHORT).show();
    	    			}
    	    		});
    	    		return;
    	    	}
    	    	
    	    	Account acct = accts[0];
    	    	
    	    	if (invalidate) {
					long now = System.currentTimeMillis();
					Log.v(TAG, "I5 Auth Token Invalidation Requested");
					if (now - lastInvalidation > invalidationDelta) {
						Log.v(TAG, "I5 Invalidating Auth Token");
						try {
							String token = mgr.blockingGetAuthToken(acct, 
									SERVICE_NAME, true);
							mgr.invalidateAuthToken(ACCOUNT_TYPE, token);
						} catch (Exception e) {
							Log.e(TAG, "I5 couldn't invalidate token", e);
						}
					}
    	    	}
    	    	
    	    	try {
    	    		String token = mgr.blockingGetAuthToken(acct, 
							SERVICE_NAME, true);
    	    		Log.v(TAG, "I5 got auth token: " + token);
    	    		authToken = token;
    	    	} catch (Exception e) {
    	    		Log.e(TAG, "I5 couldn't authenticate", e);
					handl.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(
									context, 
									"Error: I5 could not authenticate with Google account", 
									Toast.LENGTH_SHORT).show();
						}
					});
    	    	}
    	    	
    	    	continuation.run();
    		}
    	}).start();
    }
    
    public static void refreshAuthTokenImpl3(final Context context, final boolean invalidate, final Runnable continuation) {
    	final Handler handl = getHandler();
    	
    	//doing this in the same thread leads to deadlock.
		new Thread(new Runnable() {
			public void run() {
				GoogleLoginServiceBlockingHelper helper = null;
				try {
					helper = new GoogleLoginServiceBlockingHelper(context);

					String googleAcct = helper.getAccount(true);
					Log.v(TAG, "Google account is " + googleAcct);
					account = googleAcct;
					
					if (invalidate) {
						long now = System.currentTimeMillis();
						Log.v(TAG, "Auth Token Invalidation Requested");
						if (now - lastInvalidation > invalidationDelta) {
							Log.v(TAG, "Invalidating Auth Token");
							try {
								String intoken = helper.getAuthToken(googleAcct, SERVICE_NAME);
								helper.invalidateAuthToken(intoken);
							} catch (AuthenticationException e) {
								Log.e(TAG, "Couldn't invalidate token", e);
							}
							lastInvalidation = now;
						}
					}
						
					try {
						String token = helper.getAuthToken(googleAcct, SERVICE_NAME);
						Log.v(TAG, "Got auth token:" + token);
						authToken = token;
					} catch (AuthenticationException e) {
						Log.e(TAG, "Couldn't authenticate", e);
						handl.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(
										context, 
										"Error: could not authenticate with Google account", 
										Toast.LENGTH_SHORT).show();
							}
						});

					}

				} catch (GoogleLoginServiceNotFoundException e) {
					Log.e(TAG, "Couldn't find login service - please contact us!", e);
					handl.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(
									context, 
									"Error: could not find Google Login service - please contact us!", 
									Toast.LENGTH_SHORT).show();
						}
					});
					
				} finally {
					if (helper != null) {
						helper.close();
					}
					continuation.run();
				}
			}
		}).start();
    }
    
    public static TasksList parseTasks(String input) {
    	TasksList tasks = null;
    	try {
			tasks = new TasksList(new JSONObject(input));
		} catch (JSONException ex) {
			Log.e(TAG, "Couldn't parse tasks list", ex);
		}
		return tasks;
    }
    
    private static final String START_STR = "_setup(";
    private static final String END_STR = "</script>";
    private static final int BRACES_NO = 2;
    public static TasksList parseTasksFromBase(String raw) {
    	Log.v(TAG, "Parsing tasks");
    	TasksList tasks = null;
    	
    	//scan for the signal tokens
    	int startdx = raw.indexOf(START_STR);
    	
    	if (startdx > -1) {
    		int enddx = raw.indexOf(END_STR, startdx);
    		if (enddx > -1) {
    			//HACK: count a few braces in
    			int braces = 0;
    			for (; enddx > startdx && braces < BRACES_NO; --enddx) {
    				if (raw.charAt(enddx) == '}') {
    					++braces;
    				}
    			}
    			
    			Log.v(TAG, "parse start=" + startdx + " end=" + enddx + " braces=" + braces);
    			
    			if (braces == BRACES_NO) {
    				String toparse = raw.substring(startdx+START_STR.length(), enddx+2);
    				JSONObject outer = null;
    				try {
    					outer = new JSONObject(toparse);
    					if (outer.has("t")) {
    						tasks = new TasksList(outer.getJSONObject("t"));
    						//Log.v(TAG, "Tasks = " + tasks.toString());
    					} else {
    						Log.e(TAG, "Couldn't find tasks object");
    					}
    				} catch (JSONException ex) {
    					Log.e(TAG, "JSON parsing Exception", ex);
    				}
    			}
    		}
    	} else {
    		Log.v(TAG, "Couldn't find starting string in " + raw);
    	}
    	
    	return tasks;
    }
    
    /**
     * Pull the raw text content of the given URL. This call blocks until the
     * operation has completed, and is synchronized because it uses a shared
     * buffer {@link #sBuffer}.
     * 
     * @param url The exact URL to request.
     * @param nvps name value pairs to send over as POST
     * @return The raw content returned by the server.
     * @throws ApiException If any connection or server error occurs.
     */
    protected static synchronized String getUrlContent(String url, List<NameValuePair> nvps) throws IOException {
        if (sUserAgent == null) {
            throw new RuntimeException("User-Agent string must be prepared");
        }
        
        // Create client and set our specific user-agent string
        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost(url);
        request.setHeader("User-Agent", sUserAgent);
        Log.v(TAG, "Using authorization " + authToken);
        request.setHeader("Authorization", "GoogleLogin auth=" + authToken);

        if (nvps != null) {
	        try {
	        	request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
	        } catch (UnsupportedEncodingException ex) {
	        	throw new RuntimeException("Couldn't encode r param", ex);
	        }
        }
        	
        HttpResponse response = client.execute(request);
        
        // Check if server response is valid
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != HTTP_STATUS_OK) {
            throw new RuntimeException("Invalid response from server: " +
                    status.toString());
        }

        // Pull content stream from response
        HttpEntity entity = response.getEntity();
        InputStream inputStream = entity.getContent();
        
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        
        // Read response into a buffered stream
        int readBytes = 0;
        while ((readBytes = inputStream.read(sBuffer)) != -1) {
            content.write(sBuffer, 0, readBytes);
        }
        
        // Return result from buffered stream
        return new String(content.toByteArray());
    }
    
}
