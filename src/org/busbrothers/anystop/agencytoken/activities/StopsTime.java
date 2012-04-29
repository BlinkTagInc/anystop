package org.busbrothers.anystop.agencytoken.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.datacomponents.Agency;
import org.busbrothers.anystop.agencytoken.datacomponents.Favorites;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.map.ChooseMap;

//import com.admob.android.ads.AdManager;//removed for SDK update
import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.LinearLayout;

/** StopsTime is the entry point (launcher activity) into the AnyStop app. It sets up the 
 * data fetcher thread (among other things) and gets initial data from the server. Obvious it 
 * displays stuff to allow the user to begin using AnyStop.
 * 
 * @author Alex
 *
 */

public class StopsTime extends Activity{
    /** Called when the activity is first created. */
	
	Button PickAg, favStops, locate, favRoutes;
	private ProgressDialog pd, pp;
	private StopsTime me;
	private boolean flurryEventLogged;
	
	private static final String activityNameTag = "StopsTime";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	
	private static boolean first=true;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	//Figure out the size of the screen
    	//(taken from http://stackoverflow.com/questions/2193457/is-there-a-way-to-determine-android-physical-screen-height-in-cm-or-inches)
    	DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(dm.widthPixels/dm.xdpi,2);
        double y = Math.pow(dm.heightPixels/dm.ydpi,2);
        double screenInches = Math.sqrt(x+y);
        //Load either the "large display" view or not depending on display size in inches
        Log.v(activityNameTag, "Loading content view, screen size was " + screenInches + " inches.");
        if(screenInches > 4.5) setContentView(R.layout.root2_largedisplay);
        else setContentView(R.layout.root2);
        Log.v(activityNameTag, "Done loading content view.");
        
        Favorites.context = this;
    	me=this;
    	//StopsNM = (Button) findViewById(R.id.near_stops);
    	//RoutesNM = (Button) findViewById(R.id.near_routes);
    	PickAg = (Button) findViewById(R.id.pick_ag);
//    	NearAg = (Button) findViewById(R.id.near_ag);
//    	favAgencies = (Button) findViewById(R.id.fav_agencies);
    	favStops = (Button) findViewById(R.id.fav_stops);
    	favRoutes = (Button) findViewById(R.id.fav_routes);
    	locate = (Button) findViewById(R.id.loc);
    	
    	//Setup all Agency-related stuff in Manager
    	Manager.setAgencyName(getString(R.string.agencyName));
    	Manager.setAgencyTag(getString(R.string.agencyTag));
    	Manager.setAppName(getString(R.string.app_name));
    	Manager.setFlurryAPIK(getString(R.string.flurryAPIK));
    	Manager.setLongName(getString(R.string.agencyLong));
    	Manager.setShortName(getString(R.string.agencyShort));
    	Manager.setTableName(getString(R.string.agencyTable));
    	Manager.setPredictionType(getString(R.string.agencyPredictionType));
    	Manager.setSettingURL(getString(R.string.anyStopSettingsURL));
    	
    	Manager.setHybridAgency(getString(R.string.agencyIsHybrid));
    	if(Manager.isHybridAgency())
    		Manager.setScheduleTableName(getString(R.string.agencyScheduleTable));
    	
    	//Set up Admob APIK and keywords; also set up AdWhirl stuff but as of 2011-12-04 it's not being used yet
    	String adsapik = getString(R.string.BBadmobAPIK);
    	String adwhirlAPIK = getString(R.string.BBadwhirlAPIK);
    	HashMap<String, Boolean> overrides = Manager.getOverrides();
    	if (!Manager.get_predictionType().equalsIgnoreCase("schedule")) {
    		adsapik = getString(R.string.NBISadmobAPIK);
    		adwhirlAPIK = getString(R.string.NBISadwhirlAPIK);
    	}
    	Boolean b = overrides.get(Manager.getAgencyTag());
    	if (b!=null) {
    		if (!b.booleanValue()) {
    			adsapik = getString(R.string.NBISadmobAPIK);
    			adwhirlAPIK = getString(R.string.NBISadwhirlAPIK);
    		} else {
    			adsapik = getString(R.string.BBadmobAPIK);
    			adwhirlAPIK = getString(R.string.BBadwhirlAPIK);
    		}
    	}
    	Manager.setAdsAPIK(adsapik);
    	Manager.setAdWhirlAPIK(adwhirlAPIK);
    	//AdManager.setPublisherId(Manager.getAdsAPIK()); //removed for Admob SDK upgrade; this gets done on individual AdView init now
    	
