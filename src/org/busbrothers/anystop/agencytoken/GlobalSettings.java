package org.busbrothers.anystop.agencytoken;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

public class GlobalSettings {
	
	private static final String activityNameTag = "GlobalSettings";
	
	private static ConcurrentHashMap <String, String> dataMap = new ConcurrentHashMap<String,String>();; //dataMap stores all of the setting key-value pairs
	private static boolean hasBeenSet = false; //This variable tells us whether we have had settings entered into
		//this object via processSettings()
	
	GlobalSettings() {
		dataMap = new ConcurrentHashMap<String,String>();
		
		dataMap.put("adsForScheduleApps", "addience");
		hasBeenSet = false; 
	}
	
	public static void processSettings(String newSettings) {
		String [] lines = newSettings.split("\n");
		
		for(int count = 0; count < lines.length; count++) {
			String current = lines[count];
			String [] tokens = current.split(":"); //Tokens will be key-value pairs from
				//server-side settings file
			
			if(tokens.length != 2) continue;
			if(current.charAt(0) == '#') continue;
			
			if(dataMap.containsKey(tokens[0])) {
				dataMap.remove(tokens[0]);
				dataMap.put(tokens[0], tokens[1]); 
			}
			else
				dataMap.put(tokens[0], tokens[1]); 
			
			Log.v(activityNameTag, "GlobalSettings added key-value pair (" + tokens[0] + ", " + tokens[1] + ")");
		}
		
		hasBeenSet = true;
	}
	
	public static String getSetting(String settingName) {
		return(dataMap.get(settingName));
	}
	
	/** This method tells us what ad agency to use. */
	public static String getAdTypeForScheduleApps() {
		return dataMap.get("adsForScheduleApps");
	}
	
	public static boolean hasBeenSet() {
		return(hasBeenSet);
	}
}
