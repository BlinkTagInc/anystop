package org.busbrothers.anystop.agencytoken.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.Utils;
import org.busbrothers.anystop.agencytoken.WMATATransitDataManager;
import org.busbrothers.anystop.agencytoken.activities.FavRoutes.IconicAdapter;
import org.busbrothers.anystop.agencytoken.activities.RouteList.DataThread;
import org.busbrothers.anystop.agencytoken.datacomponents.Favorites;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.uicomponents.CustomList;
import org.busbrothers.anystop.agencytoken.uicomponents.IndexCheck;
import org.busbrothers.anystop.agencytoken.uicomponents.SelfResizingTextView;

import com.google.ads.AdView;
import com.sensedk.AswAdLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class StopList extends CustomList {

	StopList me;
	/*public static StopList single() {
		return me;
	}*/
	
	//TextView selection;
	ArrayList<String> arr;
	List<SimpleStop> stopArr;
	HashMap<String, SimpleStop> stopMap;
	IconicAdapter theListAdapter;
	
	int nearest_stop_index;
	Button mapButton;
	int lastPosition;
	
	ProgressDialog pd;
	
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/	
	
	private static final String activityNameTag = "StopList";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	
	@Override
	public void onCreate(Bundle icicle) {
		me=this;
		String searchQuery = null;
			
		_usageMessages= new String[] {"Here is a list of stops. You can see all the routes they serve below them.",
				"Select a stop from this list to view predictions.",
				"You can save a stop to favorites by clicking the heart on the right hand side",
				"You can turn off screen instructions in the Preferences."
		};
		
		//The below block of code sets up the AdView for this Activity, and adds it to the Layout
		setContentView(R.layout.stoplist);
		
		Log.v(activityNameTag, activityNameTag+" was opened, but not in a search.");
		
		List<String> tempArr;

		if(Manager.isWMATA()) {
			stopArr = (ArrayList<SimpleStop>) WMATATransitDataManager.peekLastData(); 
			tempArr = new ArrayList<String>();
			stopMap = new HashMap<String, SimpleStop>();
			
			for(SimpleStop s : stopArr) {
				tempArr.add(s.intersection);
				stopMap.put(s.intersection, s);
			}
			
		} else {
			String[] actions = new String[Manager.stopMap.keySet().size()];
			tempArr = Arrays.asList(Manager.stopMap.keySet().toArray(actions));
		}
		
		if (tempArr==null) {
			this.setResult(-1);
			this.finish();
		} else {
			//Added to turn arr into an ArrayList that we can manipulate
			arr = new ArrayList<String>();
			for(String str : tempArr) arr.add(str);	
			
			if(!Manager.isWMATA()) {
				Collections.sort(arr);
				
				//Find the nearest stop to the users's current location
				stopArr = new ArrayList<SimpleStop>();
				for(String stopName : arr) stopArr.add(Manager.stopMap.get(stopName).get(0));
				SimpleStop nearestStop = Manager.getNearestStop(stopArr, this);
	
				//If we find a nearest stop, put it at the top and remember it's position in the list so we can mark it as the nearest stop
				if(nearestStop != null) {
					int nearest_stop_index_old = stopArr.indexOf(nearestStop);
					String nearestStopName = arr.remove(nearest_stop_index_old);
					arr.add(0, nearestStopName);
					nearest_stop_index = 0;
				}
				else nearest_stop_index = -1;
			}
			
			SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
			title.setText("Stops Near Me");
			theListAdapter = new IconicAdapter(this);
			setListAdapter(theListAdapter);
		}
		
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
		} else {
			//This should never happen? Right?
			Log.v(activityNameTag, "Somehow searchButton was null...IVANY?!");
		}
		
		super.onCreate(icicle);
	}
	
	/** This method gets called if this activity is at the top of the stack and its launch mode is set to singleTop in the App manifest.
	 *  
	 */
	@Override
	protected void onNewIntent(Intent mIntent) { 
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
			ArrayList<String> positionsToFilter = new ArrayList<String>();
			for(int currentPosition = 0; currentPosition < theListAdapter.getCount(); currentPosition++) {
				String currStopName = theListAdapter.getItem(currentPosition);
				SimpleStop currStop;
				
				if(Manager.isWMATA()) currStop = stopMap.get(currStopName);
				else currStop = Manager.stopMap.get(currStopName).get(0);
				boolean passFilter = true; //passFilter will remain set to true if currRoute matches all search queries in searchQueryStack.
				
				//For each Route, we go through the entire search query stack, comparing all queries against currRoute.sName
				searchQueryStackIterator = searchQueryStack.iterator();
				while(searchQueryStackIterator.hasNext()) {
					String currQuery = searchQueryStackIterator.next();
					passFilter = passFilter && currStop.containsForSearch(currQuery.toLowerCase());
				}
				
				if(!passFilter) positionsToFilter.add(currStopName);
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
	
	protected void onResume() {
		super.onResume();
		
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
	
	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
		lastPosition = position;
		Manager.stringTracker=arr.get(position);
		
		//TODO: fixme (and call prediction for stop in DataThread)
		SimpleStop predictedStop;
		
		if(Manager.isWMATA()) predictedStop = stopMap.get(arr.get(position));
		else predictedStop = Manager.stopMap.get(arr.get(position)).get(0);
		
		if(predictedStop != null) Manager.flurryPredictionEvent(this, predictedStop);
		else Log.d(activityNameTag, "Error in StopList - onListItemClick() could not set predictedStop correctly.");
		
		if(Manager.isWMATA()) {
			handler.sendEmptyMessage(0);
		} else {
			v.postDelayed(new Runnable() {
	            public void run() {
	            	startActivityForResult(new Intent(me, StopDrill.class),0);
	            }
	        }, super.getAnimationTime());
		}
    	
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

	class IconicAdapter extends ArrayAdapter<String> {
		StopList context;

		IconicAdapter(StopList context) {
			super(context, R.layout.check_sched, arr);

			this.context=context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater=LayoutInflater.from(context);
			
			View row=inflater.inflate(R.layout.check_sched, null);
			TextView label=(TextView)row.findViewById(R.id.dir_label);
			TextView content=(TextView)row.findViewById(R.id.dir_subinfo);
			IndexCheck check=(IndexCheck)row.findViewById(R.id.favbox);
			TextView sched = (TextView)row.findViewById(R.id.dir_sched);
			Manager.applyFonts(row);

			check.index = position;
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					//mash agency name and intersection
					if (((IndexCheck)buttonView).stopUpdate==true) {return;}
					SimpleStop ag;
					
					if(Manager.isWMATA()) ag = stopMap.get(arr.get(((IndexCheck)buttonView).index)); 
					else ag = Manager.stopMap.get(arr.get(((IndexCheck)buttonView).index)).get(0);
					
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
			
			if(Manager.isWMATA()) check.setChecked(favs.checkStop(stopMap.get(arr.get(position)).agency+arr.get(position)));
			else check.setChecked(favs.checkStop(Manager.stopMap.get(arr.get(position)).get(0).agency+arr.get(position)));
			check.stopUpdate=false;
			
			StringBuilder b = new StringBuilder("Routes Served: ");
			boolean isSched = false;
			boolean isReal = false;
			//WMATA already have the correct route names and routes served encoded in routeName (as a comma-delimited list)
			if(Manager.isWMATA()) {
				b.append(stopMap.get(arr.get(position)).routeName.replace("r_", ""));
				isReal=true;
			} else {
				ArrayList<SimpleStop> stops = Manager.stopMap.get(arr.get(position));
				HashSet<String> routeNamesAlreadyListed = new HashSet<String>();
				
				
				boolean isFirstRouteInb = true;
				for (SimpleStop s : stops) {
					//The below code makes sure to avoid duplicate listings in "Routes Served: <routes>"
					if(!routeNamesAlreadyListed.contains(Utils.routeStripTrailing(s.routeName))) {
						//Only prepend a comma if we are not the first route entry to be placed here 
						if(isFirstRouteInb) isFirstRouteInb = false;
						else b.append(", ");
						
						b.append(Utils.routeStripTrailing(s.routeName));
						routeNamesAlreadyListed.add(Utils.routeStripTrailing(s.routeName));
					}
					
					if (!Manager.isScheduleApp()) {isReal = true;} else {isSched = true;}
				}
			}
			
			if (position % 2 == 0) {
				row.setBackgroundResource(R.drawable.cust_list_selector);
			}
			
			//If we are processing the first item, use its prediction status to set the title of the screen, telling the
			//user whether predictions are real-time or not
			//We assume that all stops on a given route are either all Real-Time, or not.
			if(position==0) {
				context.isRealTimeSchedule(isReal, isSched);
			}
			
			/*if (isSched) {
				sched.setText("Scheduled Arrivals");
				sched.setTextColor(0xFFE0B000);
			} 
			if (isReal) {
				sched.setText("Real Time Predictions");
				sched.setTextColor(0xFF009900);
			} 

			if (isSched && isReal) {
				sched.setText("Predictions & Schedules");
				sched.setTextColor(0xFF28A400);
			}*/
			
			if( (Manager.isWMATA() && position == 0) || (!Manager.isWMATA() && position == nearest_stop_index) ) {
				sched.setText("Nearest Stop");
				sched.setTextColor(0xFF009900);
			}
			else sched.setVisibility(View.GONE);
			
			label.setText(arr.get(position));
			content.setText(b.toString());
			//ImageView icon=(ImageView)row.findViewById(R.id.icon);
			return(row);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode==-1) {
			showError();
		}
		if (resultCode==3) {

			Manager.stringTracker=arr.get(lastPosition);
			Intent i = new Intent(this, StopDrill.class);
	    	startActivityForResult(i,0);
		}
	}
	
	private void showError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Route List Error")
        .setIcon(R.drawable.ico)
        .setMessage("It seems that there are no routes associated with the chosen stop; " +
        		"this is most likely because you are near a route we do not support, " +
        		"or if could be a problem with the underlying data.\n" +
        		"Our data is constanly improving, so please check back later!")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        SimpleStop errorStop;
        
        if(Manager.isWMATA()) errorStop = stopMap.get(arr.get(lastPosition));
        else errorStop= Manager.stopMap.get(arr.get(lastPosition)).get(0);
        
        if(errorStop != null)
	        Manager.flurryNoneFoundEvent(Manager.currAgency, 
					Manager.routeTracker, 
					errorStop.intersection);
        else
        	Manager.flurryNoneFoundEvent(Manager.currAgency, 
					Manager.routeTracker);
        
        b.show();
	}
		

	/** Overrides the default onDestroy. Special things we do in AgencyRouteList.onDestroy():
	 * 1. Pop searchQueryStack, if this was launched through the search dialog.
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(Manager.isWMATA()) WMATATransitDataManager.popCommand();
	}
	
	/** This method gets called by the iconic adapater to set the "Prediction or Schedule" status
	 * of this StopList list.
	 * @param isRealTime Set whether we display that Real-Time info is available or not.
	 */
	public void isRealTimeSchedule(boolean isRealTime, boolean isSched) {
		TextView dirSched = (TextView) findViewById(R.id.dir_sched);
		
		if(dirSched == null) {
			Log.e(activityNameTag, "Somehow, isRealTimeSchedule() could not get dir_sched TextView from layout.");
			return;
		}
		
		if (isSched) {
			dirSched.setText("Scheduled Arrivals");
			dirSched.setTextColor(0xFFE0B000);
		} 
		if (isRealTime) {
			dirSched.setText("Real Time Predictions");
			dirSched.setTextColor(0xFF009900);
		} 

		if (isRealTime && isSched) {
			dirSched.setText("Predictions & Schedules");
			dirSched.setTextColor(0xFF28A400);
		}
	}

	//Used to call DataThread from onListItemClick
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			DataThread d = new DataThread();
				pd = ProgressDialog.show(me, "Getting Stops",
						"Contacting the Server:\nStops Near Me",
						true, false);

				d.start();

		};
	};
	
	//Used to fetch data from the server
	class DataThread extends Thread {
		public DataThread() {

		}

		public void run() {
			Manager.clearStops();
			try {
				if(Manager.isWMATA()) {
					SimpleStop selectedStop = stopMap.get(Manager.stringTracker);
					
					if(selectedStop != null)
						WMATATransitDataManager.fetchPredictionsByStop(selectedStop);
				} else Manager.repeat();
			} catch (NoneFoundException e) {
				e.printStackTrace();
			} catch (ServerBarfException e) {
				e.printStackTrace();
			}
			pd.dismiss();
			stact.sendEmptyMessage(0);

		}
	};
	
	//Used to launch the next activity (StopDrill) after DataThread returns
	 private Handler stact = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
			 Intent i = new Intent(me, StopDrill.class);
			 i.putExtra("CallingActivity", activityNameTag);
			 startActivityForResult(i,0);
		 }
			
	 };

}
