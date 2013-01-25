package org.busbrothers.anystop.agencytoken.map;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.WMATATransitDataManager;
import org.busbrothers.anystop.agencytoken.activities.AgencyRouteList;
import org.busbrothers.anystop.agencytoken.activities.EditPrefs;
import org.busbrothers.anystop.agencytoken.activities.RouteList;
import org.busbrothers.anystop.agencytoken.activities.StopList;
import org.busbrothers.anystop.agencytoken.datacomponents.Agency;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.sensedk.AswAdLayout;


public class ChooseMap extends MapActivity {
	
	static ChooseMap me;
	
	private ProgressDialog pd, locd;
	
	private static final int START=234, END=237, LOAD=2;
	public static ChooseMap single() {
		return me;
	}
	String locString;
	Button routeButton, startButton, stopButton;
	LinearLayout zoomBox;
	MyView mapView;
	View routeView;
	ZoomControls mZoom;
	List<Overlay> mapOverlays;
	Drawable start, end;
	ChooseMapItemizedOverlay startOverlay, endOverlay;
	OverlayItem startItem, endItem;
	Projection p;
	MyLocationOverlay myLocationOverlay;
	Dialog dialog;
    static GeoPoint startpoint = null;
    static GeoPoint endpoint = null;
    
    int startingLat, startingLon, startingZoom; //Define the starting map location and zoom level for this Agency
    
    static final String activityNameTag = "ChooseMap";
    
    private static boolean first = true; //!< Determine if this is the first time we have launched ChooseMap; display instructions if it is.
    private static long lastLocationSetTime=-1;
    private boolean flurryEventLogged;
    