    	//Remove me
    	Log.v(activityNameTag, "This is a verbose log message.");
    	Log.d(activityNameTag, "This is a debug log message.");
    	Log.i(activityNameTag, "This is an info log message.");
    	Log.w(activityNameTag, "This is a warning log message.");
    	Log.e(activityNameTag, "This is an error log message.");
    	//Remove or comment the below also
    	//Log.i(activityNameTag, "Using flurry APIK: " + Manager.getFlurryAPIK());
    	//Log.i(activityNameTag, "Using ads APIK: " + Manager.getAdsAPIK());
    	//String maps_apik = getString(R.string.maps_apik);
    	//Log.i(activityNameTag, "Using google maps APIK: " + maps_apik);
    	
    	//Try to load settings from server
    	SettingsThread setThread = new SettingsThread();
    	setThread.run();
    	
    	//Setup UI-related stuff in manager
    	String faceName = (String) getString(R.string.defaultTypeface);
    	if(!faceName.equals("normal")) {
    		Typeface face=Typeface.createFromAsset(getAssets(),faceName);
    		Manager.setDefaultTypeface(face); //if R.string.defaultTypeface is "normal" don't set the default typeface in Manager
    	}
    	
    	//The below code applies Typeface face to the root view of this layout, and all of its children
    	Manager.applyFonts((View) findViewById(R.id.rootlayout));
    	
    	PickAg.setText("All routes for " + Manager.getShortName());
    	
//    	NearAg.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//            	Manager.viewing=Manager.AGENCY;
//            	LaxPositionThread t = new LaxPositionThread();
//            	pp = ProgressDialog.show(me, "Local Agencies", "Determining Your Approximate Location", true, false);
//            	t.start();
//            }
//        });
    	
    	PickAg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
        
//    	favAgencies.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//            	Manager.agencies=new ArrayList<Agency>((HashSet<Agency>)Favorites.getInstance().getFavAgencies());
//            	Intent i = new Intent(v.getContext(), AgencyList.class);
//            	startActivityForResult(i,0);
//            }
//        });
        
