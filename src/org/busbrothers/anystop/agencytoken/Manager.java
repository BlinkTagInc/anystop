package org.busbrothers.anystop.agencytoken;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.datacomponents.Agency;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.map.GeoUtils;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.ads.AdRequest;
import com.sensedk.AswAdLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import android.os.Debug; //for printMemoryUsageInfo()

public class Manager {
	private static String currlat;
	private static String currlon;
	private static String currinter;
	
	//The below class variables represent the current Activity being viewed by the user
	//Sometimes it is useful to have global (!!!) access to this information
	public static final int STOPS = 0;
	public static final int ROUTES = 1;
	public static final int AGENCY = 2;
	public static final int FAVROUTES = 3;
	public static final int FAVSTOPS = 4;
	public static final int STOP = 5;
	public static final int ROUTE = 6;
	public static final int CHOOSEMAP = 7;
	
	//Kind of like an enum for advertisement types
	public static final int ADMOB_AD = 0;
	public static final int ADDIENCE_AD = 1;
	public static final int ADWHIRL_AD = 2;
	
	public static final int LISTWINDOW_START_FONTSIZE = 26;
	public static final int LISTWINDOW_MIN_FONTSIZE = 18;
	public static final int LISTWINDOW_MAX_NUMLINES = 3;
	public static final int LISTWINDOW_MAX_HEIGHT = LISTWINDOW_START_FONTSIZE*LISTWINDOW_MAX_NUMLINES;
	
	public static int viewing;
	public static int positionTracker;
	public static String stringTracker;
	public static String routeTracker;
	public static String messageTracker;
	public static Agency currAgency;
	public static SimpleStop stopTracker;
	public static Location loctracker;
	public static Location currloc; //!<Used by LocationGetter class
	
	public static int lat, lon;
	
	private static String name;
	public static ArrayList<Agency> agencies;
	public static ArrayList<Route> agencyRoutes;
	public static ArrayList<SimpleStop> stops;
	public static ArrayList<SimpleStop> favstops;
	public static HashMap<String, ArrayList<SimpleStop>> stopMap;
	public static HashMap<String, ArrayList<SimpleStop>> routeMap;
	public static String tableTracker;
	
	public static final int LNS=1, LAR=2, LS=3, LAS=4, LASP=5;
	private static String _agencyTag = null;
	private static String _agencyName = null;
	private static String _agencyTableName = null;
	private static String _agencyShortName = null;
	private static String _agencyLongName = null;
	private static boolean _hybridAgency = false;
	private static String _agencyScheduleTableName = null;
	private static String _appName = null;
	private static String _flurryAPIK = null;
	private static String _predictionType = null;
	private static String _settingURL = null;
	
	private static String _adsAPIK = null;
	private static String _adwhirlAPIK = null;
	
	private static Object param1, param2, param3;
	public static int tracker;
	public static String currError;
	
	public static Stack<String> messageQueue = new Stack<String>();
	
	private static String lastRequest;
	
	private static final String activityNameTag = "Manager";//!<Represents the "Tag" for this Activity; used for LogCat printing and Flurry Analytics Reporting
	private static Typeface defaultTypeface = null;//!<Typeface that will be applied to all TextViews (including buttons) in this App
	
	
	public static void loadNearStops(Location loc, String tableName) throws NoneFoundException, ServerBarfException {
		try {
			Manager.inflateXML(Manager.getStopsXML(loc, tableName));
			tracker = LNS;
			param1 = loc;
			param2 = tableName;
		} catch (NoneFoundException nf) {
			throw nf;
		} catch (Exception ex) {
			try {
				Manager.inflateXML(Manager.getStopsXML(loc, tableName));
				tracker = LNS;
				param1 = loc;
				param2 = tableName;
			}catch (NoneFoundException nf) {
				flurryNearbyNotFoundEvent(loc, tableName);
				throw nf;
			} catch (Exception ex2) {
				Manager.flurryServerErrorEvent(Manager.lastRequest);
				throw new ServerBarfException();
			}
		}
	}

	public static void loadAgencyRoutes(Agency a) throws ServerBarfException {
		try {
			
			Manager.inflateAgencyRouteXML(Manager.getRouteXML(a));
			tracker = LAR;
			param1 = a;
		} catch (Exception ex) {
			try {
			Manager.inflateAgencyRouteXML(Manager.getRouteXML(a));
			tracker = LAR;
			param1 = a;
			} catch (Exception ex2) {
				Manager.flurryServerErrorEvent(Manager.lastRequest);
				throw new ServerBarfException();
			}
		}
	}

	public static void loadStop(SimpleStop stop) throws NoneFoundException, ServerBarfException {
		try {
			Manager.inflateXML(Manager.getStopXML(stop));
			tracker = LS;
			param1 = stop;
		} catch (NoneFoundException nf) {
			throw nf;
		} catch (Exception ex) {
			try {
			Manager.inflateXML(Manager.getStopXML(stop));
			tracker = LS;
			param1 = stop;
			}catch (NoneFoundException nf) {
				Manager.flurryNoneFoundEvent(stop.agency, stop.routeName, stop.intersection);
				throw nf;
			} catch (Exception ex2) {
				Manager.flurryServerErrorEvent(Manager.lastRequest);
				throw new ServerBarfException();
			}
		}
	}

	public static void loadAgencyStop(Agency a, String route)
			throws NoneFoundException, ServerBarfException {
		try {
			Log.v(activityNameTag,"Trying to fetch data for route=\"" + route + "\" from primary table.");
			Manager.inflateXML(Manager.getRouteStopsXML(a, route));
			tracker = LAS;
			param1 = a;
			param2 = route;
		} catch (NoneFoundException nf) {
			if(!Manager.isHybridAgency()) {
				Manager.flurryNoneFoundEvent(a, route);
				throw nf;
			}
			else {
				try {
					Log.v(activityNameTag,"Trying to fetch data for route=\"" + route + "\" from secondary table.");
					Manager.inflateXML(Manager.getRouteStopsXML(a, route, true));
				} catch (NoneFoundException nf2) {
					Manager.flurryNoneFoundEvent(a, route);
					throw nf2;
				} catch (Exception ex3) {
					Manager.flurryServerErrorEvent(Manager.lastRequest);
					throw new ServerBarfException();
				}
			}
		} catch (Exception ex) {
			try {
			Manager.inflateXML(Manager.getRouteStopsXML(a, route));
			tracker = LAS;
			param1 = a;
			param2 = route;
			} catch (NoneFoundException nf) {
				if(!Manager.isHybridAgency()) {
					Manager.flurryNoneFoundEvent(a, route);
					throw nf;
				}
				else {
					try {
						Manager.inflateXML(Manager.getRouteStopsXML(a, route, true));
					} catch (NoneFoundException nf3) {
						Manager.flurryNoneFoundEvent(a, route);
						throw nf3;
					} catch (Exception ex4) {
						Manager.flurryServerErrorEvent(Manager.lastRequest);
						throw new ServerBarfException();
					}
				}
			}catch (Exception ex2) {
				Manager.flurryServerErrorEvent(Manager.lastRequest);
				throw new ServerBarfException();
			}
		}
	}

