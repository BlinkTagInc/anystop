/** This class is used to perform calls to fetch transit data and predictions for WMATA rail and bus 
 * routes. It calls static methods in WMATATransitDataFetcher to get this data, and stores the results
 * (and a stack of the queries that have been performed). Storing state allows us to re-do previous
 * queries to for example refresh data that we have fetched.
 * 
 * The comments in this class are pretty spare, looking in the TransitDataFetcher class may be
 * more informative.
 * 
 * Written I. Yulaev 2013-01-(01-18)
 */

package org.busbrothers.anystop.agencytoken;

import java.util.ArrayList;
import java.util.HashMap;

import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;

import android.location.Location;
import android.util.Log;

public class WMATATransitDataManager {
	private static boolean has_been_reset = false;
	private static ArrayList<Integer> lastCommandStack;
	private static ArrayList<Object> lastFetchStack; //Store objects we use TO get results from WMATA server
	private static ArrayList<Object> lastResultStack; //Store results we get from the WMATA server
	
	private static final int COMMAND_FETCH_ROUTES = 0;
	private static final int COMMAND_FETCH_STOPS_BY_ROUTE = 1;
	private static final int COMMAND_FETCH_PREDICTIONS_BY_STOP = 2;
	private static final int COMMAND_FETCH_NEAREST_ROUTES = 3;
	private static final int COMMAND_FETCH_PREDICTIONS_MULTISTOP = 4;
	private static final int COMMAND_FETCH_NEAREST_STOPS = 5;
	
	private static final String activitynametag = "WMATATransitDataManager";
	
	/** Reset all command state and data that we have fetched. Also clear the TransitDataFetcher
	 * cache.
	 */
	public static void reset() {
		lastCommandStack = new ArrayList<Integer>();
		lastFetchStack = new ArrayList<Object>();
		lastResultStack =new ArrayList<Object>(); 
		has_been_reset = true;
		
		WMATATransitDataFetcher.reset();
	}
	
