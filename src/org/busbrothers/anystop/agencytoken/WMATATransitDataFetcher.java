package org.busbrothers.anystop.agencytoken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
	
	private static final String WMATA_APIKEY="7j6wqja5t48cfkt2hw33ugp8";
	
	private static final String activitynametag = "WMATATransitDataFetcher";
	
	//Stores a mapping of route code to Route object, for filling in route lists
	//in SimpleStop objects
	private static HashMap<String, Route> routesNameMap;
	
	public static void reset() {
		routesNameMap = null;
	}
	
	private static void fillInRoutesNameMap() {
		routesNameMap = new HashMap<String, Route>();
		
		ArrayList<Route> allRoutes = fetchAgencyRouteList();
		
		for(Route r : allRoutes)
			routesNameMap.put(r.sName, r);
	}
	
	private static void fixSimpleStopRoutes(ArrayList<SimpleStop> sl) {
		if(routesNameMap == null) {
			fillInRoutesNameMap();
		}
		
		for(SimpleStop s : sl)
			fixSimpleStopRoutes(s, routesNameMap);
	}
	
	private static void fixSimpleStopRoutes(SimpleStop s) {
		if(routesNameMap == null) {
			fillInRoutesNameMap();
		}
		
		fixSimpleStopRoutes(s, routesNameMap);
	}
	
	/** Stupid helper method. Takes a SimpleStop and transforms the route, which is a 
	 * comma-delimited string of route codes, into a comma-delimited string of long
	 * route names (requires being given a Route.sName -> Route map)
	 * @param routeObject
	 * @return
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
	
	private static Route jsonToRoute(JSONObject routeObject) {
		Route returned = new Route();
		
		try {
			if(routeObject.has("DisplayName")) returned.lName = routeObject.getString("DisplayName");
			if(routeObject.has("LineCode")) returned.sName = routeObject.getString("LineCode");
		} catch (JSONException e) {
			System.err.println(e);
		}
		
		returned.isRT = USING_RT_FEED;
		
		return(returned);
	}
	
	private static SimpleStop jsonToSimpleStop(JSONObject stopObject) {
		return jsonToSimpleStop(stopObject, true);
	}
	private static SimpleStop jsonToSimpleStop(JSONObject stopObject, boolean do_fix_routes) {
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
	
	private static ArrayList<SimpleStop> jsonToPrediction(SimpleStop predStop, JSONArray predictionArray) {
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
			
			Log.d(activitynametag, "Adding another stop to returned: " + currStop.routeName + ", " + currStop.headSign + ", " + currStop.intersection);
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
				try { newRoute = jsonToRoute(jsonRouteObjectArray.getJSONObject(i)); }
				catch (JSONException e) { System.err.println(e); newRoute = null; }
				
				if(newRoute != null) routeList.add(newRoute);
			}
		} else {
			System.out.println("No objects in jsonRouteObjectArray! Terminating...");
		}
		
		//Fill in route name cache since it's convenient to do so now
		if(routesNameMap == null) {
			routesNameMap = new HashMap<String, Route>();
			for(Route r : routeList)
				routesNameMap.put(r.sName, r);
		}
		
		return(routeList);		
	}
	
	/** returns a list of all stops/stations for a given route/line */
	public static ArrayList<SimpleStop> fetchStopsByRoute(Route r) {
		return fetchStopsByRoute(r, true);
	}
	public static ArrayList<SimpleStop> fetchStopsByRoute(Route r, boolean do_fix_routes) {
		String url;
		
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
			jsonStopObjectArray = (JSONArray) jsonResult.getJSONArray("Stations");
		} catch (JSONException e) {System.err.println(e);}
		
		//Convert JSONArray to ArrayList of Route objects
		ArrayList <SimpleStop> stopList = new ArrayList<SimpleStop>();
		if(jsonStopObjectArray != null) {
			for(int i = 0; i < jsonStopObjectArray.length(); i++) {
				//Build a route object from the JSon object
				SimpleStop newStop;				
				try { newStop = jsonToSimpleStop(jsonStopObjectArray.getJSONObject(i), do_fix_routes); }
				catch (JSONException e) { System.err.println(e); newStop = null; }
				
				if(newStop != null) stopList.add(newStop);
			}
		} else {
			System.out.println("No objects in jsonRouteObjectArray! Terminating...");
		}

		return(stopList);
	}
	
	/** Return predictions for arrival times for a given SimpleStop. In fact we will 
	 * return multiple SimpleStop objects, since we have no better way of expressing the multiple
	 * route and headsign combinations that Predictions will probably return */
	public static ArrayList<SimpleStop> fetchPredictionsByStop(SimpleStop s) {
		String url = WMATA_RAIL_PREDICTIONS_URL 
				+ s.stopcode
				+ "?api_key=" + WMATA_APIKEY;
		
		String jsonText = APIEndpointAccessUtils.jsonStringFromURLString(url);
		
		JSONObject jsonResult = null;
		JSONArray jsonPredictionObjectArray = null;
		try {
			jsonResult = new JSONObject( jsonText ); 
			jsonPredictionObjectArray = (JSONArray) jsonResult.getJSONArray("Trains");
		} catch (JSONException e) {System.err.println(e);}
				
		return(jsonToPrediction(s, jsonPredictionObjectArray));
	}
	
	public static ArrayList<SimpleStop> fetchPredictionsByStops(ArrayList<SimpleStop> stops) {
		ArrayList<SimpleStop> retval = new ArrayList<SimpleStop>();
		
		for(SimpleStop s : stops) {
			ArrayList<SimpleStop> newStops = fetchPredictionsByStop(s);
			
			for(SimpleStop t : newStops) {
				retval.add(t);
			}
		}
		
		return retval;
	}
	
	public static HashMap<Route, ArrayList<SimpleStop>> getNearestRoutes(Double lat, Double lon) {
		final int RADIUS_IN_METERS = 2000;
		
		//Get a list of the nearest route stop codes
		ArrayList<String> nearestStopCodes = new ArrayList<String>();
		
		Log.d(activitynametag, "Nearest routes got params: " + lat + ", " + lon);
		
		String nearest_url = WMATA_RAIL_NEAREST_STATIONS_URL;
		nearest_url +="?lat=" + Double.toString(lat*10);
		nearest_url +="&lon=" + Double.toString(lon*10);
		nearest_url +="&radius=" + Integer.toString(RADIUS_IN_METERS);
		nearest_url += "&api_key=" + WMATA_APIKEY;
		
		Log.d(activitynametag, "Fetching nearest routes with URL: " + nearest_url);
		
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
			
		return(returned);		
	}
	
	/** Assumes that lat and lon are actually /=10 from normal because for some reason
	 * Location.getLongitude()/getLatitude() return it that way.
	 * @param lat
	 * @param lon
	 * @return
	 */
	public static ArrayList<SimpleStop> getNearestStops(Double lat, Double lon) {
		final int RADIUS_IN_METERS = 2000;
		
		//Get a list of the nearest route stop codes
		ArrayList<String> nearestStopCodes = new ArrayList<String>();;
		
		String nearest_url = WMATA_RAIL_NEAREST_STATIONS_URL;
		nearest_url +="?lat=" + Double.toString(lat*10);
		nearest_url +="&lon=" + Double.toString(lon*10);
		nearest_url +="&radius=" + Integer.toString(RADIUS_IN_METERS);
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
}