	public static void loadAgencyStopPred(Agency a, String route,
			String intersection) throws NoneFoundException, ServerBarfException {
		try {
			Log.v(activityNameTag,"Trying to fetch data for route=\"" + route + "\", stop=\"" + intersection + "\" from primary table.");
			Manager.inflateXML(Manager.getRouteStopsPredsXML(a, route,
					intersection));
			tracker = LASP;
			param1 = a;
			param2 = route;
			param3 = intersection;
		} catch (NoneFoundException nf) {
			if(!Manager.isHybridAgency()) {
				Manager.flurryNoneFoundEvent(a, route, intersection);
				throw nf;
			}
			else {
				try {
					Log.v(activityNameTag,"Trying to fetch data for route=\"" + route + "\", stop=\"" + intersection + "\" from secondary table.");
					Manager.inflateXML(Manager.getRouteStopsPredsXML(a, route, intersection, true));
				} catch (NoneFoundException nf2) {
					Manager.flurryNoneFoundEvent(a, route, intersection);
					throw nf2;
				} catch (Exception ex3) {
					Manager.flurryServerErrorEvent(Manager.lastRequest);
					throw new ServerBarfException();
				}
			}
		} catch (Exception ex) {
			try {
			Manager.inflateXML(Manager.getRouteStopsPredsXML(a, route,
					intersection));
			tracker = LASP;
			param1 = a;
			param2 = route;
			param3 = intersection;
			} catch (NoneFoundException nf) {
				if(!Manager.isHybridAgency()) {
					Manager.flurryNoneFoundEvent(a, route, intersection);
					throw nf;
				}
				else {
					try {
						Log.v(activityNameTag,"Trying to fetch data for route=\"" + route + "\" from secondary table.");
						Manager.inflateXML(Manager.getRouteStopsPredsXML(a, route, intersection, true));
					} catch (NoneFoundException nf3) {
						Manager.flurryNoneFoundEvent(a, route, intersection);
						throw nf3;
					} catch (Exception ex4) {
						Manager.flurryServerErrorEvent(Manager.lastRequest);
						throw new ServerBarfException();
					}
				}
			}catch (Exception ex2) {
				Manager.flurryServerErrorEvent(Manager.lastRequest);
				throw new ServerBarfException();
			}
		}
	}
		
	public static void repeat() throws NoneFoundException {
		
		switch (tracker) {
		case LNS:
			try {
				Manager.inflateXML(Manager.getStopsXML((Location) param1,(String) param2));
			} catch (NoneFoundException e) {
				Manager.flurryNearbyNotFoundEvent((Location) param1,(String) param2);
				throw e;
			}
			break;

		case LAR:
			Manager.inflateAgencyRouteXML(Manager.getRouteXML((Agency) param1));
			break;

		case LS:
			try {
				Manager.inflateXML(Manager.getStopXML((SimpleStop) param1));
			} catch (NoneFoundException e) {
				SimpleStop p1Stop = (SimpleStop) param1;
				Manager.flurryNoneFoundEvent(p1Stop.agency, p1Stop.routeName, p1Stop.intersection);
				throw e;
			}
			break;

		case LAS:
			try {
				Manager.inflateXML(Manager.getRouteStopsXML((Agency) param1, (String) param2));
			} catch (NoneFoundException e) {
				if(!Manager.isHybridAgency()) {
					Manager.flurryNoneFoundEvent((Agency) param1, (String) param2);
					throw e;
				}
				else {
					try {
						Manager.inflateXML(Manager.getRouteStopsXML((Agency) param1, (String) param2, true));
					} catch (NoneFoundException nf2) {
						Manager.flurryNoneFoundEvent((Agency) param1, (String) param2);
						throw nf2;
					} 
				}
			}
			break;
			
		case LASP:
			try {
				Manager.inflateXML(Manager.getRouteStopsPredsXML((Agency) param1, (String) param2, (String) param3));
			} catch (NoneFoundException e) {
				if(!Manager.isHybridAgency()) {
					Manager.flurryNoneFoundEvent((Agency) param1, (String) param2, (String) param3);
					throw e;
				}
				else {
					try {
						Manager.inflateXML(Manager.getRouteStopsPredsXML((Agency) param1, (String) param2, (String) param3, true));
					} catch (NoneFoundException nf2) {
						Manager.flurryNoneFoundEvent((Agency) param1, (String) param2, (String) param3);
						throw nf2;
					} 
				}
			}
			break;
		}
	}
	
