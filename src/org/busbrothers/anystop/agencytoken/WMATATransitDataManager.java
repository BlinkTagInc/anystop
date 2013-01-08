package org.busbrothers.anystop.agencytoken;

import java.util.ArrayList;

import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;

public class WMATATransitDataManager {
	private static ArrayList<Integer> lastCommandStack;
	private static ArrayList<Object> lastFetchStack; //Store objects we use TO get results from WMATA server
	private static ArrayList<Object> lastResultStack; //Store results we get from the WMATA server
	
	private static final int COMMAND_FETCH_ROUTES = 0;
	private static final int COMMAND_FETCH_STOPS_BY_ROUTE = 1;
	private static final int COMMAND_FETCH_PREDICTIONS_BY_STOP = 2;
	
	public static void reset() {
		lastCommandStack = new ArrayList<Integer>();
		lastFetchStack = new ArrayList<Object>();
		lastResultStack =new ArrayList<Object>(); 
	}
	
	public static void fetchAgencyRouteList() throws ServerBarfException {
		try {
			ArrayList<Route> routeList = WMATATransitDataFetcher.fetchAgencyRouteList();
			
			if(routeList != null) {
				System.err.println("Got some shit!");
				lastCommandStack.add(COMMAND_FETCH_ROUTES);
				lastResultStack.add(routeList);
				lastFetchStack.add(new Object()); //add dummy object to lastFetchStack
			} else {
				System.err.println("Didn't get shit!");
				throw new ServerBarfException();
			}
		} catch (Exception e) {
			System.err.println("bananas: " + e);
			throw new ServerBarfException();
		}
	}
	
	public static void fetchStopsByRoute(Route r) throws ServerBarfException {
		if(lastCommandStack.get(lastCommandStack.size()-1) != COMMAND_FETCH_ROUTES) {
			System.err.println("Error for fetchStopsByRoute() - did not detect that last command fetched a route!");
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
	
	public static void fetchPredictionsByStop(SimpleStop s) throws ServerBarfException {
		//TODO: Maybe the conditions here will have to change...
		if(lastCommandStack.get(lastCommandStack.size()-1) != COMMAND_FETCH_STOPS_BY_ROUTE) {
			System.err.println("Error for fetchPredictionsByStop() - did not detect that last command fetched a route!");
			throw new ServerBarfException();
		}
		
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
	
	public static void popCommand() {
		if(lastCommandStack.size() > 0)
			lastCommandStack.remove(lastCommandStack.size()-1);
		if(lastFetchStack.size() > 0)
			lastFetchStack.remove(lastFetchStack.size()-1);
		if(lastResultStack.size() > 0)
			lastResultStack.remove(lastResultStack.size()-1);
	}
	
	public static Object peekLastData() {
		if(lastResultStack.size() > 0)
			return(lastResultStack.get(lastFetchStack.size()-1));
		else return null;
	}
}