	/** Fetch a list of Routes for the agency */
	public static void fetchAgencyRouteList() throws ServerBarfException {
		if(!has_been_reset) return;
		
		try {
			ArrayList<Route> routeList = WMATATransitDataFetcher.fetchAgencyRouteList();
			
			if(routeList != null) {
				Log.d(activitynametag, "Got some shit!");
				lastCommandStack.add(COMMAND_FETCH_ROUTES);
				lastResultStack.add(routeList);
				lastFetchStack.add(new Object()); //add dummy object to lastFetchStack
			} else {
				Log.d(activitynametag, "Didn't get shit!");
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			Log.d(activitynametag, "bananas: " + e);
			throw new ServerBarfException();
		}
	}
	
	/** Fetch a list of stops for a given Route */
	public static void fetchStopsByRoute(Route r) throws ServerBarfException {
		if(!has_been_reset) return;
		
		if(lastCommandStack.get(lastCommandStack.size()-1) != COMMAND_FETCH_ROUTES) {
			Log.d(activitynametag, "Error for fetchStopsByRoute() - did not detect that last command fetched a route!");
			throw new ServerBarfException();
		}
		
		try {
			ArrayList<SimpleStop> stopList = WMATATransitDataFetcher.fetchStopsByRoute(r);
			
			if(stopList != null) {
				lastCommandStack.add(COMMAND_FETCH_STOPS_BY_ROUTE);
				lastResultStack.add(stopList);
				lastFetchStack.add(r);
			} else {
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			throw new ServerBarfException();
		}
	}
	
	/** Get some predictions for a given SimpleStop */
	public static void fetchPredictionsByStop(SimpleStop s) throws ServerBarfException {
		if(!has_been_reset) return;
		
		//This isn't the right condition to check anyway since we can be fetching a stop from FavStops that was saved ages ago
		/*if(lastCommandStack.get(lastCommandStack.size()-1) != COMMAND_FETCH_STOPS_BY_ROUTE) {
			Log.d("Error for fetchPredictionsByStop() - did not detect that last command fetched a route!");
			throw new ServerBarfException();
		}*/
		
		try {
			ArrayList<SimpleStop> predStopsList = WMATATransitDataFetcher.fetchPredictionsByStop(s);
			
			if(predStopsList != null) {
				lastCommandStack.add(COMMAND_FETCH_PREDICTIONS_BY_STOP);
				lastFetchStack.add(s);
				lastResultStack.add(predStopsList);
			} else {
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			throw new ServerBarfException();
		}
	}
	
	/** Get some predictions for a given ArrayList SimpleStop */
	public static void fetchPredictionsByStops(ArrayList<SimpleStop> stops) throws ServerBarfException {
		if(!has_been_reset) return;
		
		try {
			ArrayList<SimpleStop> predStopsList = WMATATransitDataFetcher.fetchPredictionsByStops(stops);
			
			if(predStopsList != null) {
				lastCommandStack.add(COMMAND_FETCH_PREDICTIONS_MULTISTOP);
				lastFetchStack.add(stops);
				lastResultStack.add(predStopsList);
			} else {
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			throw new ServerBarfException();
		}
	}
	
	/** Given a Location for the requestor, fetch some nearby Routes */
	public static void fetchRoutesByLocation(Location loc) throws ServerBarfException {
		if(!has_been_reset) return;
		
		try {
			HashMap<Route, ArrayList<SimpleStop>> nearestRouteMap = 
					WMATATransitDataFetcher.getNearestRoutes(loc.getLatitude(), loc.getLongitude());
			
			if(nearestRouteMap != null) {
				lastCommandStack.add(COMMAND_FETCH_NEAREST_ROUTES);
				lastFetchStack.add(loc);
				lastResultStack.add(nearestRouteMap);
			} else {
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			System.err.println(e);
			throw new ServerBarfException();
		}
	}
	
	/** Given a Location for the requestor, fetch some nearby Stops */
	public static void fetchStopsByLocation(Location loc) throws ServerBarfException {
		if(!has_been_reset) return;
		
		try {
			ArrayList<SimpleStop> nearestStops = 
					WMATATransitDataFetcher.getNearestStops(loc.getLatitude(), loc.getLongitude());
			
			if(nearestStops != null) {
				lastCommandStack.add(COMMAND_FETCH_NEAREST_STOPS);
				lastFetchStack.add(loc);
				lastResultStack.add(nearestStops);
			} else {
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			System.err.println(e);
			throw new ServerBarfException();
		}
	}
	
	/** Re-do the last query; this is used to "refresh" the data fetched for the previous query */
	public static void repeat() throws ServerBarfException {
		if(!has_been_reset) return;
		
		if(lastCommandStack.size() == 0 || lastFetchStack.size() == 0) {
			Log.w(activitynametag, "Error doing repeat - last command was null!");
		}
		
		int last_command = lastCommandStack.get(lastCommandStack.size() - 1);
		
		Object lastResult = null;
		if(lastResultStack.size() > 0)
			lastResult = lastResultStack.get(lastResultStack.size() - 1);
		
		Object lastFetch;
		lastFetch = lastFetchStack.get(lastFetchStack.size() - 1);
		
		try {
			if(last_command == COMMAND_FETCH_ROUTES) {
				popCommand();
				fetchAgencyRouteList();
			} else if (last_command == COMMAND_FETCH_STOPS_BY_ROUTE) {
				Route r = (Route) lastFetchStack.get(lastFetchStack.size() - 1);
				popCommand();
				fetchStopsByRoute(r);
			} else if (last_command == COMMAND_FETCH_PREDICTIONS_BY_STOP) {
				SimpleStop r = (SimpleStop) lastFetchStack.get(lastFetchStack.size() - 1);
				popCommand();
				fetchPredictionsByStop(r);
			} else if(last_command == COMMAND_FETCH_NEAREST_ROUTES) {
				Location l = (Location) lastFetchStack.get(lastFetchStack.size() - 1);
				popCommand();
				fetchRoutesByLocation(l);
			} else if(last_command == COMMAND_FETCH_PREDICTIONS_MULTISTOP) {
				ArrayList<SimpleStop> stops = (ArrayList<SimpleStop>) lastFetchStack.get(lastFetchStack.size() - 1);
				popCommand();
				fetchPredictionsByStops(stops);
			} else if(last_command == COMMAND_FETCH_NEAREST_STOPS) {
				Location l = (Location) lastFetchStack.get(lastFetchStack.size() - 1);
				popCommand();
				fetchStopsByLocation(l);
			}
		//If we get an exception, throw it, but restore the state of the stacks since
		//it might just be a time-out or something
		} catch (ServerBarfException e) {
			lastCommandStack.add(last_command);
			lastResultStack.add(lastResult);
			lastFetchStack.add(lastFetch);
			throw e;
		}
	}
	
	/** Remove a command, the query data, and the result from our command stacks */
	public static void popCommand() {
		if(!has_been_reset) return;
		
		if(lastCommandStack.size() > 0)
			lastCommandStack.remove(lastCommandStack.size()-1);
		if(lastFetchStack.size() > 0)
			lastFetchStack.remove(lastFetchStack.size()-1);
		if(lastResultStack.size() > 0)
			lastResultStack.remove(lastResultStack.size()-1);
	}
	
	/** Peek at the last data that we fetched. Lots of classes use this method to actually access the 
	 * data that the WMATATransitDataManager fetches. You'll have to know what command was done to cast
	 * the result though.
	 * @return An Object representing the last data that the WMATA TDM fetched.
	 */
	public static Object peekLastData() {
		if(!has_been_reset) return null;
		
		if(lastResultStack.size() > 0)
			return(lastResultStack.get(lastFetchStack.size()-1));
		else return null;
	}
}