    //Will be used to determine the user's current location and create a new LocThread to accept that location
    LocationManager locationManager;
    LocationListener locationListener;
    Location previousLocation;
    static long lastLocThreadCreationTime; //This will prevent us from creating too many location threads
    boolean userHasClickedSetLocation; //This will be set to true as soon as user clicks "Set Location". It will suppress call-backs from
    	//the locationListener.
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        me=this;
        
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.choosemap);
        
        mapView = (MyView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
		
        startButton = (Button) findViewById(R.id.choose);
        routeButton = (Button) findViewById(R.id.go_routes);
        stopButton = (Button) findViewById(R.id.go_stops);
        
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	userHasClickedSetLocation = true;
            	Log.v(activityNameTag,"MapView zoom level was set to " + mapView.getZoomLevel());
            	showDialog(START);
            }
        }); 
        routeButton.setOnClickListener(routeMe);
        stopButton.setOnClickListener(stopMe);


        p = mapView.getProjection();
        //mapView.layout(0, 0, 320, 400); //Ivan: I don't think this does anything
        mapOverlays = mapView.getOverlays();
        
        if (startpoint==null) {
        	startpoint = p.fromPixels(mapView.getWidth()/2, mapView.getHeight()/2);
        }
        
        start = this.getResources().getDrawable(R.drawable.mapstart);
        
        startOverlay = new ChooseMapItemizedOverlay(start);
        startItem = new OverlayItem(startpoint, "", "");
        startOverlay.addOverlay(startItem);

        mapOverlays.add(startOverlay);
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        mapOverlays.add(myLocationOverlay);
        myLocationOverlay.enableMyLocation();
        
        //Get starting latitude and starting longitude and starting zoom level from strings set at compile time
        //-999 is a magic "invalid number"
        try {
	        String startingLatStr = (String) this.getString(R.string.agencyLat);
	        startingLat = (startingLatStr != null) ? Integer.parseInt(startingLatStr) : -999;
	        String startingLonStr = (String) this.getString(R.string.agencyLon);
	        startingLon = (startingLonStr != null) ? Integer.parseInt(startingLonStr) : -999;
	        String startingZoomStr = (String) this.getString(R.string.agencyZoomLevel);
	        startingZoom = (startingZoomStr != null) ? Integer.parseInt(startingZoomStr) : -999;
        } catch (NumberFormatException e) {
        	startingLat = -999;
        	startingLon = -999;
        	startingZoom = 11;
        }
        
        //Use the numbers we acquired above to set starting position and zoom level
    	MapController myMapController = mapView.getController();
    	if(startingLat != -999 && startingLon != -999 && startingZoom != -999) {
    		myMapController.setCenter(new GeoPoint(startingLat, startingLon));
    		myMapController.setZoom(startingZoom);
    	} else Log.w(activityNameTag, "Warning: Couldn't get starting lat/lon/zoom for MapView");
    	
    	//Try and set the current location to where the user is now
    	//LocThread t = new LocThread(true, this);
        //t.start();
    	//Start the LocationListener. Have it set the location of the user if we get a valid location from the system
    	final int seconds_between_loc_updates = 5; //determine how many seconds (minimum) between location updates from location listeners
    	userHasClickedSetLocation = false;
    	previousLocation = null;
    	lastLocThreadCreationTime = System.currentTimeMillis()-((seconds_between_loc_updates+1) * 1000);
    	locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
    	locationListener = new LocationListener() {

    		public void onLocationChanged(Location location) {
    			//When the location changes, create a new data thread to update the user's location
    			//IF the user hasn't clicked "Set Location" AND we haven't updated in the last seconds_between_loc_updates seconds
    			if(!userHasClickedSetLocation && (lastLocThreadCreationTime <= System.currentTimeMillis()-(seconds_between_loc_updates*1000))) {
    				Log.d(activityNameTag, "LocationListener has detected a location change, making a new LocThread about it.");
	    			LocThread t = new LocThread(location);
	    			t.run();
	    			lastLocThreadCreationTime = System.currentTimeMillis();
    			}
    			else if (lastLocThreadCreationTime > System.currentTimeMillis()-(seconds_between_loc_updates*1000))
    				Log.d(activityNameTag, "LocationListener has detected a location change BUT we've updated in the last 10 seconds");
    			else Log.d(activityNameTag, "LocationListener has detected a location change BUT user clicked a button so no LocThread made.");
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
    	};
    	//2nd parameter specifies minimum time between updates
    	try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5*1000, 0, locationListener); }
    	catch (IllegalArgumentException e) {;}
    	
    	try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5*1000, 0, locationListener); }
    	catch (IllegalArgumentException e) {;}
        
        //Apply new typeface to all Views in this listview
        Manager.applyFonts((View) findViewById(R.id.mainlayout));
        
        flurryEventLogged = false; //will be re-set to true once we run onStart() and log the flurry event
        
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		if (Manager.isUseUsage(this) && first) {
			Toast t = Toast.makeText(this, "Welcome to the Map!", Toast.LENGTH_LONG);
			//t.show();
			
			t = Toast.makeText(this, "Press 'Set Location' to put the start where you want.'", Toast.LENGTH_LONG);
			t.show();
			
			t = Toast.makeText(this, "You can use 'My Location', or type an address/intersection.", Toast.LENGTH_SHORT);
			t.show();
			
			t = Toast.makeText(this, "Press 'Routes' to get a list of routes near your choice.", Toast.LENGTH_SHORT);
			t.show();
			
			t = Toast.makeText(this, "Pressing 'Stops' to get a list of stops near your choice.", Toast.LENGTH_SHORT);
			t.show();
			
			t = Toast.makeText(this, "You can turn off screen instructions in the Preferences.", Toast.LENGTH_SHORT);
			t.show();
		}
		
		if(first) {
	        Log.v(activityNameTag, "Running ChooseMap's initial location setting script.");
		}
		
		first = false;
		
		//Report opening of StopsTime to Flurry Analytics
		//For some reason it'll crash with "no API key specified". Not sure how to continue from this.
		try {
			FlurryAgent.onStartSession(this, Manager.getFlurryAPIK());
		} catch(IllegalArgumentException e) { ; } 
		if(! flurryEventLogged) {
			Manager.flurryActivityOpenEvent(activityNameTag);
			flurryEventLogged = true;
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);	
	}
    
	
	
    @Override
	protected void onPause() {
		super.onPause();
		Log.v(activityNameTag, "stopping location provider...");
		this.myLocationOverlay.disableMyLocation(); //Manage MyLocationOverlay LocationManager
		this.mapView.stopLoc(); //manage MyView location manager
		locationManager.removeUpdates(locationListener); //manage local LocationManager
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		Log.v(activityNameTag, "starting location provider...");
		this.myLocationOverlay.enableMyLocation();
		this.mapView.initLoc();
		
		try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5*1000, 0, locationListener); }
    	catch (IllegalArgumentException e) {;}
    	
    	try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5*1000, 0, locationListener); }
    	catch (IllegalArgumentException e) {;}
		
		LinearLayout adLayoutParent = (LinearLayout) findViewById(R.id.dlist_ad_holder); //this LinearLayout will contain the AdView
		
		//The below code sets up Advertisement-related stuff for this activity
		if(Manager.adTypeUsing() == Manager.ADMOB_AD) {			
			//The below block of code sets up the AdView for this Activity, and adds it to the Layout
			AdView googleAdMobAd = Manager.setupAdView(this); //have Manager set up the AdView for us :)
			adLayoutParent.addView(googleAdMobAd);
		}
		else if(Manager.adTypeUsing() == Manager.ADDIENCE_AD) {
			AswAdLayout adView = Manager.makeAddienceAd(this);
			adLayoutParent.addView(adView);
		}
		
		adLayoutParent.invalidate();
	}

	protected Dialog onCreateDialog(int d) {
    	
        dialog = new Dialog(this);
        Button find;
        Button guess;
        EditText loc;
        
//        if (!isDlgShowing) {
//        	return dialog;
//        }
        
        switch (d) {
        	case START:
        		dialog.setContentView(R.layout.locator_dialog);
                dialog.setTitle("Choose Start Location");
                loc = (EditText) dialog.findViewById(R.id.locatortext);
                loc.setText("Address or Intersection");
                loc.setTextColor(Color.GRAY);
                
                /*loc.setOnKeyListener(new View.OnKeyListener() {
                	public boolean onKey(View v, int i, KeyEvent e) {
                		
                		if (((EditText)v).getText().toString().equalsIgnoreCase("Address or Intersection")) {
                			((EditText)v).setText(e.getCharacters());
                			((EditText)v).setTextColor(Color.BLACK);
                		}
                		return false;
                	}
                });*/
                loc.setOnClickListener(new View.OnClickListener() {
                	public void onClick(View v) {
                		((EditText)v).setText("");
            			((EditText)v).setTextColor(Color.BLACK);
                	}
                });
                
                find = (Button) dialog.findViewById(R.id.find);
                find.setOnClickListener(new View.OnClickListener() {
                	public void onClick(View v) {
                        EditText startbox = (EditText) dialog.findViewById(R.id.locatortext);
                        locString = startbox.getText().toString();
                        if(locString.equals("Address or Intersection")) locString = "My Location";
                        removeDialog(START);
                        lochandler.sendEmptyMessage(0);
                	}
                });
                
                guess = (Button) dialog.findViewById(R.id.use_current);
                guess.setOnClickListener(new View.OnClickListener() {
                	public void onClick(View v) {
                        locString = "My Location";
                        removeDialog(START);
                        lochandler.sendEmptyMessage(0);
                	}
                });
                ImageView startImage = (ImageView) dialog.findViewById(R.id.image);
                startImage.setImageResource(R.drawable.mapstart);
                dialog.show();
              
        		break;
        }
        
        
        
        return dialog;
    }
    
    private Handler lochandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==4) {
    			Toast.makeText(me, "Sorry, your current position could not be determined!", 2000).show();
    			return;
			} else if (msg.what==6) {
				Toast.makeText(me, "Sorry, could not get coordinates for the specified location!", 2000).show();
			} else if (msg.what==5){
				mapView.invalidate();
			} else {
	            locd = ProgressDialog.show(me, "Geocoding", "Geocoding your request...", true, false);
	            LocThread t = new LocThread();
	            t.start();
			}
		}
    };
    

	@Override
    protected boolean isRouteDisplayed() {
        return true;
    }
	@Override
    protected boolean isLocationDisplayed() {
        return true;
    }
    
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mapmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.prefs:
				startActivity(new Intent(this, EditPrefs.class));  
				return(true);
				
			case R.id.about:
				Manager.aboutDialog(this).show();
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}
	
	
	
	OnClickListener routeMe = new OnClickListener(){
        // @Override
        public void onClick(View arg0) {
        	Manager.viewing = Manager.ROUTES;
        	handler.sendEmptyMessage(0);
        	Manager.flurryUserUsedMapEvent(locString, !userHasClickedSetLocation);
        	
        	Log.v(activityNameTag,"MapView zoom level was set to " + mapView.getZoomLevel());
        } 
	};
	
	OnClickListener stopMe = new OnClickListener(){
        // @Override
        public void onClick(View arg0) {
        	Manager.viewing = Manager.STOPS;
        	handler.sendEmptyMessage(0);
        	Manager.flurryUserUsedMapEvent(locString, !userHasClickedSetLocation);
        	
        	Log.v(activityNameTag,"MapView zoom level was set to " + mapView.getZoomLevel());
        } 
	};
	
	
	class DataThread extends Thread {
		public void run() {
			if (Manager.viewing == Manager.AGENCY || Manager.viewing == Manager.FAVROUTES) {
				try {
					Agency a = new Agency();
					a.name = Manager.getAgencyDisplayName();
					a.table = Manager.getTableName();
					a.isRTstr = Manager.get_predictionType();
					Manager.currAgency = a;
					Manager.loadAgencyRoutes(a);
				} catch (ServerBarfException e) {
					errorHandler.sendEmptyMessage(1); return;
				}
				pd.dismiss();
				stact.sendEmptyMessage(0);
			}
			
			else if(Manager.isWMATA()) {
				if (Manager.viewing==Manager.ROUTES) {
					//Date starttime = new Date();
					Manager.clearStops();
	
					Location loc = new Location("faked");
					loc.setLatitude(startpoint.getLatitudeE6()/10e6);
					loc.setLongitude(startpoint.getLongitudeE6()/10e6);
					try {
						WMATATransitDataManager.fetchRoutesByLocation(loc);
					} catch (ServerBarfException e) {
						errorHandler.sendEmptyMessage(1); return;
					}
					
					HashMap<Route, ArrayList<SimpleStop>> retval = 
							(HashMap<Route, ArrayList<SimpleStop>>)WMATATransitDataManager.peekLastData();
					
					if(retval == null || retval.keySet().size() == 0) {
						errorHandler.sendEmptyMessage(0); return;
					}
					
					pd.dismiss();
					stact.sendEmptyMessage(0);
				} else if (Manager.viewing==Manager.STOPS) {
					//Date starttime = new Date();
					Manager.clearStops();
					
					Log.d("ChooseMap", "Got to here!");
	
					Location loc = new Location("faked");
					loc.setLatitude(startpoint.getLatitudeE6()/10e6);
					loc.setLongitude(startpoint.getLongitudeE6()/10e6);
					try {
						WMATATransitDataManager.fetchStopsByLocation(loc);
					} catch (ServerBarfException e) {
						errorHandler.sendEmptyMessage(1); return;
					}
					
					ArrayList<SimpleStop> retval = 
							(ArrayList<SimpleStop>)WMATATransitDataManager.peekLastData();
					
					if(retval == null || retval.size() == 0) {
						errorHandler.sendEmptyMessage(0); return;
					}
					
					pd.dismiss();
					stact.sendEmptyMessage(0);
				}
				
			} else {
				if (Manager.viewing==Manager.ROUTES || Manager.viewing==Manager.STOPS) {
					//Date starttime = new Date();
					Manager.clearStops();
	
					Location loc = new Location("faked");
					loc.setLatitude(startpoint.getLatitudeE6()/10e6);
					loc.setLongitude(startpoint.getLongitudeE6()/10e6);
					try {
						Manager.loadNearStops(loc, Manager.getTableName());
					} catch (NoneFoundException e) {
						errorHandler.sendEmptyMessage(0); return;
					} //Needs to be threaded itself!
					catch (ServerBarfException e) {
						errorHandler.sendEmptyMessage(1); return;
					}
					pd.dismiss();
					stact.sendEmptyMessage(0);
				}
				
			}

		}
	};
	
	/** This class handles GeoCoding a user's location entry, and setting the map to point to that location 
	 * @author ivany
	 *
	 */
	class LocThread extends Thread {
		boolean useManagerLocation; //!<useManagerLocation basically indicates if LocThread was called from a listener, rather than from user input.
		Activity caller;
		Location givenLocation; //!<givenLocation represents a location provided directly to this LocThread
		
		/** Does nothing special other than set useManagerLocation to false.
		 * 
		 */
		LocThread () {
			super();
			useManagerLocation=false;
			givenLocation = null;
		}
		
		/** If this constructor is used, we have the option of telling the LocThread to skip the GeoCoding
		 * step and to directly use the Manager's location
		 * @param museManagerLoc Whether to use Manager.getMyLoc() 
		 * @param mcaller The calling Activity
		 */
		LocThread(boolean museManagerLoc, Activity mcaller) {
			super();
			caller = mcaller;
			useManagerLocation = museManagerLoc; 
			givenLocation = null; 
		}
		
		/** This thread constructor allows the LocThread to be given a location (for example, from a locationListener). The thread 
		 * will check this Location and it if meets criteria it will animateTo the location directly
		 * 
		 * @param givenLocation
		 */
		LocThread(Location pgivenLocation) {
			super();
			useManagerLocation = true;
			givenLocation = pgivenLocation;
		}
		
		public void run() {
			String string = locString;
			GeoPoint g;
			
			/*This conditional gets entered if we are NOT calling LocThread from the "Set Location" dialog
			* In this conditional, we will set the location IF
			* (1) User Location is available
			* (2) It is less than 1 hour (3600000ms) old
			* (3) It is within 100km of Agency lat/lon center
			* (4) It has been at least 10 minute since we have re-set the users location in this way
			*/
			if(useManagerLocation) {
				if(givenLocation == null)
					Log.d(activityNameTag, "LocThread attempting to get your location.");
				else
					Log.d(activityNameTag, "LocThread going to use the provided location.");
				
				//If we were given a valid location (in the constructor) use that, otherwise ask the Manager for the location
				Location myLoc = (givenLocation!=null)?givenLocation : Manager.getMyLoc(caller);					
				
				final boolean ignoreLocAgeAndDistance = false;

				if (myLoc == null) { //user location is unavailable
					//lochandler.sendEmptyMessage(6);
					Log.d(activityNameTag, "LocThread could not get your location.");
					g=null;
				}
				else if(System.currentTimeMillis() - myLoc.getTime() > 3600000) { //over 1 hour old
					//lochandler.sendEmptyMessage(6);
					Log.d(activityNameTag, "LocThread got a stale location, over 1 hour old.");
					g=null;
				}
				else if(!ignoreLocAgeAndDistance && (SystemClock.elapsedRealtime() - lastLocationSetTime) < (10 * 60 * 1000)) { //we reset the location less than 10 minutes ago 
					//lochandler.sendEmptyMessage(6);
					Log.d(activityNameTag, "LocThread not updating location on principle; we updated it less than 10 minutes ago.");
					g=null;
				}
				else {
					int lat = (int) (myLoc.getLatitude()*1000000);
					int lon = (int) (myLoc.getLongitude()*1000000);
					
					//Check to see if the current location is over 100kM away from Agency center
					double distToAgencyCtr = GeoUtils.distanceKm(myLoc.getLatitude(), myLoc.getLongitude(), ((double)startingLat)/1000000.0, 
							((double)startingLon)/1000000.0);
					if(!ignoreLocAgeAndDistance && distToAgencyCtr > 100.0) {
						//lochandler.sendEmptyMessage(6);
						Log.d(activityNameTag, "LocThread not updating location since it is over 100km away from Agency center " +
								"(it was " + distToAgencyCtr + "km away)");
						Log.d(activityNameTag, "Comparing point=(" + myLoc.getLatitude() + ", " + myLoc.getLongitude() + ") to starting_point=("
								+ ((double)startingLat)/1000000.0 + ", " + ((double)startingLon)/1000000.0 + ")");
						g=null;
					}
					else {					
						g = new GeoPoint(lat,lon);
						Log.d(activityNameTag, "LocThread was successful in getting and setting your location.");
					}
				}
			}
			else if (string.equalsIgnoreCase("My Location")) {
				g=myLocationOverlay.getMyLocation();
	    		if (g==null) {
	    			lochandler.sendEmptyMessage(4);
	    		}
	    	} else {
		    	String encoded = URLEncoder.encode(string);
		    	String url = "http://maps.google.com/maps/geo?q=" + 
		    		encoded + "&output=csv&oe=utf8&sensor=false&key=" + R.string.maps_apik;
		    	String s;
		    	InputStream is;
		    	DataInputStream dis;
		    	StringBuffer b = new StringBuffer();
				try {
					URL u = new URL(url);
					is = u.openStream();
					dis = new DataInputStream(new BufferedInputStream(is));
					while ((s = dis.readLine()) != null) {
						b.append(s);
					}
				} 	catch (Exception e) {
					Log.w(activityNameTag, "Could not geocode request!");
					if(!useManagerLocation) locd.dismiss();
					return; //end run() if we were unable to geocode the request, there is no reason we could continue...
				}
				
				String[] res = b.toString().split(",");
				
				if(res.length < 4) {
					Log.e(activityNameTag, "Invalid string from google geocoding request, terminating LocThread.");
					if(!useManagerLocation) locd.dismiss();
					return;
				}
				
				int lat = (int) (Double.parseDouble(res[2])*1000000);
				int lon = (int) (Double.parseDouble(res[3])*1000000);
				if (lat==0 || lon==0) {
					lochandler.sendEmptyMessage(6);
					g=null;
				} else {
					g = new GeoPoint(lat,lon);
					
				}
	    	}
			
			//Don't need to dismiss locd if userManagerLocation because that means we didn't create a locd to begin with :)
			if(!useManagerLocation) locd.dismiss();
	    	
			//This would only happen if for some reason we couldn't find the current location
			if (g==null) {
				return;
			}
			
			Log.v(activityNameTag, "Decided that your location was (" + g.getLatitudeE6() + ", " + g.getLongitudeE6() + ")");
			
			mapOverlays.remove(startOverlay);
	        startOverlay = new ChooseMapItemizedOverlay(start);
	        startpoint = g;
	        startItem = new OverlayItem(g, "", "");
	        startOverlay.addOverlay(startItem);
	        mapOverlays.add(startOverlay);
	        lochandler.sendEmptyMessage(5);
	        
			MapController m = mapView.getController();
			
			//Disable animateTo behavior if this LocThread was created by LocationListener AND the new location
			//is within location_threshold km of the old one
			//Check if it was within distanceThreshold kilometers of the old location
			final double distanceThreshold = 0.05; //threshold for distance where we animateTo
			if(previousLocation != null && givenLocation != null) {
				double distanceBetweenOldAndNew = GeoUtils.distanceKm(givenLocation.getLatitude(), givenLocation.getLongitude(),
						previousLocation.getLatitude(), previousLocation.getLongitude());
				
				if(distanceBetweenOldAndNew > distanceThreshold) {
					Log.v(activityNameTag,"Distance to previous location was " + distanceBetweenOldAndNew + ", animating to point");
					previousLocation = givenLocation;
					m.animateTo(g);
				}
				else Log.v(activityNameTag,"Distance to previous location was " + distanceBetweenOldAndNew + ", not animating or anything.");
			}
			else {
				if(givenLocation != null) previousLocation = givenLocation;
				m.animateTo(g);
			}
		}

	};
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
	        	DataThread d = new DataThread();
	        	if (Manager.viewing==Manager.AGENCY) {
	        		pd = ProgressDialog.show(me, "Getting Agency List", "Contacting the Server:\nAgency List", true, false);
	        	} else if (Manager.viewing==Manager.STOPS){
	        		pd = ProgressDialog.show(me, "Getting Stops", "Contacting the Server:\nStops Near Selected Location", true, false);
	        	} else if (Manager.viewing==Manager.ROUTES) {
	        		pd = ProgressDialog.show(me, "Getting Routes", "Contacting the Server:\nRoutes Near Selected Location", true, false);
	        	}     	
	        	d.start();
			}
	};
		

	private Handler stact = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==1) {
				Toast t = Toast.makeText(me, "There was a problem with your request", Toast.LENGTH_LONG);
				if (Manager.viewing == Manager.STOPS) {
					t = Toast.makeText(me,  "Sorry, we could not find any stops near you.", Toast.LENGTH_LONG);
					
				} else if (Manager.viewing == Manager.ROUTES) {
					t = Toast.makeText(me, "Sorry, we could not find any routes near you.", Toast.LENGTH_LONG);
					
				}
				t.show();
				return;
			}
			
			if (Manager.viewing == Manager.AGENCY) {
				Intent i = new Intent(me, AgencyRouteList.class);
				startActivityForResult(i,0);
			} else if (Manager.viewing == Manager.STOPS) {
				Intent i = new Intent(me, StopList.class);
				startActivityForResult(i,0);
			} else if (Manager.viewing == Manager.ROUTES) {
				Intent i = new Intent(me, RouteList.class);
				startActivityForResult(i,0);
			}
		}
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode==-1) {
			showNFError();
		}
	}
	
	private void showNFError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Problem")
        .setIcon(R.drawable.ico)
        .setMessage("It seems that we cannot find stops near the location you have chosen; " +
        		"this is most likely because you are near an agency we do not support, " +
        		"or if could be a problem with the underlying data.\n" +
        		"Our data is constanly improving, so please check back later!")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        })
        .setNegativeButton("View all Routes for " + Manager.getShortName(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	Manager.viewing=Manager.AGENCY;
                	try {
                		handler.sendEmptyMessage(1);
            		} catch (Exception e) {
            			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
            					"Please try again later!", Toast.LENGTH_LONG);
            			t.show();
            		}
                }
        });
        
        b.show();
	}
	
	private Handler errorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==0) showNFError();
			if (msg.what==1) showServError();
			try {
				pd.dismiss();
			} catch (Exception ex) {}
		}
	};
	private void showServError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Problem")
        .setIcon(R.drawable.ico)
        .setMessage("It seems that you are having trouble contacting the server!\n" +
        		"Please try again soon.")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}

}
