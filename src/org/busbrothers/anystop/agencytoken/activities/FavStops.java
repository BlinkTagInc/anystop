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
import org.busbrothers.anystop.agencytoken.activities.AgencyRouteDrill.IconicAdapter;
import org.busbrothers.anystop.agencytoken.datacomponents.Agency;
import org.busbrothers.anystop.agencytoken.datacomponents.Favorites;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
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

public class FavStops extends CustomList {

	FavStops me;
	
	ProgressDialog pd;
	
	//TextView selection;
	ArrayList<SimpleStop> arr;
	IconicAdapter theListAdapter;
	
	Button searchButton; //!<A button that will invoke the search dialog by calling onSearchRequested().
	
	Stack <String> searchQueryStack = new Stack<String>(); /**<A Stack of search queries, which will ALL be used to filter
	 the route results. Basically, every time we run a search operation, we will push the new query to this stack, and the entirety
	 of the stack will be used to filter the results. For example, if the user decides to search for "A" and "B", only routes with BOTH
	 "A" and "B" in their names (sName) will be returned.*/	
	
	private static final String activityNameTag = "FavStops";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	
	@Override
	public void onCreate(Bundle icicle) {
		
		me=this;
		String searchQuery = null;
		
		_usageMessages= new String[] {"Here is a list of favorite stops!",
				"Select a stop from this list to view all the routes servicing it.",
				"You can remove a stop from favorites by clicking the heart on the right hand side."
		};
		
		//The below block of code sets up the AdView for this Activity, and adds it to the Layout
		setContentView(R.layout.stoplist);
		
		Log.v(activityNameTag, activityNameTag+" was opened, but not in a search.");
		
		arr=Manager.favstops;
		if (arr==null) {
			this.setResult(-1);
			this.finish();
		}else {
			Collections.sort(arr);
			TextView title = (TextView) findViewById(R.id.title);
			title.setText("My Favorite Stops");
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
		
		//Hide the "Prediction or Schedule" title sub-text
		TextView predOrSched = (TextView) findViewById(R.id.dir_sched);
		if(predOrSched != null) predOrSched.setVisibility(View.GONE);
		SelfResizingTextView title = (SelfResizingTextView) findViewById(R.id.title);
		title.setResizeParams(Manager.LISTWINDOW_START_FONTSIZE, Manager.LISTWINDOW_MIN_FONTSIZE, Manager.LISTWINDOW_MAX_NUMLINES, Manager.LISTWINDOW_MAX_HEIGHT);
		
		if(arr.size() == 0) showNoStopsError();
		
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
	
	/**
	 * This method gets called when any item in the Favorite Stops list (representing a favorite-ed Stop) gets clicked.
	 *  It generates a prediction event and something-or-other.
	 */
	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
		Manager.positionTracker=position;
		Manager.stringTracker=arr.get(position).intersection;
		
		Manager.flurryPredictionEvent(this, arr.get(position));
		
		v.postDelayed(new Runnable() {
            public void run() {
        		try {
        			Manager.viewing = Manager.STOP;
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
		Activity context;

		IconicAdapter(Activity context) {
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
					SimpleStop ag = arr.get(((IndexCheck)buttonView).index);
					if (isChecked) {
						Favorites.getInstance().addStop(ag);
					} else {
						Favorites.getInstance().removeStop(ag);
					}
				}
			});
			Favorites favs = Favorites.getInstance();
			check.stopUpdate=true;
			check.setChecked(favs.checkStop(arr.get(position).agency+ arr.get(position).intersection));
			check.stopUpdate=false;
			
			if (position % 2 == 0) {
				row.setBackgroundResource(R.drawable.cust_list_selector);
			}
			
			if (!Manager.isScheduleApp()) {
				sched.setText("Real Time Predictions");
				sched.setTextColor(0xFF009900);
			} else {
				sched.setText("Scheduled Arrivals");
				sched.setTextColor(0xFFE0B000);
			} 
			
			sched.setVisibility(View.GONE);
			
			label.setText(arr.get(position).intersection);
			//content.setText("Direction: " + Utils.fmt(arr.get(position).dirName));
			if(!Manager.isWMATA())
				content.setText("Direction: " + Utils.fmtHeadsign(Utils.checkHeadsign(arr.get(position).headSign)));
			
			ImageView icon=(ImageView)row.findViewById(R.id.icon);
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
					
					if(Manager.isWMATA()) WMATATransitDataManager.fetchAgencyRouteList();
					else Manager.loadAgencyRoutes(a);
				} catch (ServerBarfException e) {
					errorHandler.sendEmptyMessage(1); return;
				}
				pd.dismiss();
				stact.sendEmptyMessage(0);
			}
			else {
				try {
					if(Manager.isWMATA()) WMATATransitDataManager.fetchPredictionsByStop(arr.get(Manager.positionTracker));
					else Manager.loadStop(arr.get(Manager.positionTracker));
				} catch (NoneFoundException e) {
					errorHandler.sendEmptyMessage(0); return;
				} catch (ServerBarfException e) {
					errorHandler.sendEmptyMessage(1); return;
				}
				
				pd.dismiss();
				stact.sendEmptyMessage(0);
			}
		}
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
        	DataThread d = new DataThread();
        	if (Manager.viewing==Manager.AGENCY || Manager.viewing==Manager.FAVROUTES) {
        		pd = ProgressDialog.show(me, "Getting Stop", "Contacting the Server:\nRefreshing Predictions", true, false);
        	}
        	else pd = ProgressDialog.show(me, "Getting Stop", "Contacting the Server:\nRefreshing Predictions", true, false);
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
			if (Manager.viewing == Manager.AGENCY) {
				Intent i = new Intent(me, AgencyRouteList.class);
				startActivityForResult(i,0);
			}
			else {
				Intent i = new Intent(me, StopDrill.class);
				startActivityForResult(i,0);
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
	
	/**
	 * This Handler is a receiver for error messages, from other parts of the FavStops code. In particular, if there is an error where either
	 *  the user's Favorite stop can't be retrieved from the server, or these is a general server error, errorHandler is expected to receive
	 *  and deal with this error appropriately.  
	 */
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
	
	/** This method gets called by the errorHandler, in the even that the user requests predictions for a stop that the server does not recognize. 
	 * The user is notified of this, and asked if they want to get rid of the stop. If so, the stop is un-favorited, and also made invisible. If not,
	 *  the stop is left in the FavStops list, but it is greyed out, and it's on-click listener will be removed.
	 */
	private void showError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Stop Predictions")
        .setIcon(R.drawable.ico)
        .setMessage("It seems that there is an error requesting your stop. " +
        		"It may no longer exist! Do you want to remove it from your favorites?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	//Since user clicked yes
            	Log.v("FavStops", "Removing stop from Favorites!");
            	//Remove the currently selected stop from Favorites, and uncheck it too
            	uncheckStopPosition(Manager.positionTracker);
            	Favorites.getInstance().removeStop(arr.get(Manager.positionTracker));
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Communicate that "no" was clicked
            	Log.v("FavStops", "NOT removing stop from Favorites!");
            }
        });
        
