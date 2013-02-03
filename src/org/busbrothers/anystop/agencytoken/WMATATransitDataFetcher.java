/** This class is used to fetch data from the WMATA JSON API. It can accept various queries (via
 * method calls like "fetchAgencyRouteList()", parse the resulting JSON response, and translate it
 * into AnyStop objects like Route, SimpleStop, etc. Everything is 100% static and the only 
 * state is the routesNameMap cache. 
 * 
 * This method is accessed entirely from WMATATransitDataManager; this class holds state regarding
 * which transactions have been performed to for example refresh the previously searched data.
 * 
 * Written I. Yulaev 2013-01-(01 - 18)
 */

package org.busbrothers.anystop.agencytoken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.busbrothers.anystop.agencytoken.datacomponents.Prediction;
import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class WMATATransitDataFetcher {
	public static final boolean USING_RT_FEED = true;
	
	//Define the URLs for API endpoints
	public static final String WMATA_RAIL_ROUTES_URL = "http://api.wmata.com/Rail.svc/json/JLines";
	public static final String WMATA_RAIL_STOPS_URL = "http://api.wmata.com/Rail.svc/json/JStations";
	public static final String WMATA_RAIL_PREDICTIONS_URL = "http://api.wmata.com/StationPrediction.svc/json/GetPrediction/";
	public static final String WMATA_RAIL_NEAREST_STATIONS_URL = "http://api.wmata.com/Rail.svc/json/JStationEntrances";
	
	public static final String WMATA_BUS_ROUTES_URL = "http://api.wmata.com/Bus.svc/json/JRoutes";
	public static final String WMATA_BUS_STOPS_URL = "http://api.wmata.com/Bus.svc/json/JRouteDetails";
	public static final String WMATA_BUS_ALL_STOPS_URL = "http://api.wmata.com/Bus.svc/json/JStops";
	public static final String WMATA_BUS_PREDICTIONS_URL = "http://api.wmata.com/NextBusService.svc/json/JPredictions";
	public static final String WMATA_BUS_NEAREST_STATIONS_URL = "http://api.wmata.com/Bus.svc/json/JStops";
	
	private static final String WMATA_APIKEY="7j6wqja5t48cfkt2hw33ugp8";
	
	private static final int MAXIMUM_STOPS_FROM_JSON = 500;
	
	private static final String activitynametag = "WMATATransitDataFetcher";
	
	/**Stores a mapping of route code to Route object, for filling in route lists
	in SimpleStop objects*/
	private static HashMap<String, Route> routesNameMap;
	
	/**Clears the routesNameMap (route short name -> Route object cache)*/
	public static void reset() {
		routesNameMap = null;
	}
	
	/**Fills in the routesNameMap (route short name -> Route object cache)*/
	private static void fillInRoutesNameMap() {
		routesNameMap = new HashMap<String, Route>();
		
		ArrayList<Route> allRoutes = fetchAgencyRouteList();
		
		for(Route r : allRoutes)
			routesNameMap.put(r.sName, r);
	}
	
	/**Returns the Route object for the given sname, from routesNameMap
	return null if a corresponding Route does not exist.
	*/ 
	private static Route getRouteNameFromSname(String sname) {
		if(routesNameMap == null) {
			fillInRoutesNameMap();
		}
		
		if(routesNameMap.containsKey(sname)) {
			return routesNameMap.get(sname);
		}
		
		return null;
	}
	
	/** "Fixes" a given SimpleStop by turning all of the RouteIds, stored in the routeName field
	 * in comma-delimited format, into the corresponding Route objects' lNames, since the latter
	 * is easier for a human to understand 
	 * @param sl An ArrayList of SimpleStops to "fix"
	 */
	private static void fixSimpleStopRoutes(ArrayList<SimpleStop> sl) {
		if(routesNameMap == null) {
			fillInRoutesNameMap();
		}
		
		for(SimpleStop s : sl)
			fixSimpleStopRoutes(s, routesNameMap);
	}
	/** "Fixes" a given SimpleStop by turning all of the RouteIds, stored in the routeName field
	 * in comma-delimited format, into the corresponding Route objects' lNames, since the latter
	 * is easier for a human to understand 
	 * @param s A SimpleStop to "fix"
	 */
	private static void fixSimpleStopRoutes(SimpleStop s) {
		if(routesNameMap == null) {
			fillInRoutesNameMap();
		}
		
		fixSimpleStopRoutes(s, routesNameMap);
	}
	
	/** Stupid helper method. Takes a SimpleStop and transforms the route, which is a 
	 * comma-delimited string of route codes, into a comma-delimited string of long
	 * route names (requires being given a Route.sName -> Route map)
	 * @param s the SimpleStop to "fix" the routeName of
	 * @param map The sName -> Route object map
	 */
	private static void fixSimpleStopRoutes(SimpleStop s, HashMap<String, Route> map) {
		if(s.routeName == null) return;
		
		String newRoutes = "";
		String[] routeCodes = s.routeName.split(",");
		
		for(int i = 0; i < routeCodes.length; i++) {
			if(!map.containsKey(routeCodes[i])) {
				newRoutes += routeCodes[i];
			} else {
				newRoutes += map.get(routeCodes[i]).lName;
			}
			
			if(i < routeCodes.length-1) newRoutes += ",";
		}
		
		s.routeName = newRoutes;
	}
	
	/** Return the Route object represented by the JSONObject routeObject. Usable for WMATA
	 * Rail API calls */
	private static Route jsonToRouteRail(JSONObject routeObject) {
		Route returned = new Route();
		
		try {
			if(routeObject.has("DisplayName")) returned.lName = "r_" + routeObject.getString("DisplayName");
			if(routeObject.has("LineCode")) returned.sName = routeObject.getString("LineCode");
		} catch (JSONException e) {
			System.err.println(e);
		}
		
		returned.isRT = USING_RT_FEED;
		
		return(returned);
	}
	
	/** Return the Route object represented by the JSONObject routeObject. Usable for WMATA
	 * Bus API calls */
	private static Route jsonToRouteBus(JSONObject routeObject) {
		Route returned = new Route();
		
		try {
			if(routeObject.has("Name")) {
				returned.lName = routeObject.getString("Name");
				returned.lName = returned.lName.replaceAll("  +", " ");
			}
			if(routeObject.has("RouteID")) returned.sName = routeObject.getString("RouteID");
		} catch (JSONException e) {
			System.err.println(e);
		}
		
		returned.isRT = USING_RT_FEED;
		
		return(returned);
	}

	/** Return the SimpleStop object represented by the JSONObject stopObject. Usable for WMATA
	 * Rail API calls 
	 * @param stopObject the JSONObject to extract data from, to create a SimpleStop
	 * @param do_fix_routes Whether or not to leave the RouteIds in the SimpleStop's routeName field, or
	 * to convert the RouteIds into Route long names (which are human readable)
	 * */
	private static SimpleStop jsonToSimpleStopRail(JSONObject stopObject) {
		return jsonToSimpleStopRail(stopObject, true);
	}
	private static SimpleStop jsonToSimpleStopRail(JSONObject stopObject, boolean do_fix_routes) {
		SimpleStop returned = new SimpleStop();
		returned.agency = Manager.getAgencyName();
		returned.headSign="";
				
		try {
			if(stopObject.has("Code")) returned.stopcode = stopObject.getString("Code");
			if(stopObject.has("Lat")) returned.lat = (int) (1000000.0 * stopObject.getDouble("Lat"));
			if(stopObject.has("Lon")) returned.lon = (int) (1000000.0 * stopObject.getDouble("Lon"));
			if(stopObject.has("Name")) returned.intersection = stopObject.getString("Name");
			
			returned.routeName = new String();
			if(stopObject.has("LineCode1") && !stopObject.isNull("LineCode1")) 
				returned.routeName += stopObject.getString("LineCode1");
			if(stopObject.has("LineCode2") && !stopObject.isNull("LineCode2")) 
				returned.routeName += "," + stopObject.getString("LineCode2");
			if(stopObject.has("LineCode3") && !stopObject.isNull("LineCode3")) 
				returned.routeName += "," + stopObject.getString("LineCode3");
			if(stopObject.has("LineCode4") && !stopObject.isNull("LineCode4")) 
				returned.routeName += "," + stopObject.getString("LineCode4");

		} catch (JSONException e) {
			System.err.println(e);
		}
		
		//convert the routeNames from a list of route codes to a list of human-readable route names
		if(do_fix_routes) fixSimpleStopRoutes(returned);
		
		returned.isRTstr = USING_RT_FEED?"true":"false";
		
		return(returned);
	}

	/** Return the SimpleStop object represented by the JSONObject stopObject. Usable for WMATA
	 * Bus API calls 
	 * @param stopObject the JSONObject to extract data from, to create a SimpleStop
	 * @param do_fix_routes Whether or not to leave the RouteIds in the SimpleStop's routeName field, or
	 * to convert the RouteIds into Route long names (which are human readable)
	 * */
	private static ArrayList<SimpleStop> jsonToSimpleStopsBus(JSONObject stopsObject) {
		return jsonToSimpleStopsBus(stopsObject, true);
	}
	private static ArrayList<SimpleStop> jsonToSimpleStopsBus(JSONObject stopsObject, boolean do_fix_routes) {
		//Will contain map from SimpleStop.intersection -> SimpleStop
		//Used to later group SimpleStops by same intersection (but possibly different headsigns)
		HashMap<String, ArrayList<SimpleStop>> returnedMap = new HashMap<String, ArrayList<SimpleStop>>();
		ArrayList<SimpleStop> returned = new ArrayList<SimpleStop>();
		
		if(!stopsObject.has("Direction0") && 
				!stopsObject.has("Direction1") && 
				!stopsObject.has("Stops")) return null;
		
		
		//If we have Direction entries, add them to the list of Directions to get a list of Stops for
		ArrayList<String> directionStrings = new ArrayList<String>();
		if(stopsObject.has("Direction0")) directionStrings.add("Direction0");
		if(stopsObject.has("Direction1")) directionStrings.add("Direction1");
		//Process Stops for each direction entry
		for(String dirObjectName : directionStrings) {
			try {
				JSONObject dirObject = stopsObject.getJSONObject(dirObjectName);
				String headSignName = dirObject.getString("TripHeadsign");
				JSONArray stopsArray = dirObject.getJSONArray("Stops");
				
				for(int i = 0; i < stopsArray.length() && i < MAXIMUM_STOPS_FROM_JSON; i++) {
					try {
						JSONObject jsonStop = stopsArray.getJSONObject(i);
						SimpleStop newSimpleStop = new SimpleStop();
						
						newSimpleStop.agency = Manager.getAgencyName();
						newSimpleStop.headSign = headSignName;
						if(jsonStop.has("Name")) newSimpleStop.intersection = jsonStop.getString("Name");
						if(jsonStop.has("StopID")) newSimpleStop.stopcode = jsonStop.getString("StopID");
						//I don't think this ever gets called
						if(jsonStop.has("Routes")) newSimpleStop.routeName = jsonStop.getString("Routes");
						
						if(jsonStop.has("Lat")) newSimpleStop.lat = (int) (1000000.0 * jsonStop.getDouble("Lat"));
						if(jsonStop.has("Lon")) newSimpleStop.lon = (int) (1000000.0 * jsonStop.getDouble("Lon"));
						newSimpleStop.isRTstr = USING_RT_FEED?"true":"false";
						
						//use returnedMap to combine SimpleStops by intersection
						if(!returnedMap.containsKey(newSimpleStop.intersection))
							returnedMap.put(newSimpleStop.intersection, new ArrayList<SimpleStop>());
							
						returnedMap.get(newSimpleStop.intersection).add(newSimpleStop);
						
					} catch (JSONException e) {
						System.err.println(e);
					}
				}
			} catch (JSONException e) {
				System.err.println(e);
			}
		}
		
		//Only do the headsign sorting/combining by intersection if we had Direction Strings
		if(directionStrings.size() > 0 ) {
			Set<String> intersections = returnedMap.keySet();
			for(String intersection : intersections) {
				ArrayList<SimpleStop> stopsWSameName = returnedMap.get(intersection);
				if(stopsWSameName != null && stopsWSameName.size() > 0) {
					SimpleStop first = stopsWSameName.get(0);
					
					for(int i = 1; i < stopsWSameName.size(); i++) {
						first.headSign += ", " + stopsWSameName.get(i).headSign;
						first.stopcode += ";" + stopsWSameName.get(i).stopcode;
					}
					
					returned.add(first);
				}
			}
		}
		
		
		
		
		//Now process if we had Stops entry in the JSONObject
		if(stopsObject.has("Stops")) {
			try {
				JSONArray stopsArray = stopsObject.getJSONArray("Stops");
				
				for(int i = 0; i < stopsArray.length() && i < MAXIMUM_STOPS_FROM_JSON; i++) {
					try {
						JSONObject jsonStop = stopsArray.getJSONObject(i);
						SimpleStop newSimpleStop = new SimpleStop();
						
						newSimpleStop.agency = Manager.getAgencyName();
						newSimpleStop.headSign = new String("");
						if(jsonStop.has("Name")) newSimpleStop.intersection = jsonStop.getString("Name");
						if(jsonStop.has("StopID")) newSimpleStop.stopcode = jsonStop.getString("StopID");
						if(jsonStop.has("Routes")) {
							JSONArray routeArray = jsonStop.getJSONArray("Routes");
							newSimpleStop.routeName = new String("");
							
							for(int j = 0; j < routeArray.length(); j++) {
								newSimpleStop.routeName += routeArray.getString(j);
								if(j<routeArray.length()-1) newSimpleStop.routeName += ",";
							}
						}
						
						if(jsonStop.has("Lat")) newSimpleStop.lat = (int) (1000000.0 * jsonStop.getDouble("Lat"));
						if(jsonStop.has("Lon")) newSimpleStop.lon = (int) (1000000.0 * jsonStop.getDouble("Lon"));
						newSimpleStop.isRTstr = USING_RT_FEED?"true":"false";
						
						returned.add(newSimpleStop);
												
					} catch (JSONException e) {
						System.err.println(e);
					}
				}
			} catch (JSONException e) {
				System.err.println(e);
			}
		}
		
		if(do_fix_routes) fixSimpleStopRoutes(returned);		
		return(returned);
	}
	
	
	/** Return an ArrayList of SimpleStops, with associated Prediction datum for each, given a SimpleStop 
	 * which we have fetched predictions for and a predictionArray storing the JSON return from the WMATA
	 * Rails API
	 * */
	private static ArrayList<SimpleStop> jsonToRailPrediction(SimpleStop predStop, JSONArray predictionArray) {
		HashMap<String, ArrayList<Integer>> routeHeadsignToPredMap = new HashMap<String, ArrayList<Integer>>();
				
		for(int i = 0; i < predictionArray.length(); i++) {
			try {
				JSONObject predictionObject = predictionArray.getJSONObject(i);
				
				if(predictionObject.has("Min") && 
						!predictionObject.isNull("Min") && 
						( predictionObject.getInt("Min") >= 0 )) {
					String hashKey = predictionObject.getString("Line") + ";" + predictionObject.getString("DestinationName");
					if(!routeHeadsignToPredMap.containsKey(hashKey))
						routeHeadsignToPredMap.put(hashKey, new ArrayList<Integer>());
					routeHeadsignToPredMap.get(hashKey).add(Integer.parseInt(predictionObject.getString("Min")));
				}
			} catch (JSONException e) {
				System.err.println(e);
			}
		} 
		
		ArrayList<SimpleStop> returned = new ArrayList<SimpleStop>();
		
		Iterator<String> keysIt = routeHeadsignToPredMap.keySet().iterator();
		
		while(keysIt.hasNext()) {
			String key = keysIt.next();
			
			String[] keyComponents = key.split(";");
			
			SimpleStop currStop = new SimpleStop();
			currStop.agency = Manager.getAgencyName();
			currStop.routeName = keyComponents[0];
			currStop.headSign = keyComponents[1];
			currStop.intersection = predStop.intersection;
			currStop.isRTstr = predStop.isRTstr;
			currStop.lat = predStop.lat;
			currStop.lon = predStop.lon;
			currStop.stopcode = predStop.stopcode;
			
			int[] preds_in_minutes_arr = new int[routeHeadsignToPredMap.get(key).size()];
			for(int i = 0; i < routeHeadsignToPredMap.get(key).size(); i++)
				preds_in_minutes_arr[i] = routeHeadsignToPredMap.get(key).get(i);
			
			Prediction prediction = new Prediction(preds_in_minutes_arr, USING_RT_FEED);
			currStop.pred = prediction;
			
			returned.add(currStop);
		}
		
		//convert the routeNames from a list of route codes to a list of human-readable route names
		fixSimpleStopRoutes(returned);
		
		return(returned);
	}
	
	
	
	/** Return an ArrayList of SimpleStops, with associated Prediction datum for each, given a SimpleStop 
	 * which we have fetched predictions for and a predictionArray storing the JSON return from the WMATA
	 * Bus API
	 * */
	private static ArrayList<SimpleStop> jsonToBusPrediction(SimpleStop predStop, JSONArray predictionArray) {
		HashMap<String, ArrayList<Integer>> routeHeadsignToPredMap = new HashMap<String, ArrayList<Integer>>();
				
		for(int i = 0; i < predictionArray.length(); i++) {
			try {
				JSONObject predictionObject = predictionArray.getJSONObject(i);
				
				if(predictionObject.has("Minutes") && 
						!predictionObject.isNull("Minutes") && 
						( predictionObject.getInt("Minutes") >= 0 )) {
					String hashKey = predictionObject.getString("RouteID") + ";" + predictionObject.getString("DirectionText");
					if(!routeHeadsignToPredMap.containsKey(hashKey))
						routeHeadsignToPredMap.put(hashKey, new ArrayList<Integer>());
					routeHeadsignToPredMap.get(hashKey).add(Integer.parseInt(predictionObject.getString("Minutes")));
				}
			} catch (JSONException e) {
				System.err.println(e);
			}
		} 
		
		ArrayList<SimpleStop> returned = new ArrayList<SimpleStop>();
		
		Iterator<String> keysIt = routeHeadsignToPredMap.keySet().iterator();
		
		while(keysIt.hasNext()) {
			String key = keysIt.next();
			
			String[] keyComponents = key.split(";");
			
			SimpleStop currStop = new SimpleStop();
			currStop.agency = Manager.getAgencyName();
			currStop.routeName = keyComponents[0];
			currStop.headSign = keyComponents[1];
			currStop.intersection = predStop.intersection;
			currStop.isRTstr = predStop.isRTstr;
			currStop.lat = predStop.lat;
			currStop.lon = predStop.lon;
			currStop.stopcode = predStop.stopcode;
			
			int[] preds_in_minutes_arr = new int[routeHeadsignToPredMap.get(key).size()];
			for(int i = 0; i < routeHeadsignToPredMap.get(key).size(); i++)
				preds_in_minutes_arr[i] = routeHeadsignToPredMap.get(key).get(i);
			
			Prediction prediction = new Prediction(preds_in_minutes_arr, USING_RT_FEED);
			currStop.pred = prediction;
			
			returned.add(currStop);
		}
		
		//convert the routeNames from a list of route codes to a list of human-readable route names
		fixSimpleStopRoutes(returned);
		
		return(returned);
	}
	
	/** returns a List of all WMATA routes, encoded as Route objects */
	public static ArrayList<Route> fetchAgencyRouteList() {
		String url = WMATA_RAIL_ROUTES_URL + "?api_key=" + WMATA_APIKEY;
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);
		
		JSONObject jsonResult = null;
		JSONArray jsonRouteObjectArray = null;
		try {
			jsonResult = new JSONObject( jsonText ); 
			jsonRouteObjectArray = (JSONArray) jsonResult.getJSONArray("Lines");
		} catch (JSONException e) {System.err.println(e);}
		
		//Convert JSONArray to ArrayList of Route objects
		ArrayList <Route> routeList = new ArrayList<Route>();
		if(jsonRouteObjectArray != null) {
			for(int i = 0; i < jsonRouteObjectArray.length(); i++) {
				//Build a route object from the JSon object
				Route newRoute;				
				try { newRoute = jsonToRouteRail(jsonRouteObjectArray.getJSONObject(i)); }
				catch (JSONException e) { System.err.println(e); newRoute = null; }
				
				if(newRoute != null) routeList.add(newRoute);
			}
		} else {
			System.out.println("No objects in jsonRouteObjectArray! Terminating...");
		}
		
		//Fill in route name cache since it's convenient to do so now
		if(routesNameMap == null) {
			routesNameMap = new HashMap<String, Route>();
			for(Route r : routeList) {
				routesNameMap.put(r.sName, r);
			}
		}
		
		
		//Now, fetch busses
		url = WMATA_BUS_ROUTES_URL + "?api_key=" + WMATA_APIKEY;
		jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);
		//Avoid getting back duplicate routes ; WMATA seems like like giving us a whole
		//list of routes with a bunch of duplicates except only the first one has a valid RouteId
		HashSet<String> lNamesSeen = new HashSet<String>();
		
		jsonRouteObjectArray = null;
		try {
			jsonResult = new JSONObject( jsonText ); 
			jsonRouteObjectArray = (JSONArray) jsonResult.getJSONArray("Routes");
		} catch (JSONException e) {System.err.println(e);}
		
		//Convert JSONArray to ArrayList of Route objects
		if(jsonRouteObjectArray != null) {
			for(int i = 0; i < jsonRouteObjectArray.length(); i++) {
				//Build a route object from the JSon object
				Route newRoute;				
				try { newRoute = jsonToRouteBus(jsonRouteObjectArray.getJSONObject(i)); }
				catch (JSONException e) { System.err.println(e); newRoute = null; }
				
				if(newRoute != null && !lNamesSeen.contains(newRoute.lName)) {
					routeList.add(newRoute);
					lNamesSeen.add(newRoute.lName);
				}
			}
		} else {
			System.out.println("No objects in busses jsonRouteObjectArray! Terminating...");
		}
		
		//Fill in route name cache since it's convenient to do so now
		if(routesNameMap == null) {
			routesNameMap = new HashMap<String, Route>();
		}
		
		for(Route r : routeList) {
			routesNameMap.put(r.sName, r);
		}
		
		return(routeList);		
	}
	
	/** returns a list of all stops/stations for a given route/line 
	 * @param r The Route to fetch a list of stops for
	 * @param do_fix_routes Whether or not to convert the returned SimpleStops' routeName field into Route
	 * long (human-readable) names rather than leaving them as RouteIds. The latter is useful if some post-processing
	 * will be done on the returned SimpleStops that requires knowing what routes they serve.*/
	public static ArrayList<SimpleStop> fetchStopsByRoute(Route r) {
		return fetchStopsByRoute(r, true);
	}
	public static ArrayList<SimpleStop> fetchStopsByRoute(Route r, boolean do_fix_routes) {
		String url;
		
		//Initially try to get routes for rail
		if(r == null) {
			url = WMATA_RAIL_STOPS_URL 
					+ "?api_key=" + WMATA_APIKEY;
		} else {
			url = WMATA_RAIL_STOPS_URL 
				+ "?LineCode=" + r.sName
				+ "&api_key=" + WMATA_APIKEY;
		}
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);
		
		JSONObject jsonResult = null;
		JSONArray jsonStopObjectArray = null;
		try {
			jsonResult = new JSONObject( jsonText ); 
			if(jsonResult.has("Stations"))
				jsonStopObjectArray = (JSONArray) jsonResult.getJSONArray("Stations");
		} catch (JSONException e) {System.err.println(e);}
		
		//Convert JSONArray to ArrayList of Route objects
		ArrayList <SimpleStop> stopList = new ArrayList<SimpleStop>();
		if(jsonStopObjectArray != null && jsonStopObjectArray.length() > 0) {
			for(int i = 0; i < jsonStopObjectArray.length(); i++) {
				//Build a route object from the JSon object
				SimpleStop newStop;				
				try { newStop = jsonToSimpleStopRail(jsonStopObjectArray.getJSONObject(i), do_fix_routes); }
				catch (JSONException e) { System.err.println(e); newStop = null; }
				
				if(newStop != null) stopList.add(newStop);
			}
		} else {
			System.out.println("No objects in jsonRouteObjectArray! (url=" + url + ")Terminating...");
		}
		
		
		
		
		//Try to get stops for busses
		//Don't try to fetch any if no route is given (the number of names returned is crippling...)
		if(r != null) {  
			url = WMATA_BUS_STOPS_URL 
					+ "?routeId=" + r.sName
					+ "&api_key=" + WMATA_APIKEY;
			
			Log.d(activitynametag, "Getting stops using URL=" + url);
	
			jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);		
			jsonResult = null;
			try {
				jsonResult = new JSONObject( jsonText ); 
			} catch (JSONException e) {System.err.println(e);}
			
			//Convert JSONArray to ArrayList of Route objects
			if(jsonResult != null) {
				ArrayList<SimpleStop> busStops = jsonToSimpleStopsBus(jsonResult);
								
				if(busStops != null)
					for(SimpleStop s : busStops) stopList.add(s);
			} else {
				System.out.println("No objects in jsonRouteObjectArray! (url=" + url + ")Terminating...");
			}
		}

		return(stopList);
	}
	
	/** Return predictions for arrival times for a given SimpleStop. In fact we will 
	 * return multiple SimpleStop objects, since we have no better way of expressing the multiple
	 * route and headsign combinations that Predictions will probably return */
	public static ArrayList<SimpleStop> fetchPredictionsByStop(SimpleStop s) {
		//If the stopcode field contains the ";" character, this means that it is in fact several
		//stopcodes mashed together. We should return the prediction results for all of these stopcodes
		if(s.stopcode.contains(";")) {
			return(fetchPredictionsByStops(splitStop(s)));
		}
		
		String url = WMATA_RAIL_PREDICTIONS_URL 
				+ s.stopcode
				+ "?api_key=" + WMATA_APIKEY;
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);
		
		JSONObject jsonResult = null;
		JSONArray jsonPredictionObjectArray = null;
		try {
			jsonResult = new JSONObject( jsonText ); 
			if(jsonResult.has("Trains"))
				jsonPredictionObjectArray = (JSONArray) jsonResult.getJSONArray("Trains");
		} catch (JSONException e) {System.err.println(e);}
		
		if(jsonPredictionObjectArray != null && jsonPredictionObjectArray.length() > 0) {
			return(jsonToRailPrediction(s, jsonPredictionObjectArray));
		}
		
		url = WMATA_BUS_PREDICTIONS_URL 
				+ "?StopID=" + s.stopcode
				+ "&api_key=" + WMATA_APIKEY;
		
		jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);
		Log.d(activitynametag, "Getting bus predictions using URL=" + url);
		
		try {
			jsonResult = new JSONObject( jsonText ); 
			if(jsonResult.has("Predictions"))
				jsonPredictionObjectArray = (JSONArray) jsonResult.getJSONArray("Predictions");
		} catch (JSONException e) {System.err.println(e);}
		
		if(jsonPredictionObjectArray != null && jsonPredictionObjectArray.length() > 0) {
			return(jsonToBusPrediction(s, jsonPredictionObjectArray));
		}
		
		//Failed to get any predictions - return a big ol' blank
		return(new ArrayList<SimpleStop>());
	}
	
	/** Return predictions for arrival times for a given ArrayList of SimpleStops. All this will do
	 * is call fetchPredictionsByStop(SimpleStop) multiple times and concatenate the results.
	 * @param stops An ArrayList of SimpleStops for which to get Predictions
	 * @return Another ArrayList of SimpleStops, with Prediction datums attached to each.
	 */
	private static final int MAX_PREDICTIONS_TO_ALLOW = 25;
	public static ArrayList<SimpleStop> fetchPredictionsByStops(ArrayList<SimpleStop> stops) {
		ArrayList<SimpleStop> retval = new ArrayList<SimpleStop>();
		
		int iter_count = 0;
		
		for(SimpleStop s : stops) {
			if(++iter_count > MAX_PREDICTIONS_TO_ALLOW) break;
			
			ArrayList<SimpleStop> newStops = fetchPredictionsByStop(s);
			
			for(SimpleStop t : newStops) {
				retval.add(t);
			}
		}
		
		return retval;
	}
	
	private static final int RADIUS_IN_METERS_RAIL_SEARCH = 1000;
	private static final int RADIUS_IN_METERS_BUS_SEARCH = 500;
	/** Return a list of Route -> List<SimpleStop> entries, in a Map, where the keys are Rail routes within 
	 * RADIUS_IN_METERS_RAIL_SEARCH and Bus routes within RADIUS_IN_METERS_BUS_SEARCH, and the ArrayLists
	 * are lists of (nearby) Stops that that Route serves. 
	 * 
	 * @param lat The latitude of the requestor, given in degrees / 10 (because that's what Location.getLatitude() returns)
	 * @param lon The longitude of the requestor, given in degrees / 10
	 * @return The Map of Routes -> ArrayList<SimpleStop> described in the method description.
	 */
	public static HashMap<Route, ArrayList<SimpleStop>> getNearestRoutes(Double lat, Double lon) {
		final int RADIUS_IN_METERS = RADIUS_IN_METERS_RAIL_SEARCH;
		
		//Get a list of the nearest route stop codes
		ArrayList<String> nearestStopCodes = new ArrayList<String>();
		
		String nearest_url = WMATA_RAIL_NEAREST_STATIONS_URL;
		nearest_url +="?lat=" + Double.toString(lat*10);
		nearest_url +="&lon=" + Double.toString(lon*10);
		nearest_url +="&radius=" + Integer.toString(RADIUS_IN_METERS);
		nearest_url += "&api_key=" + WMATA_APIKEY;
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(nearest_url);
			
		final int MAX_ENTRANCE_TO_RESOLVE = 10;
		
		try {
			JSONObject jsonObject = new JSONObject( jsonText ); 
			JSONArray jsonEntrancesArray = jsonObject.getJSONArray("Entrances");
			
			for(int i = 0; i < ((MAX_ENTRANCE_TO_RESOLVE<jsonEntrancesArray.length())?MAX_ENTRANCE_TO_RESOLVE:jsonEntrancesArray.length()); i++) {
				JSONObject stationEntrance = jsonEntrancesArray.getJSONObject(i);
								
				if(stationEntrance.has("StationCode1") && !stationEntrance.isNull("StationCode1")) {
					nearestStopCodes.add(stationEntrance.getString("StationCode1"));
				}
				
				if(stationEntrance.has("StationCode2") && !stationEntrance.isNull("StationCode2")) {
					nearestStopCodes.add(stationEntrance.getString("StationCode2"));
				}
			}
		} catch (JSONException e) {
			System.err.println(e);
			return null;
		}
		
		ArrayList<Route> allRoutes = fetchAgencyRouteList();
		HashMap<String, Route> routesNameToRoute = new HashMap<String, Route>();
		for(Route r : allRoutes) {
			routesNameToRoute.put(r.sName, r);
		}
		
		ArrayList<SimpleStop> allStops = fetchStopsByRoute(null, false);
		ArrayList<SimpleStop> nearStops = new ArrayList<SimpleStop>();
		
		for(SimpleStop s : allStops) {
			if(nearestStopCodes.contains(s.stopcode)) {
				nearStops.add(s);
			}
		}
		
		HashMap<Route, ArrayList<SimpleStop>> returned = new HashMap<Route, ArrayList<SimpleStop>>();
		
		for(SimpleStop s : nearStops) {
			String [] routeCodes = s.routeName.split(",");
			//Since we're done with the routecodes for the stop, replace them with human-readable route names
			fixSimpleStopRoutes(s, routesNameToRoute);
			
			for(int i = 0; i < routeCodes.length; i++) {
				String routeCode = routeCodes[i];
				if(routeCode.length() > 0 && routesNameToRoute.containsKey(routeCode)) {
					Route r = routesNameToRoute.get(routeCode);
					if(!returned.containsKey(r)) returned.put(r, new ArrayList<SimpleStop>());
					returned.get(r).add(s);
				}
			}
		}
		
		
		//Now, we get the nearest bus stops
		ArrayList<SimpleStop> newBusStop = getNearestStopsBus(lat, lon, false);
		HashMap<String, ArrayList<SimpleStop>> shortRouteToBusMap = new HashMap<String, ArrayList<SimpleStop>>();
		
		for(SimpleStop s : newBusStop) {
			Log.d(activitynametag, "Got the following near stops: (" + s.intersection + ", " + s.routeName + ")");
		}
		
		//If we didn't find any nearby stops return
		if(newBusStop == null || newBusStop.size() <= 0) return(returned);
		
		for(SimpleStop s : newBusStop) {
			String [] routesServed = s.routeName.split(",");
			
			for(String route : routesServed) {
				if(!shortRouteToBusMap.containsKey(route)) 
					shortRouteToBusMap.put(route, new ArrayList<SimpleStop>());
				
				shortRouteToBusMap.get(route).add(s);
			}
		}
		//Now let's translate the Route short names to Route objects and put that into returned
		HashSet<String> longRouteNamesSeen = new HashSet<String>(); //store the (human readable) route names we've seen
		for(String shortRouteName : shortRouteToBusMap.keySet()) {
			Route routeObj = getRouteNameFromSname(shortRouteName);
			
			if(routeObj != null && !longRouteNamesSeen.contains(routeObj.lName)) {
				returned.put(routeObj, shortRouteToBusMap.get(shortRouteName));
				longRouteNamesSeen.add(routeObj.lName);
			}
		}
			
		return(returned);
	}
	
	/** Return a list of the nearby stops, given a latitude and longitude. We will return Rail stops within
	 * RADIUS_IN_METERS_RAIL_SEARCH meters and bus stops within RADIUS_IN_METERS_BUS_SEARCH.
	 * 
	 * Assumes that lat and lon are actually /=10 from normal because for some reason
	 * Location.getLongitude()/getLatitude() return it that way.
	 * 
	 * @param lat The latitude of the requestor, given in degrees / 10 (because that's what Location.getLatitude() returns)
	 * @param lon The longitude of the requestor, given in degrees / 10 
	 * @return An ArrayList of SingleStop objects, representing nearby Rail and Bus stops.
	 */
	public static ArrayList<SimpleStop> getNearestStops(Double lat, Double lon) {
		ArrayList<SimpleStop> returned = new ArrayList<SimpleStop>();
		
		ArrayList<SimpleStop> nSR = getNearestStopsRail(lat, lon);
		if(nSR != null) for(SimpleStop s : nSR) returned.add(s);
		
		ArrayList<SimpleStop> nSB = getNearestStopsBus(lat, lon);
		if(nSB != null) for(SimpleStop s : nSB) returned.add(s);
		
		return returned;
	}
	
	/** Return a list of the nearby stops, given a latitude and longitude. We will return Rail stops within
	 * RADIUS_IN_METERS_RAIL_SEARCH meters.
	 * 
	 * Assumes that lat and lon are actually /=10 from normal because for some reason
	 * Location.getLongitude()/getLatitude() return it that way.
	 * 
	 * @param lat The latitude of the requestor, given in degrees / 10 (because that's what Location.getLatitude() returns)
	 * @param lon The longitude of the requestor, given in degrees / 10 
	 * @return An ArrayList of SingleStop objects, representing nearby Rail stops.
	 */
	public static ArrayList<SimpleStop> getNearestStopsRail(Double lat, Double lon) {
		//Get a list of the nearest route stop codes
		ArrayList<String> nearestStopCodes = new ArrayList<String>();
		
		String nearest_url = WMATA_RAIL_NEAREST_STATIONS_URL;
		nearest_url +="?lat=" + Double.toString(lat*10);
		nearest_url +="&lon=" + Double.toString(lon*10);
		nearest_url +="&radius=" + Integer.toString(RADIUS_IN_METERS_RAIL_SEARCH);
		nearest_url += "&api_key=" + WMATA_APIKEY;
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(nearest_url);
			
		try {
			JSONObject jsonObject = new JSONObject( jsonText ); 
			JSONArray jsonEntrancesArray = jsonObject.getJSONArray("Entrances");
			
			for(int i = 0; i < ((10<jsonEntrancesArray.length())?10:jsonEntrancesArray.length()); i++) {
				JSONObject stationEntrance = jsonEntrancesArray.getJSONObject(i);
								
				if(stationEntrance.has("StationCode1") && !stationEntrance.isNull("StationCode1")) {
					nearestStopCodes.add(stationEntrance.getString("StationCode1"));
				}
				
				if(stationEntrance.has("StationCode2") && !stationEntrance.isNull("StationCode2")) {
					nearestStopCodes.add(stationEntrance.getString("StationCode2"));
				}
			}
		} catch (JSONException e) {
			System.err.println(e);
			return null;
		}
		
		ArrayList<Route> allRoutes = fetchAgencyRouteList();
		HashMap<String, Route> routesNameToRoute = new HashMap<String, Route>();
		for(Route r : allRoutes) {
			routesNameToRoute.put(r.sName, r);
		}
		
		ArrayList<SimpleStop> allStops = fetchStopsByRoute(null);
		ArrayList<SimpleStop> nearStops = new ArrayList<SimpleStop>();
		
		for(SimpleStop s : allStops) {
			if(nearestStopCodes.contains(s.stopcode)) {
				//fixSimpleStopRoutes(s, routesNameToRoute);
				nearStops.add(s);
			}
		}
		
		return(nearStops);	
	}
	
	
	/** Return a list of the nearby stops, given a latitude and longitude. We will return Bus stops within
	 * RADIUS_IN_METERS_BUS_SEARCH meters.
	 * 
	 * Assumes that lat and lon are actually /=10 from normal because for some reason
	 * Location.getLongitude()/getLatitude() return it that way.
	 * 
	 * @param lat The latitude of the requestor, given in degrees / 10 (because that's what Location.getLatitude() returns)
	 * @param lon The longitude of the requestor, given in degrees / 10 
	 * @param fix_routes Whether or not to fix SimpleStops.routeName fields for the returned SimpleStops. Default to true. It
	 * is useful NOT to fix this and keep them as a comma-delimited list of RouteIds if some post-processing will be 
	 * done on the data and the RouteIds are necessary.
	 * @return An ArrayList of SingleStop objects, representing nearby Rail and Bus stops.
	 */
	public static ArrayList<SimpleStop> getNearestStopsBus(Double lat, Double lon) {
		return getNearestStopsBus(lat, lon, true);
	}
	public static ArrayList<SimpleStop> getNearestStopsBus(Double lat, Double lon, boolean fix_routes) {
		//Get a list of the nearest route stop codes
		ArrayList<SimpleStop> returned = null;
		
		String nearest_url = WMATA_BUS_NEAREST_STATIONS_URL;
		nearest_url +="?lat=" + Double.toString(lat*10);
		nearest_url +="&lon=" + Double.toString(lon*10);
		nearest_url +="&radius=" + Integer.toString(RADIUS_IN_METERS_BUS_SEARCH);
		nearest_url += "&api_key=" + WMATA_APIKEY;
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(nearest_url);
			
		try {
			JSONObject jsonObject = new JSONObject( jsonText ); 
			returned = jsonToSimpleStopsBus(jsonObject, fix_routes);
		} catch (JSONException e) {
			System.err.println(e);
			return null;
		}
		
		return(returned);
	}
	
	
	/** Main method is used for stand-alone testing. */
	public static void main(String [] args) {
		ArrayList<Route> routeList = fetchAgencyRouteList();
		
		for(Route r : routeList) {
			System.out.println("Route: (" + r.lName + ", " + r.sName + ")");
		}
		
		if(routeList.size() > 0) {
			ArrayList<SimpleStop> stopsOnFirst = fetchStopsByRoute(routeList.get(0));
			
			for(SimpleStop s : stopsOnFirst) {
				System.out.println("Stop: (" + s.intersection + "," + s.stopcode + "," + s.lat + "," + s.lon + "," + s.routeName + ")");
			}
			
			boolean not_printed_yet = true;
			int i = 0;
			while(not_printed_yet && i < stopsOnFirst.size()) {
				ArrayList<SimpleStop> predStops = fetchPredictionsByStop(stopsOnFirst.get(i));
				
				for(SimpleStop s : predStops) {
					System.out.println("Prediction: (" + s.routeName + "," + s.intersection + "," + s.headSign + "," + s.pred.toString() + ")");
				}
				
				if(predStops.size() > 0) not_printed_yet = false;
				
				i++;
			}
		}
		
		System.out.println("Routes nearest to center of DC:");
		HashMap<Route, ArrayList<SimpleStop>> nearestRoutes = getNearestRoutes(32.824552, -117.108978);
		for(Route key : nearestRoutes.keySet()) {
			System.out.print("Routes -> Stop : " + key.lName + " -> {" );
			
			ArrayList<SimpleStop> stops = nearestRoutes.get(key);
			
			for(SimpleStop s : stops) {
				System.out.print(s.intersection + ",");
			}
			
			System.out.println("}");
		}
	}
	
	/** Used to split up a SimpleStop having multiple, ';' delimited stop codes, into an
	 * ArrayList of SimpleStops */
	private static ArrayList<SimpleStop> splitStop(SimpleStop s) {
		String [] stopCodes = s.stopcode.split(";");
		ArrayList<SimpleStop> returned = new ArrayList<SimpleStop>(stopCodes.length);
		
		for(String new_stopcode : stopCodes) {
			SimpleStop newSimpleStop = new SimpleStop();
			
			//Clone the stop except use a single stop code
			newSimpleStop.agency = s.agency;
			newSimpleStop.routeName = s.routeName;
			newSimpleStop.dirName = s.dirName;
			newSimpleStop.headSign = s.headSign;
			newSimpleStop.intersection = s.intersection;
			newSimpleStop.diruse = s.diruse;
			newSimpleStop.direction = s.direction;
			newSimpleStop.table = s.table;
			newSimpleStop.isRTstr = s.isRTstr;
			newSimpleStop.pred = s.pred;
			newSimpleStop.lat = s.lat;
			newSimpleStop.lon = s.lon;
			
			newSimpleStop.stopcode = new_stopcode;
			
			returned.add(newSimpleStop);
		}
		
		return(returned);
	}
}
