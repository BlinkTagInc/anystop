package org.busbrothers.anystop.agencytoken;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.busbrothers.anystop.agencytoken.datacomponents.Agency;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.Prediction;
import org.busbrothers.anystop.agencytoken.datacomponents.Route;
import org.busbrothers.anystop.agencytoken.datacomponents.SimpleStop;
import org.busbrothers.anystop.agencytoken.parser.SimpleDOMParser;
import org.busbrothers.anystop.agencytoken.parser.SimpleElement;

import android.util.Log;

import com.google.android.maps.GeoPoint;

public class MakeStops {

	private static ArrayList<SimpleStop> stops;
	private static ArrayList<Agency> agencies;
	private static ArrayList<Route> agencyRoutes;
	
	public static ArrayList<SimpleStop> make(String xml) throws NoneFoundException {
		stops = new ArrayList<SimpleStop>();
		SimpleDOMParser d = new SimpleDOMParser();
		Reader r = new StringReader(xml);
		SimpleElement e = null;
		try {
			e = d.parse(r);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (e.getTagName().equalsIgnoreCase("error")) {
			String errortext=e.getText();
			if (errortext.equalsIgnoreCase("NoneFound")) {
				throw new NoneFoundException();
			}
			Manager.currError = errortext;
		}
		ArrayList<SimpleElement> stopElements = e.getChildElements();
		for (SimpleElement stop : stopElements) {
			
			if (stop.getTagName().equalsIgnoreCase("message")) {
				String errortext=stop.getText();
				Manager.currError = errortext;
			}
			if (stop.getTagName().equalsIgnoreCase("stop")) {
				SimpleStop s = makeStop(stop);
				stops.add(s);
			}
			if (stop.getTagName().equalsIgnoreCase("text")) {
				Manager.messageQueue.push(stop.getText());
				continue;
			}
		}
		return stops;
	}
	
	private static SimpleStop makeStop(SimpleElement stopElement) {
		SimpleStop stop = new SimpleStop();
		ArrayList<SimpleElement> stopParts = stopElement.getChildElements();
		HashMap<String, String> attr = new HashMap<String, String>();
		for (SimpleElement element: stopParts) {
			attr.put(element.getTagName(), element.getText());
		}
		
		stop.pred = makePred(attr.get("predictions"), attr.get("isRealPrediction"));
			stop.lat = Integer.parseInt(attr.get("latitude"));
			stop.lon = Integer.parseInt(attr.get("longitude"));
		stop.agency = attr.get("agencyName");
		stop.dirName = attr.get("directionName");
		stop.headSign = attr.get("headsign");
		//stop.intersection = attr.get("intersection").replace(" and ", " & ").replace(" St ", " ").trim();
		//stop.routeName = attr.get("routeName").replace('.', '-').replace('%', '/').trim();
		stop.intersection = attr.get("intersection");
		stop.routeName = attr.get("routeName");
		stop.stopcode = attr.get("stopcode");
		stop.diruse = attr.get("diruse");
		stop.isRTstr = attr.get("predictionType");
		stop.table = (attr.get("tableName")==null) ? Manager.tableTracker : attr.get("tableName");
		
		return stop;
	}
	
	private static GeoPoint makePoint(String latstr, String lonstr) {
		// TODO Auto-generated method stub
		int lat = Integer.parseInt(latstr);
		int lon = Integer.parseInt(lonstr);
		GeoPoint g = new GeoPoint(lat,lon);
		return g;
	}

	private static Prediction makePred(String predStr, String isRTstr) {
		String[] predsSplit;
		int[] preds = null;
		if (predStr!=null && !predStr.equalsIgnoreCase("")) {
			predsSplit = predStr.split(",");
			preds = new int[predsSplit.length];
			for (int i=0; i<predsSplit.length; i++) {
				preds[i] = Integer.parseInt(predsSplit[i]);
			}
		} else {
			preds = new int[0];
		}
		boolean isReal = Boolean.parseBoolean(isRTstr);
		return new Prediction(preds, isReal);
	}

	public static ArrayList<Agency> makeAgencies(String xml) throws NoneFoundException {
		agencies = new ArrayList<Agency>();
		SimpleDOMParser d = new SimpleDOMParser();
		Reader r = new StringReader(xml);
		SimpleElement e = null;
		try {
			e = d.parse(r);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (e.getTagName().equalsIgnoreCase("error")) {
			String errortext=e.getText();
			if (errortext.equalsIgnoreCase("NoneFound")) {
				throw new NoneFoundException();
			}
			Manager.currError = errortext;
		}
		Manager.messageTracker = null;
		ArrayList<SimpleElement> agencyElements = e.getChildElements();
		for (SimpleElement agency : agencyElements) {
			if (agency.getTagName().equalsIgnoreCase("text")) {
				Manager.messageQueue.push(agency.getText());
				continue;
			}
			
			if (agency.getTagName().equalsIgnoreCase("agencyDetails")) {
				Agency a = makeAgency(agency);
				agencies.add(a);
			}
		}
		return agencies;
	}

	private static Agency makeAgency(SimpleElement agencyElement) {
		Agency agency = new Agency();
		ArrayList<SimpleElement> agencyParts = agencyElement.getChildElements();
		HashMap<String, String> attr = new HashMap<String, String>();
		for (SimpleElement element: agencyParts) {
			attr.put(element.getTagName(), element.getText());
		}

		agency.isRT = attr.get("predictionType").equalsIgnoreCase("real");
		agency.isSpecial = attr.get("predictionType").equalsIgnoreCase("special");
		agency.isRTstr = attr.get("predictionType");
		agency.name = attr.get("agencyName");
		agency.table = attr.get("tableName");
		agency.state = attr.get("state");
		agency.city = attr.get("city");
		
		return agency;
	}

	public static ArrayList<Route> makeAgencyRoutes(String xml) {
		agencyRoutes = new ArrayList<Route>();
		SimpleDOMParser d = new SimpleDOMParser();
		Reader r = new StringReader(xml);
		SimpleElement e = null;
		try {
			e = d.parse(r);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<SimpleElement> agencyRouteElements = e.getChildElements();
		for (SimpleElement agencyRoute : agencyRouteElements) {
			if (agencyRoute.getTagName().equalsIgnoreCase("routeDetails")) {
				Route a = makeRoute(agencyRoute);			
				agencyRoutes.add(a);
			}
			if (agencyRoute.getTagName().equalsIgnoreCase("text")) {
				Manager.messageQueue.push(agencyRoute.getText());
				continue;
			}
		}
		return agencyRoutes;
	}

	private static Route makeRoute(SimpleElement agencyRouteE) {
		Route route = new Route();
		ArrayList<SimpleElement> routeParts = agencyRouteE.getChildElements();
		HashMap<String, String> attr = new HashMap<String, String>();
		for (SimpleElement element: routeParts) {
			attr.put(element.getTagName(), element.getText());
		}

		route.sName = attr.get("shortName");
		route.lName = attr.get("routeName");
		route.isRT = attr.get("predictionType").equalsIgnoreCase("real") ? true : false;
		
		return route;
	}
}