	private static void inflateXML(String xml) throws NoneFoundException {
		stops = null;
		stops = MakeStops.make(xml); //stops does not appear to be used anwhere else
		stopMap = new HashMap<String, ArrayList<SimpleStop>>();
		routeMap = new HashMap<String, ArrayList<SimpleStop>>();
		
		HashMap<String, String> strippedToRealRouteNameMap = new HashMap<String, String>();
		
		for (SimpleStop s : stops) {
			String intersection = s.intersection;
			String route = s.routeName;
			
			if (stopMap.containsKey(intersection)) {
				ArrayList<SimpleStop> storedArr = stopMap.get(intersection);
				
				//We need to remove duplicate routes from storedArr, if they are schedule-based and we are real-time
				boolean foundDuplicateRouteName = false;
				Iterator <SimpleStop> storedArrIterator = storedArr.iterator();
				while(storedArrIterator.hasNext() && !foundDuplicateRouteName) {
					SimpleStop currStop = storedArrIterator.next();
					//We found a SimpleStop in storedArr that has an equivalent route name
					if(Utils.routeStripTrailing(s.routeName).equals(Utils.routeStripTrailing(currStop.routeName))) {
						foundDuplicateRouteName = true;
						
						//If the currently stored SimpleStop is real-time, keep it & don't add a new simple stop
						if(currStop.isRTstr.equals("real"));
						//If the currenly stored SimpleStop is NOT real time, replace it
						else if(!currStop.isRTstr.equals("real")) {
							Log.v(activityNameTag,"Replacing stored stop with route " + currStop.routeName + 
									" with an equivalent one with route name " + s.routeName);
							
							storedArr.remove(currStop);
							storedArr.add(s);
							
						}
					}
				}
				
				//If we haven't added this route yet at all, then put it in of course!
				if(!foundDuplicateRouteName)
					stopMap.get(intersection).add(s);
			} else {
				ArrayList<SimpleStop> arr = new ArrayList<SimpleStop>(2);
				arr.add(s);
				stopMap.put(intersection, arr);
			}
			
			/* We write the below algorithm to remove duplicate real-time/schedule-based route from our list
			   Algorithm is:
			(1) Check if stripped route name is in sTRRNM
			(2) If no, add to sTRRNM and to routeMap
			(3) If yes, fetch by real route name
				(a) If stored stop is real-time and route is real-time, and route names match, add to list
					If they don't match, throw it out
				(b) If stored stop is not real-time, and route is not real-time, and route names match, add to list
					If they don't match, throw it out
				(c) If stored stop is real-time, and route is not, do nothing
				(d) If stored stop is not real time, but route is, throw out old route, and begin anew
			*/
			String strippedRouteName = Utils.routeStripTrailing(route);
			
			if(!strippedToRealRouteNameMap.containsKey(strippedRouteName)) {
				ArrayList<SimpleStop> arr = new ArrayList<SimpleStop>(2);
				arr.add(s);
				routeMap.put(route, arr);
				
				strippedToRealRouteNameMap.put(strippedRouteName, route);
			}
			else {
				String storedRouteName = strippedToRealRouteNameMap.get(strippedRouteName);
				ArrayList<SimpleStop> storedArr = routeMap.get(storedRouteName);
				
				if(storedArr == null) {
					ArrayList<SimpleStop> arr = new ArrayList<SimpleStop>(2);
					arr.add(s);
					routeMap.put(route, arr);
					strippedToRealRouteNameMap.put(strippedRouteName, route);
				}
				else {
					SimpleStop first = storedArr.get(0);
					if(first.isRTstr.equals("real") && s.isRTstr.equals("real")) {
						if(route.equals(storedRouteName)) storedArr.add(s);
					}
					else if(!first.isRTstr.equals("real") && !s.isRTstr.equals("real")) {
						if(route.equals(storedRouteName)) storedArr.add(s);
					}
					else if(first.isRTstr.equals("real") && !s.isRTstr.equals("real")) {
						;
					}
					else if(!first.isRTstr.equals("real") && s.isRTstr.equals("real")) {
						routeMap.remove(storedRouteName);
						Log.v(activityNameTag, "Removing route " + storedRouteName + " in favor of " + route);
						
						ArrayList<SimpleStop> arr = new ArrayList<SimpleStop>(2);
						arr.add(s);
						routeMap.put(route, arr);
						strippedToRealRouteNameMap.put(strippedRouteName, route);
					}
				}
			}
		}
		return;
	}

	public static void clearStops() {
		stops = null;
	}

	/*private static String getAgenciesXML(Location loc) {
		InputStream is;
    	DataInputStream dis;
    	StringBuffer b = new StringBuffer();
    	String s;
    	String rq = "http://feed.busbrothers.org/ClosestStops/";
    	if (loc!=null) {
    		rq=rq+"XMLClosestAgencies.jsp?&latitude=" + loc.getLatitude() + "&longitude=" + loc.getLongitude();
    	} else {
    		rq=rq+"XMLAgencyList.jsp";
    	}
    	System.out.println("outgoing request: " + rq);
    	Manager.lastRequest = rq;
		try {	
			URL u = new URL(rq);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return b.toString();
	}*/
	