    	favStops.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Manager.favstops=new ArrayList<SimpleStop>((HashSet<SimpleStop>)Favorites.getInstance().getFavStops());
            	Intent i = new Intent(v.getContext(), FavStops.class);
            	startActivityForResult(i,0);
            }
        });
    	
    	favRoutes.setOnClickListener(new View.OnClickListener() {
    		
    		public void onClick(View v) {
            	Manager.viewing=Manager.FAVROUTES;
            	try {
            		handler.sendEmptyMessage(1);
        		} catch (Exception e) {
        			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
        					"Please try again later!", Toast.LENGTH_LONG);
        			t.show();
        		}
            }
        });
    	
    	locate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent i = new Intent(v.getContext(), ChooseMap.class);
            	startActivityForResult(i,0);
            }
        });
    	
    	LinearLayout t;
    	t =(LinearLayout) findViewById(R.id.rootlayout);
    	RelativeLayout logoBackgroundRL = (RelativeLayout) findViewById(R.id.titlelayout);
    	ImageView logoBackgroundIV = (ImageView) findViewById(R.id.title_background);
    	WindowManager wm = getWindowManager(); 
        Display d = wm.getDefaultDisplay();
        
        //We're supposed to use Display.getSize() since getWidth(getHeight() is deprecated but it just crashes on older Android versions...
        //Point dSize = new Point();
        //d.getSize(dSize);
        //int width = dSize.x; int height = dSize.y;
        
        //if (width > height)
        if (d.getWidth() > d.getHeight())
        {
        	t.setOrientation(LinearLayout.HORIZONTAL);
        	//logoBackgroundLL.setBackgroundResource(R.drawable.subwaymap);
        	logoBackgroundIV.setImageResource(R.drawable.subwaymap);
        	//logoBackgroundRL.
        }
        else
        {
        	t.setOrientation(LinearLayout.VERTICAL);
        	//logoBackgroundLL.setBackgroundResource(R.drawable.subwaymaplong);
        	logoBackgroundIV.setImageResource(R.drawable.subwaymaplong);
        }
        
        flurryEventLogged = false; //this will be set to true once we log the activity opening event in onStart()        
    }

	class DataThread extends Thread {
		public DataThread() {

		}

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
				Log.d(activityNameTag, "DataThread returned from Manager, calling stack Thread.");
				stact.sendEmptyMessage(0);
			}
		}
	}
	
	class SettingsThread extends Thread {
		public SettingsThread() {

		}

		public void run() {
			Manager.loadSettings();
		}
	}
	
	/** 
	 * This Handler is responsible for receiving messages from OnClickListener events from this Activity. It will create a new DataThread 
	 * and run it, to update the list of all Routes for this app's Agency. 
	 */
	private Handler handler = new Handler() {

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
				try {
					pp.dismiss();
				} catch (Exception ex) {}
	        	DataThread d = new DataThread();
	        	if (Manager.viewing==Manager.AGENCY || Manager.viewing==Manager.FAVROUTES) {
	        		pd = ProgressDialog.show(me, "Getting Agency Data", "Contacting the Server:\nRoute List", true, false);
	        	}
	        	try {
	        		d.start();
	    		} catch (Exception e) {
	    			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
	    					"Please try again later!", Toast.LENGTH_LONG);
	    			t.show();
	    		}
		}
	};
	

	private Handler stact = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==1) {
				Toast t = Toast.makeText(me, "There was a problem with your request", Toast.LENGTH_LONG);
				if (Manager.viewing == Manager.AGENCY) {
					t = Toast.makeText(me, "Sorry, we could not find any agencies near you.", Toast.LENGTH_LONG);
					
				}
				t.show();
				return;
			}
			
			if (Manager.viewing == Manager.AGENCY) {
				Intent i = new Intent(me, AgencyRouteList.class);
				startActivityForResult(i,0);
			}
			if (Manager.viewing == Manager.FAVROUTES) {
				Intent i = new Intent(me, FavRoutes.class);
				startActivityForResult(i,0);
			}
			
		}
		
	};
	
	
	
	protected void onStart() {
		super.onStart();
		
		//Log.v(activityNameTag, "IsUseUsage is set to " + Manager.isUseUsage(this) + " right now.");
		
		if (Manager.isUseUsage(this) && first) {
			//Below two lines commented out per to-do 55622789
//			Toast t = Toast.makeText(this, "Welcome to " + getString(R.string.app_name) + "!", Toast.LENGTH_LONG);
//			t.show();
			
//			t = Toast.makeText(this, "You can look at routes and stops near a selected location.", Toast.LENGTH_SHORT);
//			t.show();
//			
//			t = Toast.makeText(this, "You can view all available agencies using the \"All Agencies\" button.", Toast.LENGTH_LONG);
//			t.show();
//			
//			t = Toast.makeText(this, "You can view nearby agencies using the \"Nearby Agencies\" button.", Toast.LENGTH_LONG);
//			t.show();
//			
//			t = Toast.makeText(this, "Or, you can access your favorites!", Toast.LENGTH_SHORT);
//			t.show();
			
			
			first=false;
		}
        
        //Report memory usage
        Manager.printMemoryUsageInfo();
        
        Log.v(activityNameTag, "Starting Flurry session.");
		FlurryAgent.onStartSession(this, Manager.getFlurryAPIK());
		
		//Report opening of StopsTime to Flurry Analytics
		if(! flurryEventLogged) {
			Manager.flurryActivityOpenEvent(activityNameTag);
			flurryEventLogged = true;
		}
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
	
	private Handler errorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==0) showError();
			if (msg.what==1) showServError();
			try {
				pd.dismiss();
			} catch (Exception ex) {}
		}
	};
	private void showServError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Problem")
        .setIcon(R.drawable.ico )
        .setMessage("It seems that you are having trouble contacting the server!\n" +
        		"Please try again soon.")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}
	private void showError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Problem")
        .setIcon(R.drawable.ico )
        .setMessage("It seems that there are no")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    
	    LinearLayout t;
    	t =(LinearLayout) findViewById(R.id.rootlayout);
    	ImageView logoBackgroundIV = (ImageView) findViewById(R.id.title_background);
	    
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
        	t.setOrientation(LinearLayout.HORIZONTAL);
        	//t.setBackgroundResource(R.drawable.subwaymap);
        	logoBackgroundIV.setImageResource(R.drawable.subwaymap);
        	
        }
        else
        {
        	t.setOrientation(LinearLayout.VERTICAL);
        	//t.setBackgroundResource(R.drawable.subwaymaplong);
        	logoBackgroundIV.setImageResource(R.drawable.subwaymaplong);
        	
        }
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		FlurryAgent.onEndSession(this);		
		Log.v(activityNameTag, "Ending Flurry session.");
					
		//Make sure to get rid of any progress dialogs
		//Log.v(activityNameTag, "Dismissing pd in onStop()");
		//pd.dismiss();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		
	}

}
