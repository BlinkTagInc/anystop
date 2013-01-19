package org.busbrothers.anystop.agencytoken.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.Utils;
import org.busbrothers.anystop.agencytoken.WMATATransitDataManager;
import org.busbrothers.anystop.agencytoken.activities.AgencyRouteDrill.IconicAdapter;
import org.busbrothers.anystop.agencytoken.datacomponents.Favorites;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.Prediction;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.map.StopMap;
import org.busbrothers.anystop.agencytoken.uicomponents.CustomList;
import org.busbrothers.anystop.agencytoken.uicomponents.IndexCheck;
import org.busbrothers.anystop.agencytoken.uicomponents.SelfResizingTextView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdView;
import com.sensedk.AswAdLayout;

/** Note that RouteDrill may be called by both AgencyRouteDrill and RouteList */
public class RouteDrill extends CustomList {

	RouteDrill me;
	
	/*public static RouteDrill single() {
		return me;
	}*/
	
	//TextView selection;
	List<SimpleStop> tempArr; //!<The List of SimpleStop objects (representing transit stops) that will be displayed in this RouteDrill.
	ArrayList<SimpleStop> arr;
	
	int nearest_stop_index; //!<The SimpleStop with this index in arr will be the nearest stop to the user's current location
	Button mapButton, refresh;
	String callingActivity;
	SelfResizingTextView title; 
	TextView subtitle;
	ProgressDialog pd;
	IconicAdapter i;
	
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	
	private static final String activityNameTag = "RouteDrill";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	private boolean doAutoRefresh;
	private int refresh_period_in_seconds;
	
	boolean isASearchedActivity;
	