	private static String getRouteXML(Agency a) {
		InputStream is;
    	DataInputStream dis;
    	StringBuffer b = new StringBuffer();
    	String s;
    	String rq;
    	
    	/* For hybrid agencies, the URL we use is slightly different; we must provide both the "main" real-time and
    	 * also a schedule-based table name, and use "scheduleReal" as the prediction type
    	 */
    	if(Manager.isHybridAgency()) {
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLRouteList.jsp?" +
			"&agencyName="+URLEncoder.encode(a.name) +
			"&predictionType="+ URLEncoder.encode("scheduleReal") +
			"&tableName="+ URLEncoder.encode(Manager.getScheduleTableName()) + 
			"&tableName2="+ URLEncoder.encode(a.table);
    	}
    	else {
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLRouteList.jsp?" +
    			"&agencyName="+URLEncoder.encode(a.name) +
    			"&predictionType="+ URLEncoder.encode(a.isRTstr) +
    			"&tableName="+ URLEncoder.encode(a.table);
    	}
    	
    	System.out.println(rq);
    	Manager.lastRequest = rq;
    	
    	//Track loading of raw XML from feed server
    	Log.v(activityNameTag, "Manager is getting route XML from URL=" + rq);
    	long startLoadTime = System.currentTimeMillis(); 
    	
		try {	
			URL u = new URL(rq);
			//is = u.openStream();
			//dis = new DataInputStream(new BufferedInputStream(is));
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			while ((s = in.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long endLoadTime = System.currentTimeMillis();
		Log.v(activityNameTag, "AgencyRoute loading took " + (endLoadTime-startLoadTime)/1000.0 + " seconds to load raw XML.");
		
		//Now the brilliant thing is that if this is a hybrid app, we should strip out the XML bit that closes the first list of
		//routes and opens to second bit. Basically we replace "</routeList><routeList>" with blank
		if(Manager.isHybridAgency()) {
			String returned = b.toString();
			returned = returned.replaceFirst("</routeList><routeList>", "");
			//Log.v("Doing replacement!");
			return(returned);
		}
		else return b.toString();
	}
	
	/**This method gets a list of stops for a particular route. 
	 * 
	 * @param a The Agency that we are performing a query on.
	 * @param route The name of the route that we should fetch a list of stops for\r.
	 * @return String representing the XML that desribes the list of stops for route.
	 */
	private static String getRouteStopsXML(Agency a, String route) {
		return(getRouteStopsXML(a, route, false));
	}
	
	//useAlternateTable added to allow fetching from the schedule table
	private static String getRouteStopsXML(Agency a, String route, boolean useAlternateTable) {
		InputStream is;
    	DataInputStream dis;
    	StringBuffer b = new StringBuffer();
    	String s;
    	String rq;
    	
    	//THis "hack" is added in because as of 2012-04-22, the trimet XMLStopList.jsp page wants only the route number,
    	//not the route name, as the query string
    	if(Manager.getAgencyName().equals("TriMet"))
    		route = Utils.getLeadingDigits(route);
    	
    	if(useAlternateTable) {
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLStopList.jsp?" +
    			"&agencyName="+ URLEncoder.encode(a.name) +
    			"&predictionType="+ URLEncoder.encode("schedule") +
    			"&tableName="+ URLEncoder.encode(Manager.getScheduleTableName()) + 
    			"&route=" + URLEncoder.encode(route);
    	}
    	else {
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLStopList.jsp?" +
			"&agencyName="+ URLEncoder.encode(a.name) +
			"&predictionType="+ URLEncoder.encode(a.isRTstr) +
			"&tableName="+ URLEncoder.encode(a.table) + 
			"&route=" + URLEncoder.encode(route);
    	}
    	System.out.println(rq);
    	Manager.lastRequest = rq;
    	
    	Log.v("Manager", "Getting Stop List using URL=" + rq);
    	
		try {	
			URL u = new URL(rq);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return b.toString();
	}
	
	private static String getStopsXML(Location loc, String string) {
		loctracker=loc;
		InputStream is;
    	DataInputStream dis;
    	StringBuffer b = new StringBuffer();
    	String s;
    	String rq;
    	
    	if(Manager.isHybridAgency())
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLClosestPredictions2.jsp?" +
    			"&latitude=" + loc.getLatitude()*10 +
    			"&longitude=" + loc.getLongitude()*10 +
    			"&agency="+Manager.getScheduleTableName() + 
    			"&agency2="+string;
    	else 
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLClosestPredictions2.jsp?" +
				"&latitude=" + loc.getLatitude()*10 +
				"&longitude=" + loc.getLongitude()*10 +
				"&agency="+string;
    	
    	Log.v(activityNameTag, "Getting closest stops using URL=" + rq);
    	
    	System.out.println(rq);
    	Manager.lastRequest = rq;
		try {
			URL u = new URL(rq);
			//is = u.openStream();
			//dis = new DataInputStream(new BufferedInputStream(is));
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			while ((s = in.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return b.toString();
	}
	
	private static String getStopXML(SimpleStop stop) {
		InputStream is;
    	DataInputStream dis;
    	StringBuffer b = new StringBuffer();
    	String s;
    	String rq = "http://feed.busbrothers.org/ClosestStops/XMLIntersection.jsp?" +
    			"&agencyName="+ URLEncoder.encode(stop.agency) +
    			"&predictionType="+ URLEncoder.encode(stop.isRTstr) +
    			"&tableName="+ URLEncoder.encode(stop.table) + 
    			"&intersection=" + URLEncoder.encode(stop.intersection);
    	System.out.println(rq);
    	Manager.lastRequest = rq;
		try {	
			URL u = new URL(rq);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return b.toString();
	}
	
	private static String getRouteStopsPredsXML(Agency a, String route, String intersection) {
		return getRouteStopsPredsXML(a, route, intersection, false);
	}
	
	private static String getRouteStopsPredsXML(Agency a, String route,
			String intersection, boolean useAlternateTable) {
		InputStream is;
    	DataInputStream dis;
    	StringBuffer b = new StringBuffer();
    	String s;
    	String rq;
    	
    	//THis "hack" is added in because as of 2012-04-22, the trimet XMLStopList.jsp page wants only the route number,
    	//not the route name, as the query string
    	if(Manager.getAgencyName().equals("TriMet"))
    		route = Utils.getLeadingDigits(route);
    	
    	if(useAlternateTable) {
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLSingleStop.jsp?" +
			"&agencyName="+ URLEncoder.encode(a.name) +
			"&predictionType="+ URLEncoder.encode("schedule") +
			"&tableName="+ URLEncoder.encode(getScheduleTableName()) + 
			"&route=" + URLEncoder.encode(route) +
			"&intersection=" + URLEncoder.encode(intersection);
    	}
    	else {
    		rq = "http://feed.busbrothers.org/ClosestStops/XMLSingleStop.jsp?" +
			"&agencyName="+ URLEncoder.encode(a.name) +
			"&predictionType="+ URLEncoder.encode(a.isRTstr) +
			"&tableName="+ URLEncoder.encode(a.table) + 
			"&route=" + URLEncoder.encode(route) +
			"&intersection=" + URLEncoder.encode(intersection);
    	}
    	
    	Log.v(activityNameTag, "getRoutStopsPredsXML using URL=" + rq);
    	
    	System.out.println(rq);
    	Manager.lastRequest = rq;
		try {	
			URL u = new URL(rq);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return b.toString();
	}

	/* deprecated
	private static void inflateAgencyXML(String xml) throws NoneFoundException {
		agencies=MakeStops.makeAgencies(xml);
	}

	private static void clearAgencies() {
		agencies=null;
	}*/
	
	private static void inflateAgencyRouteXML(String xml) {
		long startMakeAgencyRouteTime = System.currentTimeMillis();
		
		if(!Manager.isHybridAgency()) {
			agencyRoutes = MakeStops.makeAgencyRoutes(xml);
		}
		//For Hybrid agencies, we get several different lists of routes. And we have to filter out duplicates such
		//that we always search for real-time data if possible!
		else {
			agencyRoutes = new ArrayList<Route>();
			//The below code filters out all duplicate Routes; we ensure that if we have both schedule
			//and real-time data for a route, we keep only the real time data
			//We also make sure to avoid "exceedingly similar" route names by first using 
			//Utils.routeStripTrailing() to harmonize route names
			ArrayList<Route> makeAgencyRoutesOutput = MakeStops.makeAgencyRoutes(xml);
			HashMap<String, Route> agencyRouteNameMap = new HashMap<String, Route>();
			Iterator <Route> makeAgencyRoutesOutputIterator = makeAgencyRoutesOutput.iterator();
			while(makeAgencyRoutesOutputIterator.hasNext()) {
				Route a = makeAgencyRoutesOutputIterator.next();
				String routeName = Utils.routeStripTrailing(a.lName);
				
				if(agencyRouteNameMap.containsKey(routeName)) {
					Route alreadyThere = agencyRouteNameMap.get(routeName);
					if(alreadyThere.isRT);
					else {
						agencyRouteNameMap.remove(routeName);
						agencyRouteNameMap.put(routeName, a);
					}
				}
				else agencyRouteNameMap.put(routeName, a);
			}
			
			//Finally, we will just extract ALL values out of the (duplicate-free!) agencyRouteNameMap and put them into agencyRoutes
			Collection<Route> nonDuplicateRoutes = agencyRouteNameMap.values();
			Iterator<Route> nonDuplicateRoutesIterator = nonDuplicateRoutes.iterator();
			while(nonDuplicateRoutesIterator.hasNext()) {
				Route a = nonDuplicateRoutesIterator.next();
				//Log.v("MakeStops", "Adding route \"" + a.lName + "\"");
				agencyRoutes.add(a);
			}
		}
		
		long endMakeAgencyRouteTime = System.currentTimeMillis();
		Log.d(activityNameTag, "Finished makeAgencyRoutes (parsing XML). XML string was " + xml.length()/1024.0 + " kbytes long. Took " + 
			(endMakeAgencyRouteTime-startMakeAgencyRouteTime)/1000.0 + " seconds.");
	}

	private static void clearAgencyRoutes() {
		agencyRoutes=null;
	}
	
	/** This function gets a Route object, by looking up a route by its sName in agencyRoutes
	 * It relies on agencyRoutes to have been initialized (otherwise we fail and return null).
	 * @param r_sName The sName of the route we're looking for.
	 * @return The Route object with that sName, or null if we couldn't find it.
	 */
	public static Route getRouteBysName(String r_sName) {
		Route returned = null;
		
		if(agencyRoutes != null) {
			Iterator<Route> aRIterator = agencyRoutes.iterator();
			
			while(aRIterator.hasNext()) {
				Route temp = aRIterator.next();
				if(temp.sName.equals(r_sName)) {
					returned = temp;
					break;
				}
			}
		}
			
		return(returned);
	}
	
	/** This function gets a Route object, by looking up a route by its lName in agencyRoutes
	 * It relies on agencyRoutes to have been initialized (otherwise we fail and return null).
	 * @param r_lName The lName of the route we're looking for.
	 * @return The Route object with that sName, or null if we couldn't find it.
	 */
	public static Route getRouteBylName(String r_lName) {
		Route returned = null;
		
		if(agencyRoutes != null) {
			Iterator<Route> aRIterator = agencyRoutes.iterator();
			
			while(aRIterator.hasNext()) {
				Route temp = aRIterator.next();
				if(temp.lName.equals(r_lName)) {
					returned = temp;
					break;
				}
			}
		}
			
		return(returned);
	}
	
	/** This method gets called when the user clicks the "About" button in the application-level menu. This is the "About" button that 
	 * is next to the Preferences button.
	 * @param c The context from which aboutDialog() gets called.
	 * @return The About dialog to display.
	 */
	public static Dialog aboutDialog(Context c) {
		String myMessage;
		
		if(Manager.isScheduleApp()) myMessage = Manager.getAppName() + " uses scheduled transit information to let you know"+ 
			" when your transit will arrive.\n\nSchedule information is based off of the transit agency's published schedule, so "+
			"it doesn't take into account unexpected delays or service changes.\n\nYou can use AnyStop to find transit routes and"+
			" stops near you or browse all routes and stops.\n\nDrop us a line at info@anystopapp.com if you have any suggestions or"+
			" feedback about the app. Thanks for using AnyStop!\n\nBrought to you by BlinkTag Inc.";
			
		else myMessage = Manager.getAppName() +" uses real-time transit information to give you the best possible estimation of "+
		"when your transit will arrive.\n\nReal-time information is based off of predictions made by GPS devices onboard the transit "+
		"vehicles, so these predications are generally quite	accurate.\n\nYou can use AnyStop to find transit routes and stops near"+
		" you or browse all routes and stops.\n\nDrop us a line at info@anystopapp.com if you have any suggestions or feedback about the"+
		" app. Thanks for using AnyStop!\n\nBrought to you by BlinkTag Inc. Powered by NextBus";

			
        Builder b = new AlertDialog.Builder(c)
        .setTitle("About " + Manager.getAppName())
        .setIcon(R.drawable.ico)
        .setMessage(myMessage)
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        return b.create();
        
	}
	
	/** This method is called when the AnyStop app is requested to make/fetch a prediction. This typically happens in
	 * one of the *Drill classes, although it can happen from FavStops and a few other locations also.
	 * 
	 * @param c The Context from which the Prediction was requested.
	 * @param s The SimpleStop (representing a transit stop) for which we are requesting a prediction.
	 */
	public static void flurryPredictionEvent(Context c, SimpleStop s) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		flurryEventMap.put("Agency", s.agency);
		flurryEventMap.put("Route", s.routeName);
		flurryEventMap.put("Headsign", s.headSign);
		flurryEventMap.put("Intersection", s.intersection);
		flurryEventMap.put("RealTime", "" + s.pred.isRT);
		FlurryAgent.logEvent("PredictionItem", flurryEventMap);
		
		Log.v("Manager", "flurryPredictionEvent called; parameters = {" + s.agency + ", " + s.routeName + ", " + s.headSign + ", " + s.intersection + ", " + s.pred.isRT + "}");
	}
	
	/** This method is called when the user select a Route to get information on.
	 * 
	 * @param r The Route (representing a transit route) for which we are requesting a prediction.
	 */
	public static void flurryRouteSelectEvent(Route r) {
		flurryRouteSelectEvent(r.sName);
	}
	
	/** This method is called when the user select a Route to get information on.
	 * 
	 * @param r The Route (representing a transit route) for which we are requesting a prediction.
	 */
	public static void flurryRouteSelectEvent(String routeName) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		flurryEventMap.put("Agency", Manager.getAgencyName());
		flurryEventMap.put("Route", routeName);
		
		FlurryAgent.logEvent("RouteSelectEvent", flurryEventMap);
		
		Log.v("Manager", "flurryRouteSelectEvent called; parameters = {" + Manager.getAgencyName() + ", " + routeName + "}");
	}
	
	/** This method gets called when AnyStop cannot make/fetch a prediction for a particular stop. It represents a soft, fluffy, 
	 * benign error - not so much an error even, as an unpleasant behavior.
	 *  
	 * @param currAgency2 - The Agency of the AnyStop app that calls this method.
	 * @param routeTracker2 - The RouteTracker string in the Manager class (not sure what it does). 
	 * @param intersection - The intersection from where we cannot make a prediction (default = "null_intersection")
	 */
	public static void flurryNoneFoundEvent(Agency currAgency2, String routeTracker2) {
		flurryNoneFoundEvent(currAgency2, routeTracker2, "null_intersection");
	}
	public static void flurryNoneFoundEvent(Agency currAgency2,
			String routeTracker2, String intersection) {
		flurryNoneFoundEvent(currAgency2.table, routeTracker2, intersection);
	}
	public static void flurryNoneFoundEvent(String agencyName,
			String routeTracker2, String intersection) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		flurryEventMap.put("Agency", agencyName);
		flurryEventMap.put("Route", routeTracker2);
		flurryEventMap.put("Intersection", intersection);
		FlurryAgent.logEvent("NotFoundItem", flurryEventMap);
		
		Log.v("Manager", "Called flurryNoneFoundEvent, parameters={" + agencyName + ", " + routeTracker2 + ", " + intersection + "}");
	}
	
	/** This method gets called when we do not succeed in finding nearby.
	 * 
	 */
	public static void flurryNearbyNotFoundEvent(Location loc, String agencyName) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		flurryEventMap.put("Agency", agencyName);
		
		if(loc!=null) {
			flurryEventMap.put("Latitude", Double.toString(loc.getLatitude()));
			flurryEventMap.put("Longitude", Double.toString(loc.getLongitude()));
		}
		
		FlurryAgent.logEvent("NearbyNotFoundEvent", flurryEventMap);
		
		if(loc != null)
			Log.v("Manager", "Called flurryNearbyNotFoundEvent, parameters={" + agencyName + ", " + 
				Double.toString(loc.getLatitude()) + ", " + Double.toString(loc.getLongitude()) + "}");
		else
			Log.v("Manager", "Called flurryNearbyNotFoundEvent, parameters={" + agencyName + "}");
	}
	
	/** This method gets called when an Activity is opened in AnyStop, it will register the opening of the activity 
	 * as a Flurry Analytics event. This allows us to track what Activities get opened and how many different pages
	 * a user browses to in a session.
	 * 
	 * @param openedActivityNameTag The NameTag of the calling Activity that is being opened. Calling convention within 
	 * @param searched Whether or not openedActivityNameTag was opened as a searched activity; default = false
	 * an Activity should be something like Manager.fluryActivityOpenEvent(this.activityNameTag)
	 */
	
	public static void flurryActivityOpenEvent(String openedActivityNameTag) {
		flurryActivityOpenEvent(openedActivityNameTag, true);
	}
	
	public static void flurryActivityOpenEvent(String openedActivityNameTag, boolean notSearched) {
		boolean searched = !notSearched;
		
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		flurryEventMap.put("ActivityNameTag", openedActivityNameTag); //pass the Name Tag as a parameter for FlurryAgent.logEvent()
		flurryEventMap.put("SearchedActivity", ((searched==true)?"true":"false")); //pass the Name Tag as a parameter for FlurryAgent.logEvent()
		
		FlurryAgent.logEvent("ActivityOpenEvent", flurryEventMap);
		
		Log.v(openedActivityNameTag, "Called Manager.flurryActivityOpenEvent, activity was " + openedActivityNameTag + ", opened as "
				+ ((searched==true)?"searched activity.":"regular activity."));
		
		//For some reason the below isn't supported in the (old) version of the Flurry Analytics API
		FlurryAgent.onPageView(); //This just increments the page count for the Flurry Analytics reporting
	}
	
	/** This method gets called when the server is unable to find predictions for the stop that the user requested in FavStops. 
	 * 
	 * @param openedActivityNameTag The NameTag of the calling Activity that is being opened. Calling convention within 
	 * an Activity should be something like Manager.fluryActivityOpenEvent(this.activityNameTag)
	 * @param agencyName The name of the agency that this app is associated with.
	 * @param routeName The name of the route that the selected Stop was on
	 * @param stopName The name of the stop that the user clicked on
	 */
	
	public static void flurryStopNotFoundEvent(String openedActivityNameTag, String agencyName, String routeName, String stopName) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		
		flurryEventMap.put("ActivityNameTag", openedActivityNameTag); //pass the Name Tag as a parameter for FlurryAgent.logEvent()
		flurryEventMap.put("Agency", agencyName);
		flurryEventMap.put("Route", routeName);
		flurryEventMap.put("Stop", stopName);
		
		FlurryAgent.logEvent("FavoriteStopNotFoundEvent", flurryEventMap);
		
		Log.v(openedActivityNameTag, "Called flurryStopNotFoundEvent, agency=" + agencyName + ", Route=" + routeName + ", Stop=" + stopName);
	}
	
	/** This method gets called when the server is unable to find predictions for the route that the user requested in FavRoutes. 
	 * 
	 * @param openedActivityNameTag The NameTag of the calling Activity that is being opened. Calling convention within 
	 * an Activity should be something like Manager.fluryActivityOpenEvent(this.activityNameTag)
	 * @param agencyName The name of the agency that this app is associated with.
	 * @param routeName The name of the route that the user had selected.
	 */
	
	public static void flurryStopNotFoundEvent(String openedActivityNameTag, String agencyName, String routeName) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		
		flurryEventMap.put("ActivityNameTag", openedActivityNameTag); //pass the Name Tag as a parameter for FlurryAgent.logEvent()
		flurryEventMap.put("Agency", agencyName);
		flurryEventMap.put("Route", routeName);
		
		FlurryAgent.logEvent("FavoriteRouteNotFoundEvent", flurryEventMap);
		
		Log.v(openedActivityNameTag, "Called flurryRouteNotFoundEvent, agency=" + agencyName + ", Route=" + routeName);
	}
	
	/** This method gets called by ChooseMap when the user clicks "Routes" or "Stops". It reports to the flurry analytics db whether 
	 * or not the user manually entered their location, and if they did, whether they used "my location" or entered in an actual thing.
	 * 
	 * @param locationEntered What location string the user entered; whether it was "My Location"("my_location") or something else ("manual")
	 * @param usedAuto Whether the user used an automatic location or not
	 */
	public static void flurryUserUsedMapEvent(String locationEntered, boolean usedAuto) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		
		String locEntered = new String();
		if(locationEntered==null) locEntered = "null";
		else if(locationEntered.equals("My Location")) locEntered = "my_location";
		else locEntered = "manual";
		
		flurryEventMap.put("LocationEntered", locEntered); //pass the Name Tag as a parameter for FlurryAgent.logEvent()
		flurryEventMap.put("UsedAuto", ((usedAuto==true)?"true":"false"));
		
		FlurryAgent.logEvent("ChooseMapSearchEvent", flurryEventMap);
		
		Log.v("Manager", "Called flurryUserUsedMapEvent, LocationEntered=" + locEntered + ", UsedAuto=" + ((usedAuto==true)?"true":"false"));
	}
	
	public static void flurryServerErrorEvent(String url) {
		Map<String, String> flurryEventMap = new HashMap<String, String>();
		flurryEventMap.put("URL", url);
		FlurryAgent.logEvent("ErrorItem", flurryEventMap);
		
		Log.v("Manager", "called flurryServerErrorEvent, url="+url);
	}
	
	public static boolean isUseUsage(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("usage_enabled", false);
	}
	
	public static String twoform(String s) {
		if (s.length()==1) {
			return "0"+s;
		}
		return s;
	}
	
	public static String getAgencyDisplayName() {
		return _agencyName;
	}
	public static String getAgencyTag() {
		return _agencyTag;
	}

	public static void setAgencyTag(String tag) {
		_agencyTag = tag;
	}

	public static void setAgencyName(String name) {
		_agencyName = name;
	}
	
	public static String getAgencyName() {
		return(_agencyName);
	}

	public static String getAppName() {
		return _appName;
	}

	public static void setAppName(String name) {
		_appName = name;
	}

	public static String getFlurryAPIK() {
		return _flurryAPIK;
	}

	public static void setFlurryAPIK(String _flurryapik) {
		_flurryAPIK = _flurryapik;
	}

	public static String getTableName() {
		return _agencyTableName;
	}

	public static void setTableName(String tableName) {
		_agencyTableName = tableName;
	}

	public static String getShortName() {
		return _agencyShortName;
	}

	public static void setShortName(String shortName) {
		_agencyShortName = shortName;
	}

	public static String getLongName() {
		return _agencyLongName;
	}

	public static void setLongName(String longName) {
		_agencyLongName = longName;
	}

	public static String getAdsAPIK() {
		return _adsAPIK;
	}

	public static void setAdsAPIK(String _adsapik) {
		_adsAPIK = _adsapik;
	}
	
	public static void setAdWhirlAPIK(String _adwhirlapik) {
		_adwhirlAPIK = _adwhirlapik;
	}

	public static String get_predictionType() {
		return _predictionType;
	}

	public static void setPredictionType(String type) {
		_predictionType = type;
	}
	
	public static void setHybridAgency(String isHybridAgency) {
		_hybridAgency = isHybridAgency.equals("isAHybridAgency");
	}
	
	public static boolean isHybridAgency() {
		return(_hybridAgency);
	}
	
	public static void setScheduleTableName(String newScheduleTableName) {
		_agencyScheduleTableName = new String(newScheduleTableName);
	}
	
	public static String getScheduleTableName() {
		return(_agencyScheduleTableName);
	}
	
	public static boolean isScheduleApp() {
		if(_predictionType != null && _predictionType.equalsIgnoreCase("schedule")) return(true);
		else return(false);
	}
	
	public static void setSettingURL(String newURL) {
		_settingURL = newURL;
	}
	
	public static String getSettingURL() {
		return(_settingURL);
	}
	
	public static HashMap<String, Boolean> getOverrides() {
		HashMap<String, Boolean> h = new HashMap<String, Boolean>();
		h.put("muni", true);
		h.put("portland", true);
		return h;
	}
	
	/** This static method will return the current location according to the phone's LocationManager
	 * @param caller The calling activity; required to do getSystemService
	 * @return Location object representing what the phone thinks is the current location, or none if no location is available.
	 */
	public static Location getMyLoc(Activity caller) {
		LocationManager locationManager = (LocationManager) caller.getSystemService(Context.LOCATION_SERVICE);
		//Give the AdView an AdRequest that will provide the last known network-based location
		if(locationManager != null) { //Avoid updating location gracefully if null parameters passed
			Location lastNetworkLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			return(lastNetworkLoc);
		}
		else return null;
	}

	/** This static method will create a new AdView object for the purpose of being added to some Activity's layout.
	 * It will also assign, to this new AdView, a network-provided Location from locationManager. Publisher ID and keywords are
	 * assigned also. 
	 * 
	 * @param caller The Activity that calls this function
	 * @return A new AdView object that is set up with the current Location, keywords, and publisher ID, and can be
	 * incorporated into the calling Activity's Layout.
	 */
	public static AdView setupAdView (Activity caller) {
		AdView returned = new AdView(caller, AdSize.BANNER, Manager.getAdsAPIK()); //!<This will be the AdView that gets returned to the caller.
		AdRequest request = new AdRequest();
		
		//Some stuff regarding how to place the ad
		returned.setGravity(Gravity.CENTER);
		
		Location currLoc = getMyLoc(caller);
		if(currLoc != null) request.setLocation(currLoc);
				
		//Give the AdView some keywords to use
		Set<String> keywordSet = new HashSet<String>();
		//Sadly we have to tokenize the admobsdk_keywords string in the resources
		String keywordStr = (String) caller.getString(R.string.admobsdk_keywords);
		String[] keywordArr = keywordStr.split("\\s"); //split by whitespace
		for(int count = 0; count < keywordArr.length; count++) keywordSet.add(keywordArr[count]);
		request.setKeywords(keywordSet); //pass set of keywords to the AdView via the AdRequest
		
		request.addTestDevice(AdRequest.TEST_EMULATOR);
		
		returned.loadAd(request); //pass request to the AdView to load a (hopefully relevant!) ad
		return(returned);
	}
	
	/** This method creates and returns an AdWhirlLayout for use in our app. It sets stuff like the APIK, the timeout,
	 * associates some keywords, and even sets test mode,
	 * @param caller The calling Activity for this method
	 * @return An AdWhirlLayout suitable for use in our app :)
	 */
	/*public static AdWhirlLayout setupAdWhirlLayout(Activity caller) {
		//Setup a bunch of global type stuff
		//I bet we don't have to do this every time we call a new activity but whatever
		String adwhirlConfigTimeoutString = (String) caller.getString(R.string.adwhirlTimeout);
		int adwhirlConfigTimeout = 60000;
		if(adwhirlConfigTimeoutString != null) adwhirlConfigTimeout = Integer.parseInt(adwhirlConfigTimeoutString);
		AdWhirlManager.setConfigExpireTimeout(adwhirlConfigTimeout);
		
		String adKeyWords = (String) caller.getString(R.string.admobsdk_keywords);
		if(adKeyWords != null)
			AdWhirlTargeting.setKeywords((String) caller.getString(R.string.admobsdk_keywords));
			
		boolean testingMode = false;
		String testingModeString = (String) caller.getString(R.string.globalTestingMode);
		if(testingModeString != null && testingModeString.equals("true")) testingMode = true;
		AdWhirlTargeting.setTestMode(testingMode);
		
		//The below AdWhirlLayout will be returned from this method
		AdWhirlLayout returned = new AdWhirlLayout(caller, _adwhirlAPIK);
		
		//This gets done according to "Android Best Practices" to scale the Ad size to something relevant for the user's device
		float density = caller.getResources().getDisplayMetrics().density;
		int diWidth=320; int diHeight=52;
		returned.setMaxWidth((int)(diWidth * density));
		returned.setMaxHeight((int)(diHeight * density));
		
		return(returned);
	}*/
	
	private static AswAdLayout currentAddienceAdview;
	private static long currentAddienceAdviewAge = 0; //time when we last refreshed currentAddienceAdview, in seconds
	private static final int currentAddienceAdviewAge_threshold = 29; //threshold for currentAddienceAdview "timeout"
	/** This methods returns, to an Activity, the Addience ad that it should use. The default behavior is 
	 * that we will create a new ad every 30 seconds. If the old ad has not "timed out" we will reuse it, 
	 * otherwise we'll create a new addience ad.
	 */
	public static AswAdLayout makeAddienceAd(Activity caller) {
		long current_time_seconds = System.currentTimeMillis()/1000; 
		if(currentAddienceAdview == null || (current_time_seconds-currentAddienceAdviewAge) > currentAddienceAdviewAge_threshold) {
			currentAddienceAdview = new AswAdLayout(caller);
			currentAddienceAdviewAge = current_time_seconds;
			
			setupAddienceAd(caller, currentAddienceAdview);
			
			Log.v(activityNameTag, "Loaded a new Addience ad!");
		}
		else {
			((ViewGroup)currentAddienceAdview.getParent()).removeView(currentAddienceAdview);
		}
		
		return(currentAddienceAdview);
	}
	
	/** This method performs the necessary setup for Addience ads 
	 * @param caller The calling Activity.
	 * @param adview The AswAdLayout that we are setting up. 
	 */
	public static void setupAddienceAd(Activity caller, AswAdLayout adview) {
		
		//Trying to reproduce the below XML, here, in Java code.
		/*<com.sensedk.AswAdLayout
		android:id="@+id/addience_adview"
		android:layout_height="50dip" (check)
		android:layout_width="fill_parent" (check)
		android:layout_gravity="center"
		android:gravity="center"
		android:background="#000000" (check)
		/>*/
		
		adview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 50));
		adview.setBackgroundColor(0x00000000);
		adview.setActivity(caller);
	}
	
