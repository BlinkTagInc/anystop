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
import org.busbrothers.anystop.agencytoken.activities.RouteDrill.IconicAdapter;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.map.StopMap;
import org.busbrothers.anystop.agencytoken.uicomponents.CustomList;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class StopDrill extends CustomList {

	StopDrill me;
	
	ProgressDialog pd;
	//TextView selection;
	List<SimpleStop> tempArr;
	ArrayList<SimpleStop> arr;
	Button mapButton, refresh;
	SelfResizingTextView title;
	IconicAdapter i;
	
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/	
	
	private static final String activityNameTag = "StopDrill";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	private boolean doAutoRefresh;
	private int refresh_period_in_seconds;
	
	@Override
	public void onCreate(Bundle icicle) {
		
		me=this;
		String searchQuery = null;
				
		_usageMessages= new String[] {"This is a list of the routes which service this stop.",
				"Predictions for each route are shown.",
				"The 'Refresh' button below will refresh the prediction information.",
				"To see this stop on the map, along with your position, hit 'Show on Map'."
		};
		
		//The below block of code sets up the AdView for this Activity, and adds it to the Layout
		setContentView(R.layout.stopdrill);
		
		Log.v(activityNameTag, activityNameTag+" was opened, but not in a search.");

		if(Manager.isWMATA()) tempArr = (ArrayList<SimpleStop>) WMATATransitDataManager.peekLastData();	
		else tempArr = Manager.stopMap.get(Manager.stringTracker);
		
		//Make sure that we were able to retrieve the things from tempArr 
		if (tempArr==null) {
			this.setResult(-1);
			this.finish();
		}else {
			//Transfer all of the things in tempArr into arr, which is an ArrayList so we can remove things from it later :)
			//(disabled the code for filtering stops based on headsign)
			arr = new ArrayList<SimpleStop>();
			//ArrayList <String> directionsInArr = new ArrayList<String>();
			Iterator <SimpleStop> tempArrIterator = tempArr.iterator();
			while(tempArrIterator.hasNext()){
				SimpleStop currStop = tempArrIterator.next();
				arr.add(currStop);
				
				//Case - we have already seen this head sign (direction)
				//if(directionsInArr.contains(currStop.headSign));
				//Case - we have NOT seen this direction yet
				//else {
				//	directionsInArr.add(currStop.headSign);
				//	arr.add(currStop);
				//}
			}
			
			Collections.sort(arr);
			i = new IconicAdapter(this);
			setListAdapter(i);
			this.setResult(0);
		}
		
		this.title = (SelfResizingTextView) findViewById(R.id.title);
		title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
		title.setText("Predicted Arrival Times for " + Manager.stringTracker);
		
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
		
    	mapButton = (Button) findViewById(R.id.back_map);
    	refresh = (Button) findViewById(R.id.refresh);
    	
    	mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(arr.size() > 0) {
	            	Manager.lat = arr.get(0).lat;
	            	Manager.lon = arr.get(0).lon;
	            	Intent i = new Intent(me, StopMap.class);
	        	 	startActivity(i);
            	} else {
            		Toast t = Toast.makeText(me, "Couldn't find any stops to display predictions for; cannot display location of stop.", Toast.LENGTH_LONG);
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
					Log.v("StopDrill", "Search button was clicked, launching search dialog! IVANY");
					onSearchRequested();
				}
			});
			searchButton.setVisibility(View.GONE); //search button not very relevant, this view will only have a few items in it
		} else {
			//This should never happen? Right?
			Log.v("StopDrill", "Somehow searchButton was null...IVANY?!");
		}
		
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
			
			//SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
			//title.setText("Stops matching \"" + searchQuery + "\"");
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
		}
		else if(Manager.adTypeUsing() == Manager.ADDIENCE_AD) {
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
	 	//selection.setText(actions[position]);
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
		Activity context;

		IconicAdapter(Activity context) {
			super(context, R.layout.sched, arr);

			this.context=context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater=LayoutInflater.from(context);
			
			View row=inflater.inflate(R.layout.sched, null);
			TextView label=(TextView)row.findViewById(R.id.dir_label);
			TextView content=(TextView)row.findViewById(R.id.dir_subinfo);
			TextView sched = (TextView)row.findViewById(R.id.dir_sched);
			SimpleStop stop = arr.get(position);
			Manager.applyFonts(row);
			
			if (position % 2 == 0) {
				row.setBackgroundResource(R.drawable.cust_list_selector);
			}
			
			StringBuilder b = new StringBuilder("");
			
			b.append("Direction: " + Utils.fmtHeadsign(Utils.checkHeadsign(stop.headSign)));
			
			ArrayList<String> preds = stop.pred.format();
			//if(stops.size() > 0) b.append("\n");
			
			//Append to subtext the predictions for this stop
			if (preds.size()==0) {
				b.append("\nNo predictions at this time");
			} else {
				TableLayout predictionTable = (TableLayout) inflater.inflate(R.layout.predictiontable, null);
				
				boolean first_string = true;
				
				for (String s : preds) {
					String [] predictionTime = s.split("-");
					if(predictionTime.length == 2) {
						TableRow tr = (TableRow) inflater.inflate(R.layout.predictiontablerow, null);
						
						TextView deltaTime = (TextView) tr.findViewById(R.id.deltatime);
						TextView absTime = (TextView) tr.findViewById(R.id.abstime);
						
						deltaTime.setText(predictionTime[0]);
						
						//Hax for TheBUS since TheBUS sometimes has real-time predictions for the next arrival
						if(stop.pred.isRT && Manager.getAgencyTag().equals("thebus") && first_string) {
							absTime.setText(predictionTime[1] + "( real-time)");
							absTime.setTextColor(0xFF009900);
							deltaTime.setTextColor(0xFF009900);
						}
						else 
							absTime.setText(predictionTime[1]);
						
						
						Manager.applyFonts(deltaTime);
						Manager.applyFonts(absTime);
						
						predictionTable.addView(tr);
						
						first_string = false;
					}
					//b.append(s);
					//if (preds.indexOf(s)!=preds.size()-1) {b.append("\n");};
				}
				
				LinearLayout textSectionLL = (LinearLayout) row.findViewById(R.id.row_textsection);
				textSectionLL.addView(predictionTable);
			}
			
			if (!Manager.isScheduleApp()) {
				sched.setText("Real Time Predictions:");
				sched.setTextColor(0xFF009900);
			} else {
				sched.setText("Scheduled Arrivals:");
				sched.setTextColor(0xFFE0B000);
			} 
			label.setText(stop.routeName);
			content.setText(b.toString());
			ImageView icon=(ImageView)row.findViewById(R.id.icon);
			icon.setImageResource(R.drawable.rail);
			return(row);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode==-1) {
			showError();
		}
	}
	
	private void showError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Problem")
        .setIcon(R.drawable.ico)
        .setMessage("We have encountered an error with your request!")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}

	class DataThread extends Thread {
		public DataThread() {

		}

		public void run() {
			//TODO: Fix for WMATA
			Manager.clearStops();
			// String lat = loc.getLatitude()+"";
			// String lon = loc.getLongitude()+"";
			try {
				//TODO: Fix for WMATA
				if(Manager.isWMATA()) WMATATransitDataManager.repeat();
				else Manager.repeat();
			} catch (NoneFoundException e) {
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
			 	//TODO: Fix for WMATA
				arr = Manager.stopMap.get(Manager.stringTracker);
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
	 * 1. Nothing.
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(Manager.isWMATA()) WMATATransitDataManager.popCommand();
	}
	
	/**This method will attempt to refresh the prediction displayed on this StopDrill screen. */
	private void doRefresh() {
		try {
    		handler.sendEmptyMessage(0);
		} catch (Exception e) {
			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
					"Please try again later!", Toast.LENGTH_LONG);
			t.show();
		}
	}
	
	/**This Handler exists soley to implement doRefresh()-based auto refresh behavior */
	private Handler autoRefreshRunnableTaskHandler = new Handler();
	
	/** This Runnable handles the automatic updating of the data displayed by this StopDrill. It will update 
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