		//Ivany: should probably add a flurry event thing here, for stopNotFoundError or something
        Manager.flurryStopNotFoundEvent(activityNameTag, getString(R.string.agencyName), 
        		arr.get(Manager.positionTracker).routeName, arr.get(Manager.positionTracker).intersection);
        
        b.show();
        
        //Grey out the List item at the position that the user just clicked
        greyOutStopPosition(Manager.positionTracker);
        Log.v(activityNameTag, "Should have greyed out position " + Manager.positionTracker);
        
        //Un-fade out the rest of the stops
        this.undoListItemClick(this.getListView(), Manager.positionTracker);
	}

	/** Overrides the default onDestroy. Special things we do in AgencyRouteList.onDestroy():
	 * 1. Nothing.
	 * 
	 * That's it */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	/**
	 * This method greys out the list item at position i. It is used by showError() to grey out stops that could not be found on the AnyStop server. 
	 * This display modification does not persist across FavStops sessions, it is not saved in any way.
	 * @param i The position of the SimpleStop, in our list, to grey out.
	 */
	private void greyOutStopPosition(int i) {
		ListView myLV = this.getListView();
		
		if(myLV.getChildCount() > i) {
			View row = myLV.getChildAt(i); //get the List item at position i
			row.setBackgroundColor(0xFFCCCCCC); //set background to light grey
			
			//Set the text color to a slightly darker grey
			TextView childTV = (TextView) row.findViewById(R.id.dir_label);
			childTV.setTextColor(0xFF999999);
			childTV = (TextView) row.findViewById(R.id.dir_sched);
			childTV.setTextColor(0xFF999999);
			childTV = (TextView) row.findViewById(R.id.dir_subinfo);
			childTV.setTextColor(0xFF999999);
			
			//Remove the onClickListener for that childview
			row.setOnClickListener(null);
			
			Log.v(activityNameTag, "Ivany: Setting background color of child #" + i + " to grey.");
		}
	}
	
	/**This method gets called when arr is empty, that is, there are no favorite stops to display. 
	 * It will bring up a dialog that will indicate this to the user, and give them the option of either doing nothing
	 *  (i.e. the "OK" button) or going to AgencyRouteList to view Routes and Stops for this Agency.
	 */
	private void showNoStopsError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("No Favorite Stops Set")
        .setIcon(R.drawable.ico)
        .setMessage("Add a favorite stop for quick access by tapping the heart by any stop.")
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
	
	/**
	 * This method un-checks the "favorited" heart for the List Item in position i
	 * @param i The position to un-check
	 */
	private void uncheckStopPosition(int i) {
		ListView myLV = this.getListView();
		
		if(myLV.getChildCount() > i) {
			View row = myLV.getChildAt(i); //get the List item at position i
			
			IndexCheck favHeart = (IndexCheck) row.findViewById(R.id.favbox);
			favHeart.setChecked(false);
			
			Log.v(activityNameTag, "Ivany: Unchecking the favorite heard of child #" + i);
		}
	}
}