	/** This method tells us what type of ad to use.
	 * @return Type of advertisement to use (ADDIENCE_AD, ADMOB_AD, ...)
	 */
	public static int adTypeUsing() {		
		if(isScheduleApp()) {
			if(GlobalSettings.getSetting("adsForScheduleApps") != null && 
					GlobalSettings.getSetting("adsForScheduleApps").equals("admob")) {
				Log.v(activityNameTag, "Using admob ads!");
				return ADMOB_AD;
			} else {
				Log.v(activityNameTag, "Using addience ads!");
				return ADDIENCE_AD;
			}
		}
		else {
			Log.v(activityNameTag, "Using admob ads!");
			return ADMOB_AD;
		}
	}
	
	/**
	 * This method sets the default Typeface to use in this App. It should be called by StopsTime, before any activities 
	 * are launched.
	 * @param defaultTypefaceString The String that provides a path to the ttf font file to use for the default Typeface.  
	 */
	public static void setDefaultTypeface(Typeface newTypeface) {
		if(newTypeface == null) {
			Log.e(activityNameTag, "Got null as parameter for setDefaultTypeface");
			defaultTypeface = null;
		}
		else {
			defaultTypeface = newTypeface;
		}
	}
	
	/**
	 * This method will apply Typeface Manager.defaultTypeface to View v, and all of its children. 
	 * In the event that defaultTypeface was not set it will do nothing.  
	 * The code was (roughly) lifted from:
	 *    http://stackoverflow.com/questions/2973270/using-a-custom-typeface-in-android
	 * @param v The (parent) View to apply the Typeface to.
	 * 
	 */
	public static void applyFonts(final View v)	{
		//Log.v("Manager", "Manager.applyFonts() was called.");
		if(defaultTypeface == null) {
			Log.d(activityNameTag, "Manager.applyFonts() was called without defaultTypeface set.");
			return;
		}
		if(v == null) {
			Log.e(activityNameTag, "Manager.applyFonts() was called with null parameter.");
			return;
		}
		
		try {
			//If v is a ViewGroup, call applyFonts on all of its children
			if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				Log.v("Manager", "Calling applyFonts recursively on ViewGroup " + vg);
				for (int i = 0; i < vg.getChildCount(); i++) applyFonts(vg.getChildAt(i));
			//Otherwise if v is a TextView set its Typeface
			} else if (v instanceof TextView) {
				((TextView)v).setTypeface(defaultTypeface);
				Log.v("Manager", "Applying TypeFace to View: " + v);
			}
			//Otherwise do nothing
			else {
				Log.v("Manager", "View " + v + " was neither TextView nor ViewGroup; no font was applied.");
			}
		} catch (Exception e) {
			Log.d("Manager", "Skipping application of TypeFace to View: " + v);
		}
	}
	
	/** This method prints, to Log.v(), memory usage info for the currently running app. 
	 * In particular, it will print the total private dirty page set and total proportional set size in kB for the running app. 
	 * The PSS is probably most relevant; this is the total number of pages used, where each page's size is divided by the number 
	 * of processes that share that page.
	 * 
	 */
	/*public static void printMemoryUsageInfo() {
		Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
		Debug.getMemoryInfo(memInfo);
		
		//Print out stuff to Log.v() about memory usage
		Log.v(activityNameTag, "AnyStop app currently has PSS of " + memInfo.getTotalPss() + "kB and total private dirty page set of " 
				+ memInfo.getTotalPrivateDirty() + "kB.");
	}*/
	
	/** This method returns the nearest SimpleStop to the current location out of a List of SimpleStops. 
	* If no current location is set, then null is returned. If the list has no SimpleStops,
	* then we return null. 
	* @param listOfStops a List of the SimpleStops to search through.
	* @param caller The calling Activity's context, for calling getMyLoc.
	*/
	public static SimpleStop getNearestStop(List<SimpleStop> listOfStops, Activity caller) {
		//Catch invalid parameters
		if(caller==null) return null;
		if(listOfStops==null || listOfStops.size() < 1) return null;
		
		//If we don't know current location, return first item in listOfStops
		Location currLoc = getMyLoc(caller);
		if(currLoc==null) return null;
		
		//This loop actually does the linear search through listOfStops to find the nearest SimpleStop
		double minDistance = Double.POSITIVE_INFINITY; //double-check syntax
		SimpleStop nearestStop = null;
		double currLat = currLoc.getLatitude();
		double currLon = currLoc.getLongitude();
		
		for(SimpleStop s : listOfStops) {
			double stopLat = ((double)s.lat) / 1000000.0;
			double stopLon = ((double)s.lon) / 1000000.0;
			double stopDist = GeoUtils.distanceKm(currLat, stopLat, currLon, stopLon);
			
			if(stopDist < minDistance) {
				minDistance = stopDist;
				nearestStop = s;
			}
		}
		
		return(nearestStop);
	}
	
	public static void loadSettings() {
		if(_settingURL != null) setSettings(_settingURL);
	}
	
	/**
	 * This method will set up the settings inside of the GlobalSettings class, using the settings located
	 * at the provided URL. It should probably be run from a non-blocking thread, like a DataThread
	 * @param settingURL
	 */
	public static void setSettings (String settingURL) {
		StringBuffer b = new StringBuffer();
    	
		try {	
			String s;
			URL u = new URL(settingURL);
	    	InputStream is = u.openStream();
	    	DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
	    	
			while ((s = dis.readLine()) != null) {
				b.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(activityNameTag, "Couldn't get data from server for GlobalSettings!");
			return;
		}

		Log.v(activityNameTag, "Loaded settings from server successfully!");
		GlobalSettings.processSettings(b.toString());
	}
}