	private boolean calledByAgencyRouteDrill = false;
	
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/	
	@Override
	public void onCreate(Bundle icicle) {
		me=this;
		
		_usageMessages= new String[] {"Here is a list of stops for the chosen route, along with their predictions.",
				"Select a stop from this list to view it on the map.",
				"You can save a stop to favorites by clicking the heart on the right hand side",
				"The 'Refresh' button below will refresh the prediction information"
		};
		
		//The below block of code sets up the AdView for this Activity, and adds it to the Layout
		setContentView(R.layout.routedrill);
		
		
		//Determine who we were called by
		Bundle extras = getIntent().getExtras();
		if(extras != null) if(extras.getString("CallingActivity")!=null) callingActivity = extras.getString("CallingActivity");
		else callingActivity = activityNameTag;
		
		Log.v(activityNameTag, activityNameTag+" was opened, not in search, by " + callingActivity);
		if(callingActivity.equals("AgencyRouteDrill"))calledByAgencyRouteDrill = true;
		
		isASearchedActivity = false;
		
		if(Manager.isWMATA()) {
			ArrayList<SimpleStop> fetchedArr = (ArrayList<SimpleStop>) WMATATransitDataManager.peekLastData();
			tempArr = new ArrayList<SimpleStop>();
			
			ArrayList<String> intersectionsAlreadySeen = new ArrayList<String>();
			
			//Filter stops with identical intersections
			for(SimpleStop s : fetchedArr) {
				//if(!intersectionsAlreadySeen.contains(s.intersection)) {
				if(true) {
					tempArr.add(s);
					intersectionsAlreadySeen.add(s.intersection);
				}
			}
		}
		
		else tempArr = Manager.routeMap.get(Manager.stringTracker);
		
		//Make sure that we were able to retrieve the things from tempArr 
		if (tempArr==null) {
			this.setResult(-1);
			this.finish();
		} else {
			//Transfer all of the things in tempArr into arr, which is an ArrayList so we can remove things from it later :)
			//Make sure that we avoid adding in duplicate directions! Duplicate directions are designed when SimpleStop.headSign is the same 
			arr = new ArrayList<SimpleStop>();
			ArrayList <String> directionsInArr = new ArrayList<String>();
			Iterator <SimpleStop> tempArrIterator = tempArr.iterator();
			while(tempArrIterator.hasNext()){
				SimpleStop currStop = tempArrIterator.next();
				
				if(!calledByAgencyRouteDrill) arr.add(currStop); //Don't filter by headsign if we weren't called by AgencyRouteDrill
				else {
					//Case - we have already seen this head sign (direction)
					if(directionsInArr.contains(currStop.headSign));
					//Case - we have NOT seen this direction yet
					else {
						directionsInArr.add(currStop.headSign);
						arr.add(currStop);
					}
				}
			}
			
			Collections.sort(arr);
			
			//Find the nearest stop, put that one on top if it is found
			SimpleStop closestStop = Manager.getNearestStop(arr, this);
			nearest_stop_index = -1; //set nearest stop index to -1, unless we later find that there is a nearest stop
			if(closestStop != null) {
				arr.remove(closestStop);
				arr.add(0, closestStop);
				nearest_stop_index = 0;
			}
			
			i = new IconicAdapter(this);
			setListAdapter(i);
			this.setResult(0);
		}
		
		refresh = (Button) findViewById(R.id.refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		handler.sendEmptyMessage(0);
        		} catch (Exception e) {
        			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
        					"Please try again later!", Toast.LENGTH_LONG);
        			t.show();
        		}
            	
            }
        });
    	
    	//Set up the search button onClick handler
		searchButton = (Button) this.findViewById(R.id.searchbutton);
		if(searchButton!=null) {
			searchButton.setOnClickListener(new View.OnClickListener() {
				//@Override
				public void onClick(View view) {
					//If searchButton gets clicked run onSearchRequested() to display the search dialog
					Log.v(activityNameTag, "Search button was clicked, launching search dialog! IVANY");
					onSearchRequested();
				}
			});
			searchButton.setVisibility(View.GONE); //search button not very relevant, this view will only have a few items in it
		} else {
			//This should never happen? Right?
			Log.v(activityNameTag, "Somehow searchButton was null...IVANY?!");
		}
    	
		//If this class gets called by AgencyRouteDrill, then it was opened to view just a single stop.
		//Set the title appropriately
		//Otherwise set the title to reflect the route being displayed in this Activity
		this.title = (SelfResizingTextView) findViewById(R.id.title);
		if(arr.size() > 0 && callingActivity.equals("AgencyRouteDrill")) title.setText("Predictions for " + arr.get(0).intersection);
		else {
			if(Manager.isWMATA()) title.setText("Predictions for Route " + Manager.stringTracker.replace("r_", ""));
			else title.setText("Predictions for Route " + Manager.stringTracker);
		}
		
		title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
		
		//Get the auto-refresh behavior started
		String refreshPeriodInSecondsString = (String) getString(R.string.PredictionAutoRefreshPeriod);
		refresh_period_in_seconds = Integer.parseInt(refreshPeriodInSecondsString) > 0 ? Integer.parseInt(refreshPeriodInSecondsString) : 60;
		autoRefreshRunnableTaskHandler.removeCallbacks(autoRefreshRunnableTask);
	    autoRefreshRunnableTaskHandler.postDelayed(autoRefreshRunnableTask, refresh_period_in_seconds * 1000);
	    
    	super.onCreate(icicle);
	}
	
	/** This method gets called if this activity is at the top of the stack and its launch mode is set to singleTop in the App manifest.
	 *  
	 */
	@Override
	protected void onNewIntent(Intent mIntent) { 	
		IconicAdapter theListAdapter = i; //aliasing
		isASearchedActivity = true;
		
		if(Intent.ACTION_SEARCH.equals(mIntent.getAction())) {
			Log.v(activityNameTag, activityNameTag+" was opened AS A SEARCHED ACTIVITY.");
			String searchQuery = mIntent.getStringExtra(SearchManager.QUERY);//Search query for this (searched) Activity instance
			searchQueryStack.push(searchQuery); //push searchQuery to our query stack
			
			//Echo all search queries to Log; will probably get removed later on in development cycle
			Iterator<String> searchQueryStackIterator = searchQueryStack.iterator();
			while(searchQueryStackIterator.hasNext()) {
				String currQuery = searchQueryStackIterator.next();
				Log.v(activityNameTag, "Got search query: " + currQuery);
			}
			
			//Make a list of the routes that we should remove from the IconicAdapter's data structure
			ArrayList<SimpleStop> positionsToFilter = new ArrayList<SimpleStop>();
			for(int currentPosition = 0; currentPosition < theListAdapter.getCount(); currentPosition++) {
				SimpleStop currStop = theListAdapter.getItem(currentPosition);
				boolean passFilter = true; //passFilter will remain set to true if currRoute matches all search queries in searchQueryStack.
				
				//For each Route, we go through the entire search query stack, comparing all queries against currRoute.sName
				searchQueryStackIterator = searchQueryStack.iterator();
				while(searchQueryStackIterator.hasNext()) {
					String currQuery = searchQueryStackIterator.next();
					passFilter = passFilter && currStop.containsForSearch(currQuery);
				}
				
				if(!passFilter) positionsToFilter.add(currStop);
			}
			
			//Remove all positions that we should filter
			for(int i = 0; i < positionsToFilter.size(); i++) {
				theListAdapter.remove(positionsToFilter.get(i));
			}			
			theListAdapter.notifyDataSetChanged();
			
			SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			title.setText("Stops matching \"" + searchQuery + "\"");
		}
	}

	@Override
	/** This method calls the super-class onStart() method, and also reports that this Activity was opened to Flurry Analytics.
	 * 
	 */
	protected void onStart() {
		super.onStart();
		
		//Report opening of StopsTime to Flurry Analytics
		if(! flurryEventLogged) {
			Manager.flurryActivityOpenEvent(activityNameTag, searchQueryStack.empty());
			flurryEventLogged = true;
		}
	}
	
	/** We override onResume, only to set doAutoRefresh appropriately */
	protected void onResume() {
		super.onResume();
		doAutoRefresh=true;
		
		LinearLayout adLayoutParent = (LinearLayout) findViewById(R.id.dlist_ad_holder); //this LinearLayout will contain the AdView
		
		//The below code sets up Advertisement-related stuff for this activity
		if(Manager.adTypeUsing() == Manager.ADMOB_AD) {			
			//The below block of code sets up the AdView for this Activity, and adds it to the Layout
			AdView googleAdMobAd = Manager.setupAdView(this); //have Manager set up the AdView for us :)
			adLayoutParent.addView(googleAdMobAd);
			
			//Hide (& disable?) the Addience adview
			//AswAdLayout adView = (AswAdLayout)findViewById(R.id.addience_adview);
			//adView.setVisibility(View.GONE);
			//adView.optOut();
		}
		else if(Manager.adTypeUsing() == Manager.ADDIENCE_AD) {
			//Don't have to disable the Admob adview, since we never create it in the first place
			
			//We would use this if we had instantiated an addience ad layout in the XML
			//AswAdLayout adView = (AswAdLayout)findViewById(R.id.addience_adview);
			
			AswAdLayout adView = Manager.makeAddienceAd(this);
			adLayoutParent.addView(adView);
		}
		
		adLayoutParent.invalidate();
	}

	
	/** We override onPause, only to set doAutoRefresh appropriately */
	protected void onPause() {
		super.onPause();
		doAutoRefresh=false;
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
	 	Manager.lat = arr.get(position).lat;
	 	Manager.lon = arr.get(position).lon;
	 	
	 	//No flurry prediction event if we are called by ARD; that would be double-counting this stop since it is selected in ARD also 
	 	if(!callingActivity.equals("AgencyRouteDrill"))
	 		Manager.flurryPredictionEvent(this, arr.get(position));

	 	v.postDelayed(new Runnable() {
            public void run() {
            	startActivity(new Intent(me, StopMap.class));
            }
        }, super.getAnimationTime());
	}
	
	/** This method overrides onSearchRequested(); it will pause this activity and transfer control to the search dialog (unwillingly). 
	 * It does nothing else interesting.
	 * 
	 * @author ivany
	 */
	@Override
	public boolean onSearchRequested() {
		return super.onSearchRequested(); //returning false means we don't actually do a search...but that's not what we are doing.
	}

	class IconicAdapter extends ArrayAdapter<SimpleStop> {
		RouteDrill context;

		IconicAdapter(RouteDrill context) {
			super(context, R.layout.check_sched, arr);
			this.context=context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater=LayoutInflater.from(context);
			
			View row=inflater.inflate(R.layout.check_sched, null);
			TextView label=(TextView)row.findViewById(R.id.dir_label);
			TextView content=(TextView)row.findViewById(R.id.dir_subinfo);
			TextView sched = (TextView)row.findViewById(R.id.dir_sched);
			IndexCheck check=(IndexCheck)row.findViewById(R.id.favbox);
			Manager.applyFonts(row);

			check.index = position;
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					//mash agency name and intersection
					if (((IndexCheck)buttonView).stopUpdate==true) {return;}
					SimpleStop ag = arr.get(((IndexCheck)buttonView).index);
					if (isChecked) {
						Favorites.getInstance().addStop(ag);
						Toast t = Toast.makeText(me, "Added to Favorites", Toast.LENGTH_SHORT);
	        			t.show();
					} else {
						Favorites.getInstance().removeStop(ag);
						Toast t = Toast.makeText(me, "Removed from Favorites", Toast.LENGTH_SHORT);
	        			t.show();
					}
				}
			});
			Favorites favs = Favorites.getInstance();
			check.stopUpdate=true;
			check.setChecked(favs.checkStop(arr.get(position).agency+arr.get(position).intersection));
			check.stopUpdate=false;
			
			if (position % 2 == 0) {
				row.setBackgroundResource(R.drawable.cust_list_selector);
			}
			
			StringBuilder b = new StringBuilder();
			//Only append direction if NOT called by AgencyRouteDrill, since AgencyRouteDrill will later make the title (label) of
			//this entry the Direction thus making the direction label redundant
			if(!callingActivity.equals("AgencyRouteDrill")) {
				if(arr.get(position).headSign != null && arr.get(position).headSign.length()<1)
					b.append(Utils.fmtHeadsign(Utils.checkHeadsign(arr.get(position).headSign)) + "\n"); 
			}
			//Append to subtext the routes served by this stop
			
			ArrayList<SimpleStop> stops;
			
			//TODO: classcastexception right here???
			if(Manager.isWMATA()) stops = (ArrayList<SimpleStop>) WMATATransitDataManager.peekLastData();
			else stops = Manager.stopMap.get(arr.get(position).intersection);
			
			HashSet<String> routeNamesAlreadyListed = new HashSet<String>();
			boolean first_entry = true;
			if(stops.size() > 0) {
				if(Manager.isWMATA()) b.append("Route: ");
				else b.append("Routes Served: ");
			}
			else Log.d(activityNameTag, "Couldn't get routes served for SimpleStop with intersection \"" + arr.get(position).intersection + "\"");
			if(Manager.isWMATA()) b.append(arr.get(position).routeName);
			else {
				for (SimpleStop s : stops) {
					//The below code makes sure to avoid duplicate listings in "Routes Served: <routes>"
					if(!routeNamesAlreadyListed.contains(Utils.routeStripTrailing(s.routeName))) {
						//Make sure to only pre-pend a comma to a routeName if it is NOT the first route name in the list
						if(first_entry) first_entry = false;
						else b.append(", ");
						b.append(Utils.routeStripTrailing(s.routeName));
						routeNamesAlreadyListed.add(Utils.routeStripTrailing(s.routeName));
					}
				}
			}	

			//Append to subtext the predictions for this stop
			if(arr.get(position).pred == null) arr.get(position).pred = new Prediction(new int[0], true);
			
			ArrayList<String> preds = arr.get(position).pred.format();
			if (preds.size()==0) {
				b.append("\nNo predictions at this time");
			} else {
				TableLayout predictionTable = (TableLayout) inflater.inflate(R.layout.predictiontable, null);
				
				boolean first_stop = true;
				
				for (String s : preds) {
					String [] predictionTime = s.split("-");
					if(predictionTime.length == 2) {
						TableRow tr = (TableRow) inflater.inflate(R.layout.predictiontablerow, null);
						
						TextView deltaTime = (TextView) tr.findViewById(R.id.deltatime);
						TextView absTime = (TextView) tr.findViewById(R.id.abstime);
						
						deltaTime.setText(predictionTime[0]);
						
						//Hax for TheBUS since TheBUS sometimes has real-time predictions for the next arrival
						if(arr.get(position).pred.isRT && Manager.getAgencyTag().equals("thebus") && first_stop) {
							absTime.setText(predictionTime[1] + "( real-time)");
							absTime.setTextColor(0xFF009900);
							deltaTime.setTextColor(0xFF009900);
							
						}
						else 
							absTime.setText(predictionTime[1]);
						
						Manager.applyFonts(deltaTime);
						Manager.applyFonts(absTime);
						
						first_stop=false;						
						predictionTable.addView(tr);
					}
					//b.append(s);
					//if (preds.indexOf(s)!=preds.size()-1) {b.append("\n");};
				}
				
				LinearLayout textSectionLL = (LinearLayout) row.findViewById(R.id.row_textsection);
				textSectionLL.addView(predictionTable);
				
			}
			
			//If we are processing the first item, use its prediction status to set the title of the screen, telling the
			//user whether predictions are real-time or not
			//We assume that all stops on a given route are either all Real-Time, or not.
			if(position==0) {
				context.isRealTimeSchedule(!Manager.isScheduleApp());
			}
			
			/*if (arr.get(position).pred.isRT) {
				sched.setText("Real-Time Info Available");
				sched.setTextColor(0xFF009900);
			} else {
				sched.setText("Schedule Available");
				sched.setTextColor(0xFFE0B000);
			} */
			
			if(position == nearest_stop_index && !(callingActivity.equals("AgencyRouteDrill"))) {
				sched.setText("Nearest Stop");
				sched.setTextColor(0xFF009900);
			}
			else sched.setVisibility(View.GONE);
			
			//If we are calling this RouteDrill activity from AgencyRouteDrill then display direction as stop "name"
			if(callingActivity.equals("AgencyRouteDrill")) label.setText("Direction: " + Utils.fmtHeadsign(Utils.checkHeadsign(arr.get(position).headSign)));
			else label.setText(arr.get(position).intersection);
			
			content.setText(b.toString());
			ImageView icon=(ImageView)row.findViewById(R.id.icon);
			icon.setImageResource(R.drawable.stop);
			return(row);
			
			
			
		}
	}
	
	class DataThread extends Thread {
		public DataThread() {

		}

		public void run() {
			Manager.clearStops();
			// String lat = loc.getLatitude()+"";
			// String lon = loc.getLongitude()+"";
			try {
				if(Manager.isWMATA()) WMATATransitDataManager.repeat();
				else Manager.repeat();
			} catch (NoneFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServerBarfException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pd.dismiss();
			stact.sendEmptyMessage(0);

		}
	};

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			DataThread d = new DataThread();
				pd = ProgressDialog.show(me, "Getting Stops",
						"Contacting the Server:\nRefreshing Predictions",
						true, false);

				d.start();

		};
	};
	
	 private Handler stact = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
				if(Manager.isWMATA()) arr = (ArrayList<SimpleStop>) WMATATransitDataManager.peekLastData();
				else arr = Manager.routeMap.get(Manager.stringTracker);
				
				if (arr==null) {
					me.setResult(-1);
					me.finish();
				}else {
					Collections.sort(arr);
					i.notifyDataSetChanged();
					me.setResult(0);
				}
			 
		 }
			
	 };
	 
	private Handler waitmessage = new Handler() {
		@Override
		
		public void handleMessage(Message msg) {
			Toast t;
			while (!Manager.messageQueue.empty()) {
				t= Toast.makeText(me, Manager.messageQueue.pop(), Toast.LENGTH_LONG);
				
				t.show();
			}
		}
	};
		
	/** Overrides the default onDestroy. Special things we do in AgencyRouteList.onDestroy():
	 * 1. Nothing
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(Manager.isWMATA()) WMATATransitDataManager.popCommand();
	}
	
	/**This method will attempt to refresh the prediction displayed on this RouteDrill screen. */
	private void doRefresh() {
		try {
    		handler.sendEmptyMessage(0);
		} catch (Exception e) {
			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
					"Please try again later!", Toast.LENGTH_LONG);
			t.show();
		}
	}
	
	/** This method gets called by the iconic adapater to set the "Prediction or Schedule" status
	 * of this RouteDrill list.
	 * @param isRealTime Set whether we display that Real-Time info is available or not.
	 */
	public void isRealTimeSchedule(boolean isRealTime) {
		TextView dirSched = (TextView) findViewById(R.id.dir_sched);
		
		if(dirSched == null) {
			Log.e(activityNameTag, "Somehow, isRealTimeSchedule() could not get dir_sched TextView from layout.");
			return;
		}
		
		if(isRealTime) {
			dirSched.setText("Real-Time Info Available");
			dirSched.setTextColor(0xFF009900);
		}
		else {
			dirSched.setText("Schedule Available");
			dirSched.setTextColor(0xFFE0B000);
		}
	}
	
	/**This Handler exists soley to implement doRefresh()-based auto refresh behavior */
	private Handler autoRefreshRunnableTaskHandler = new Handler();
	
	/** This Runnable handles the automatic updating of the data displayed by this RouteDrill. It will update 
	 * the data by calling doRefresh() conditional on doAutoRefresh being true. 
	 */
	private Runnable autoRefreshRunnableTask = new Runnable() {
	   public void run() {
	      if(doAutoRefresh) {
	    	  doRefresh();
	    	  i.notifyDataSetChanged();
	    	  
	    	  Log.v(activityNameTag, activityNameTag + " called auto-refresh method from autoRefreshRunnableTask, refreshed data");
	      }
	      else Log.v(activityNameTag, activityNameTag + " called auto-refresh method from autoRefreshRunnableTask, but did nothing");
	     
	      autoRefreshRunnableTaskHandler.removeCallbacks(autoRefreshRunnableTask);
	      autoRefreshRunnableTaskHandler.postDelayed(autoRefreshRunnableTask, refresh_period_in_seconds * 1000);
	   }
	};
}
