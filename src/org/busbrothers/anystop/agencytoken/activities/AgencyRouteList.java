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
import android.app.AlertDialog.Builder;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AgencyRouteList extends CustomList {

	static String keybuffer;
	
	AgencyRouteList me;
	/*public static AgencyRouteList single() {
		return me;
	}*/
	
	IconicAdapter theListAdapter;
	ProgressDialog pd;
	//TextView selection;
	List<Route> arr; //!<A List of Routes that will be displayed by this AgencyRouteList
	Button mapButton;
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	//TextView search; //!<Ivany: I don't know what this is used for. Maybe it isn't?!
	
	private static final String activityNameTag = "AgencyRouteList";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
		
	boolean isASearchedActivity; //!<Whether this AgencyRouteList has been launched from a search dialog or not
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/
	
	/** OnCreate of course gets called when the AgencyRouteList activity is created. 
	 * OnCreate is also responsible for determining whether this AgencyRouteList instance is called with the 
	 *  ACTION_SEARCH intent, and if so, to extract the Search Query from the intent. 
	 *  
	 */
	@Override
	public void onCreate(Bundle icicle) {		
		me=this;
		
		_usageMessages= new String[] {"This is a simple list of routes for the chosen agency.",
				"Select a route to view its stops.",
				"You can turn off screen instructions in the Preferences."
		};
		
		setContentView(R.layout.agencyroutelist);
		
		//The below was added to support AgencyRouteList as a Searchable Activity; it determines if we were opened to search
		//and if so does the appropriate thing with the provided query
		isASearchedActivity = false;
				
		//The below code will retrieve the Routes to be displayed in this RouteList
		//Also it will filter them, (if there are search queries to filter by)
		//Get arr, the List of Routes, from Manager
		if(Manager.isWMATA()) arr = (ArrayList<Route>) WMATATransitDataManager.peekLastData();
		else arr = Manager.agencyRoutes;
		
		for ( Route r : arr ) {
			Log.d("AgencyRouteList", "Got route: (" + r.sName + ", " + r.lName + ")");
		}
		
		if (arr==null) {
			this.setResult(-1);
			this.finish();
		}else {
			Log.d(activityNameTag, "AgencyRouteList has started sorting; arr has " + arr.size() + "members...");
			long startSortTime = System.currentTimeMillis();
			Collections.sort(arr);
			Utils.sortRoutesBySpecial(arr);
			
			long endSortTime = System.currentTimeMillis();
			Log.v(activityNameTag, "AgencyRoute sorting took " + (endSortTime-startSortTime)/1000.0 + 
				" seconds to sort route names. Sorted " + arr.size() + " names.");
			
			//setContentView(R.layout.agencyroutelist);
			theListAdapter = new IconicAdapter(this);
			setListAdapter(theListAdapter);
			
			//Set title and subtitle for this Activity window
			SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
			TextView subtitle = (TextView) findViewById(R.id.subtitle);
			
			//Set subtitle "hint" also
			String subtitleHint;
			subtitleHint = (String) getString(R.string.AgencyRouteList_SubtitleHint);
			subtitle.setText(subtitleHint);
			
			title.setText("Select a Route");
			subtitle.setVisibility(View.GONE);
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
			//adLayoutParent.AswAdLayout adView = (AswAdLayout)findViewById(R.id.addience_adview);
			//adView.setActivity(this);
			
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
		if(Intent.ACTION_SEARCH.equals(mIntent.getAction())) {
			isASearchedActivity = true;
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
			ArrayList<Route> positionsToFilter = new ArrayList<Route>();
			for(int currentPosition = 0; currentPosition < theListAdapter.getCount(); currentPosition++) {
				Route currRoute = theListAdapter.getItem(currentPosition);
				boolean passFilter = true; //passFilter will remain set to true if currRoute matches all search queries in searchQueryStack.
				
				//For each Route, we go through the entire search query stack, comparing all queries against currRoute.sName
				searchQueryStackIterator = searchQueryStack.iterator();
				while(searchQueryStackIterator.hasNext()) {
					String currQuery = searchQueryStackIterator.next();
					if(Manager.isWMATA()) passFilter = passFilter && currRoute.lName.toLowerCase().contains(currQuery.toLowerCase());
					else passFilter = passFilter && currRoute.sName.toLowerCase().contains(currQuery.toLowerCase());
				}
				
				if(!passFilter) {
					Log.d("AgencyRouteList", "Route " + currRoute.lName + " didn't pass filter");
					positionsToFilter.add(currRoute);
				} else
					Log.d("AgencyRouteList", "Route " + currRoute.lName + " passed filter");
			}
			
			//Remove all positions that we should filter
			for(int i = 0; i < positionsToFilter.size(); i++) {
				theListAdapter.remove(positionsToFilter.get(i));
			}			
			theListAdapter.notifyDataSetChanged();
			
			SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			TextView subtitle = (TextView) findViewById(R.id.subtitle);
			title.setText("Route matching \"" + searchQuery + "\"");
			subtitle.setVisibility(View.VISIBLE);
		}
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
		Manager.positionTracker=position;
		Manager.flurryRouteSelectEvent(arr.get(position));
		
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

	class IconicAdapter extends ArrayAdapter<Route> {
		Activity context;

		IconicAdapter(Activity context) {
			super(context, R.layout.sched, arr);

			this.context=context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater=LayoutInflater.from(context);
			
			View row=inflater.inflate(R.layout.sched, null);
			TextView label=(TextView)row.findViewById(R.id.dir_label);
			TextView content = (TextView) row.findViewById(R.id.dir_subinfo);
			TextView sched = (TextView)row.findViewById(R.id.dir_sched);
			IndexCheck check=(IndexCheck)row.findViewById(R.id.favbox);
			Manager.applyFonts(row);
			
			//Figure out if we need to set checked or not, and also add listeners so that we add the Route to Favorites
			//if the favorites "heart" gets clicked on
			check.index = position;
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					//mash agency name and intersection
					if (((IndexCheck)buttonView).stopUpdate==true) {return;} //Some kind of mutex here? Does this even work correctly?
					Route ag = arr.get(((IndexCheck)buttonView).index);
					if (isChecked) {
						Favorites.getInstance().addRoute(ag);
						Toast t = Toast.makeText(me, "Added to Favorites", Toast.LENGTH_SHORT);
	        			t.show();
					} else {
						Favorites.getInstance().removeRoute(ag);
						Toast t = Toast.makeText(me, "Removed from Favorites", Toast.LENGTH_SHORT);
	        			t.show();
					}
				}
			});
			Favorites favs = Favorites.getInstance();
			check.stopUpdate=true;
			check.setChecked(favs.checkRoute(arr.get(position)));
			check.stopUpdate=false;
			check.setVisibility(View.VISIBLE);
			
			if (position % 2 == 0) {
				row.setBackgroundResource(R.drawable.cust_list_selector);
			}

			if (arr.get(position).isRT) {
				sched.setText("Real Time Predictions");
				sched.setTextColor(0xFF00DD00);
			} else {
				sched.setText("Schedule Info");
				sched.setTextColor(0xFFE0B000);
			} 

			Log.v(activityNameTag, "Route has sName=" + arr.get(position).sName + ", lName=" + arr.get(position).lName);
			
			if(Manager.isWMATA()) {
				String routeName = arr.get(position).lName;
				routeName = routeName.replace("r_", "");
				label.setText(Utils.capFirst(routeName.trim()));
			} else
				label.setText(Utils.capFirst(arr.get(position).lName.trim()));
			
			ImageView icon=(ImageView)row.findViewById(R.id.icon);
			//content.setText("Tap to view stops");
			//content.setHeight(3);
			content.setVisibility(View.GONE);
			icon.setImageResource(R.drawable.rail);
			return(row);
		}
	}
	
	class DataThread extends Thread {
		public DataThread() {

		}
		public void run() {
				Manager.stringTracker=arr.get(Manager.positionTracker).lName;
				Manager.routeTracker=arr.get(Manager.positionTracker).sName;
				try {
					if(Manager.isWMATA()) WMATATransitDataManager.fetchStopsByRoute(arr.get(Manager.positionTracker));
					else Manager.loadAgencyStop(Manager.currAgency, arr.get(Manager.positionTracker).sName);
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
        	pd = ProgressDialog.show(me, "Getting Agency Routes", "Contacting the Server:\nGetting Stops for Route", true, false);
        	d.start();
		}
	};
	private Handler stact = new Handler() {
		@Override
		public void handleMessage(Message msg) {
				Intent i = new Intent(me, AgencyRouteDrill.class);
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
        .setMessage("It seems that there are no stops associated with the chosen route; " +
        		"this is most likely because you are near a route we do not support, " +
        		"or if could be a problem with the underlying data.\n" +
        		"Our data is constanly improving, so please check back later!")
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
	 * (1) Nothing
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		//Determine if this AgencyRouteList was launched as a searched activity
		Intent mIntent = getIntent(); //mIntent is just this Activity's intent
		
		//Pop the result stack if we are killing the ARL that was NOT a searched activity
		Log.d("AgencyRouteList", "Clearing WMATATransitDataManager command");
		WMATATransitDataManager.popCommand();
		Log.d("AgencyRouteList", "Cleared WMATATransitDataManager command");
		
		super.onDestroy();
	}

	

}
