package org.busbrothers.anystop.agencytoken.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.Utils;
import org.busbrothers.anystop.agencytoken.activities.FavRoutes.IconicAdapter;
import org.busbrothers.anystop.agencytoken.datacomponents.Favorites;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.datacomponents.SpecialSort;
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

public class RouteList extends CustomList {

	RouteList me;
	static String copyTracker = "";

	/*public static RouteList single() {
		return me;
	}*/

	// TextView selection;
	ArrayList<String> arr;
	IconicAdapter theListAdapter;
	
	Button mapButton;
	ProgressDialog pd;
	
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/
	
	private static final String activityNameTag = "RouteList";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting

	@Override
	public void onCreate(Bundle icicle) {
		me = this;
		
		_usageMessages= new String[] {"Here is a list of routes. Below each route is a list of stops near your location.",
				"Select a route from this list to view predictions.",
				"You can turn off screen instructions in the Preferences."		};
		
		//The below block of code sets up the AdView for this Activity, and adds it to the Layout
		setContentView(R.layout.routelist);
		LinearLayout adLayoutParent = (LinearLayout) findViewById(R.id.dlist_ad_holder); //this LinearLayout will contain the AdView
		
		//The below code sets up Advertisement-related stuff for this activity
		if(Manager.adTypeUsing() == Manager.ADMOB_AD) {			
			//The below block of code sets up the AdView for this Activity, and adds it to the Layout
			AdView googleAdMobAd = Manager.setupAdView(this); //have Manager set up the AdView for us :)
			adLayoutParent.addView(googleAdMobAd);
			
			//Hide (& disable?) the Addience adview
			AswAdLayout adView = (AswAdLayout)findViewById(R.id.addience_adview);
			adView.setVisibility(View.GONE);
			adView.optOut();
		}
		else if(Manager.adTypeUsing() == Manager.ADDIENCE_AD) {
			//Don't have to disable the Admob adview, since we never create it in the first place
			AswAdLayout adView = (AswAdLayout)findViewById(R.id.addience_adview);
			Manager.setupAddienceAd(this, adView);
		}
		
		adLayoutParent.invalidate();
		
		Log.v(activityNameTag, activityNameTag+" was opened, but not in a search.");

		String[] actions = new String[Manager.routeMap.keySet().size()];
		List<String> tempArr = Arrays.asList(Manager.routeMap.keySet().toArray(actions));
		arr = new ArrayList<String>();

		if (tempArr == null) {
			this.setResult(-1);
			this.finish();
		} else {
			Iterator<String> tempArrIterator = tempArr.iterator();
			while(tempArrIterator.hasNext()) arr.add(tempArrIterator.next());
			
			Collections.sort(arr);
			SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
			title.setText("Routes Near Me");
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
				String currRoute = theListAdapter.getItem(currentPosition);
				boolean passFilter = true; //passFilter will remain set to true if currRoute matches all search queries in searchQueryStack.
				
				//For each Route, we go through the entire search query stack, comparing all queries against currRoute.sName
				searchQueryStackIterator = searchQueryStack.iterator();
				while(searchQueryStackIterator.hasNext()) {
					String currQuery = searchQueryStackIterator.next();
					passFilter = passFilter && currRoute.toLowerCase().contains(currQuery.toLowerCase());
				}
				
				if(!passFilter) positionsToFilter.add(currRoute);
			}
			
			//Remove all positions that we should filter
			for(int i = 0; i < positionsToFilter.size(); i++) {
				theListAdapter.remove(positionsToFilter.get(i));
			}			
			theListAdapter.notifyDataSetChanged();
			
			SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			title.setText("Routes matching \"" + searchQuery + "\"");
		}
	}
	
	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
		Manager.stringTracker = arr.get(position);
		Manager.routeTracker = arr.get(position);
		
		Manager.flurryRouteSelectEvent(arr.get(position));

		v.postDelayed(new Runnable() {
            public void run() {
            	Intent rdIntent = new Intent(me, RouteDrill.class);
            	rdIntent.putExtra("CallingActivity", activityNameTag);
            	startActivityForResult(rdIntent, 0);
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

	class IconicAdapter extends ArrayAdapter<String> {
		Activity context;

		IconicAdapter(Activity context) {
			super(context, R.layout.sched, arr);

			this.context = context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);

			View row = inflater.inflate(R.layout.sched, null);
			TextView label = (TextView) row.findViewById(R.id.dir_label);
			TextView content = (TextView) row.findViewById(R.id.dir_subinfo);
			TextView sched = (TextView) row.findViewById(R.id.dir_sched);
			IndexCheck check=(IndexCheck)row.findViewById(R.id.favbox);
			Manager.applyFonts(row);

			StringBuilder b = new StringBuilder("Nearest Stops:");
			ArrayList<SimpleStop> stops = Manager.routeMap.get(arr
					.get(position));
			
			Collections.sort(stops, new SpecialSort());
			boolean isSched = false;
			boolean isReal = false;
			
			SimpleStop prevStop = null;
			for (SimpleStop s : stops) {
				String intersection = s.intersection;
				if (s.pred.isRT) {isReal = true;} else {isSched = true;}
				
				//Check to make sure we don't have duplicate stop names (SimpleStop.intersection) in our nearest stops list
				if(prevStop == null)
					b.append("\n" + intersection);
				else if(!s.intersection.equals(prevStop.intersection))
					b.append("\n" + intersection);
				//else do nothing
				
				prevStop = s;
			}
			
			//Figure out if we need to set checked or not, and also add listeners so that we add the Route to Favorites
			//if the favorites "heart" gets clicked on
			check.index = position;
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					//mash agency name and intersection
					if (((IndexCheck)buttonView).stopUpdate==true) {return;} //Some kind of mutex here? Does this even work correctly?
					String ag = arr.get(((IndexCheck)buttonView).index);
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
			
			if (isSched) {
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
			}

			label.setText(Utils.capFirst(arr.get(position).trim()));
			content.setText(b.toString());
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			icon.setImageResource(R.drawable.rail);

			// row.setOnClickListener(cl);

			return (row);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == -1) {
			showError();
		}
	}

	private void showError() {
		Builder b = new AlertDialog.Builder(this)
				.setTitle("Stops Error")
				.setIcon(R.drawable.ico)
				.setMessage(
						"It seems that there are no stops associated with the chosen route; " +
						"this is most likely because you are near stops we do not support, " +
		        		"or if could be a problem with the underlying data.\n" +
		        		"Our data is constanly improving, so please check back later!")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Put your code in here for a positive response
					}
				});

		b.show();
	}

	class DataThread extends Thread {
		public DataThread() {

		}

		public void run() {
			Manager.clearStops();
			try {
				Manager.repeat();
			} catch (NoneFoundException e) {
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
						"Contacting the Server:\nRoutes Near Me",
						true, false);

				d.start();

		};
	};
	
	 private Handler stact = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
			 Intent i = new Intent(me, RouteDrill.class);
			 startActivityForResult(i,0);
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
	 * 1. Nothing.
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}


