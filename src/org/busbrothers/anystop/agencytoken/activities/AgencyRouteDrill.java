package org.busbrothers.anystop.agencytoken.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.Utils;
import org.busbrothers.anystop.agencytoken.WMATATransitDataManager;
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

public class AgencyRouteDrill extends CustomList {

	AgencyRouteDrill me;
	
	/*public static AgencyRouteDrill single() {
		return me;
	}*/
	
	//TextView selection;
	boolean isASearchedActivity;
	List<SimpleStop> arr; //!<The List of SimpleStop objects (representing transit stops) that will be displayed in this AgencyRouteDrill.
	int nearest_stop_index; //!<The SimpleStop with this index in arr will be the nearest stop to the user's current location
	Button mapButton, refresh;
	ProgressDialog pd;
	IconicAdapter i;
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	//TextView search; //!<Ivany: I don't know what this is used for. Maybe it isn't?!
	
	private static final String activityNameTag = "AgencyRouteDrill";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/
	
	@Override
	public void onCreate(Bundle icicle) {
		me=this;

		_usageMessages= new String[] {"Here is a list of stops for the chosen route, along with the stop's predictions.",
				"Select a stop from this list to view it on the map.",
				"You can save a stop to favorites by clicking the heart on the right hand side!",
				"The 'Refresh' button below will refresh the prediction information."
		};
		
		setContentView(R.layout.agencyroutedrill);
		
		
		
		Log.v(activityNameTag, activityNameTag+" was opened, but not in a search.");
		
		if(Manager.routeMap == null) Log.w(activityNameTag, "routeMap was null");
		if(Manager.stringTracker == null) Log.w(activityNameTag, "stringTracker was null");
		
		
		if(Manager.isWMATA()) arr = (ArrayList<SimpleStop>) WMATATransitDataManager.peekLastData();
		else arr = Manager.routeMap.get(Manager.stringTracker);
		
		isASearchedActivity = false;
		
		if (arr==null) {
			this.setResult(-1);
			this.finish();
		}else {
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
		
		SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
		title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
		if (Manager.isWMATA()) title.setText("Route " + Manager.stringTracker.replace("r_", ""));
		else title.setText("Route " + Manager.stringTracker);
		//Set subtitle "hint" also
		String subtitleHint;
		subtitleHint = (String) getString(R.string.AgencyRouteDrill_SubtitleHint);
		TextView subtitle = (TextView) findViewById(R.id.subtitle);
		subtitle.setText(subtitleHint);
		
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
	
	/** This method gets called if this activity is at the top of the stack and its launch mode is set to singleTop in the App manifest.
	 *  
	 */
	@Override
	protected void onNewIntent(Intent mIntent) { 
		IconicAdapter theListAdapter = i;
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
			TextView subtitle = (TextView) findViewById(R.id.subtitle);
			title.setText("Stops matching \"" + searchQuery + "\"");
			subtitle.setVisibility(View.VISIBLE);
		}
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
		Manager.positionTracker=position;
		
		Manager.flurryPredictionEvent(this, arr.get(position));
		
		v.postDelayed(new Runnable() {
            public void run() {
        		try {
        			handler.sendEmptyMessage(0);
        		} catch (Exception e) {
        			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
        					"Please try again later!", Toast.LENGTH_LONG);
        			t.show();
        		}
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
		AgencyRouteDrill context;

		IconicAdapter(AgencyRouteDrill context) {
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
			
			//Fill in the subtext for each stop - direction & prediction data
			StringBuilder b = new StringBuilder(Utils.checkHeadsign(arr.get(position).headSign));
			Log.v(activityNameTag, "Headsign for stop " + arr.get(position).intersection + " was " + arr.get(position).headSign);
			//Log.v(activityNameTag, "Trimmed headsign for stop " + arr.get(position).intersection + " is " + Utils.fmtHeadsign(arr.get(position).headSign));
			
			//Append to subtext the routes served by this stop
			//As of 2011-12-19, this doesn't work; it would be nice to get it to work but it can't really be done efficiently the way the server
			//side works right now. The server side only returns the Stops that are associated with this route.
			/*ArrayList<SimpleStop> stops = Manager.stopMap.get(arr.get(position).intersection);
			if(stops.size() > 0) b.append("\nRoutes Served: ");
			else Log.i(activityNameTag, "Couldn't get routes served for SimpleStop with intersection \"" + arr.get(position).intersection + "\"");
			for (SimpleStop s : stops) {
				b.append(s.routeName);
				if(stops.indexOf(s) != stops.size()-1) b.append(", ");
			}*/
			
			//Append to subtext the predictions for this stop
			if(arr.get(position).pred != null) {
				ArrayList<String> preds = arr.get(position).pred.format();

				for (String s : preds) {
					if(b.length() > 0) b.append("\n");
					b.append(s);
				}
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
			
			if(position == nearest_stop_index) {
				sched.setText("Nearest Stop");
				sched.setTextColor(0xFF009900);
			}
			else sched.setVisibility(View.GONE);
			
			label.setText(arr.get(position).intersection);
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
			try {
				if(Manager.isWMATA()) WMATATransitDataManager.fetchPredictionsByStop(arr.get(Manager.positionTracker));
				else Manager.loadAgencyStopPred(
						Manager.currAgency, 
						Manager.routeTracker, 
						arr.get(Manager.positionTracker).intersection);
			} catch (NoneFoundException e) {
				errorHandler.sendEmptyMessage(0); return;
			} catch (ServerBarfException e) {
				errorHandler.sendEmptyMessage(1); return;
			}
			pd.dismiss();
			stact.sendEmptyMessage(0);
		}
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
        	DataThread d = new DataThread();
        	pd = ProgressDialog.show(me, "Getting Stop", "Contacting the Server:\nRefreshing Predictions", true, false);
        	d.start();
		}
	};
	private Handler stact = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Intent i = new Intent(me, RouteDrill.class);
			i.putExtra("CallingActivity", activityNameTag);
			startActivityForResult(i,0);
		}
	};
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode==-1) {
			showError();
		}
	}
	
	private void showError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Route Stops Error")
        .setIcon(R.drawable.ico)
        .setMessage("It seems that there are no predictions associated with the chosen stop; " +
        		"this is most likely because this schedule is not running today, " +
        		"or you might not be near any routes.\n" +
        		"If you feel this is in error, PLEASE provide feedback to busbrothers@gmail.com!")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}
	
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
	
	/** Overrides the default onDestroy. Special things we do in AgencyRouteList.onDestroy():
	 * 1. Nothing
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		//Determine if this AgencyRouteList was launched as a searched activity
		Intent mIntent = getIntent(); //!<mIntent is just this Activity's intent
		
		//Pop the result stack if we are killing the ARL that was NOT a searched activity
		WMATATransitDataManager.popCommand();
		
		super.onDestroy();
	}
	
	/** This method gets called by the iconic adapater to set the "Prediction or Schedule" status
	 * of this AgencyRouteDrill list.
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
}
