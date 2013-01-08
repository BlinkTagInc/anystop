package org.busbrothers.anystop.agencytoken;

import org.busbrothers.anystop.agencytoken.datacomponents.Route;

public class Utils {
	
	public static String capFirst(String inputWord) {
		return inputWord.substring(0,1).toUpperCase() + inputWord.substring(1);
	}
	public static String replaceUnder(String inputWord) {
		return inputWord.replace("_", " ");
	}
	
	public static String fmt(String inputWord) {
		return replaceUnder(capFirst(inputWord));
	}
	
	/**This function is used to re-format a headsign. 
	 * We remove a leading "to " if it is present.
	 * @param headsignWord The input headsign
	 * @return Output headsign, without a leading "to"
	 */
	public static String fmtHeadsign(String headsignWord) {
		if(headsignWord == null) return "";
		
		if(headsignWord.startsWith("To ") || headsignWord.startsWith("to ")) {
			return headsignWord.substring(3);
		}
		else return headsignWord;
	}
	
	/** This function is used to remove a headsign entirely, if the headsign is equal to
	 * "no direction specified"
	 * @param hs the headsign we read from a SimpleStop object
	 * @return "" if the headsign was "no direction specified" else the headsign itself
	 */
	public static String checkHeadsign(String hs) {
		if(hs == null) return "";
		
		if(hs.toLowerCase().contains("no direction specified")) return "";
		else return hs;
	}
	
	/**This function compares two Route names for "equality". Since the feed server sometimes adds extra '-'
	 * characters to the end of a route name, this function will ignore that character for purposes of comparison.
	 * @return true if route names match w/o consideration of trailing '-', else false
	 */
	public static boolean routeNamesEqual(Route one, Route two) {
		//Usually sName and lName will be the same but just in case there is a discrepancy
		return(routeNamesEqual(one.sName, two.sName) || routeNamesEqual(one.lName, two.lName));
	}
	
	public static boolean routeNamesEqual(String one, String two) {
		String one_stripped = routeStripTrailing(one);
		String two_stripped = routeStripTrailing(two);
		
		return(one_stripped.equals(two_stripped));
	}
	
	public static String routeStripTrailing(String input) {
		String returned = input;		
		while(returned.length() > 0 && returned.endsWith("-")) returned=returned.substring(0, returned.length()-1);
		
		return(returned);
	}
	
	/**This methods returns the leading digits of a given string. If the string does not begin with a digit, it returns
	 * the input string itself. For example input "37 - Banana" should return the strin "37". The input "pineapple88" will 
	 * return "pineapple 88".
	 * @param input The input string, whose digit we will return.
	 * @return The leading digits of the input string, or the string itself if input does not begin with a digit.
	 */
	public static String getLeadingDigits(String input) {
		String returned = new String();
		
		if(input.charAt(0) < '0' || input.charAt(0) > '9') return(input);
		
		for(int current_index = 0; current_index < input.length(); current_index++) {
			if(input.charAt(current_index) >= '0' && input.charAt(current_index) <= '9')
				returned = returned + input.charAt(current_index);
			else
				return(returned);
		}
		
		return(returned);
	}
	
	public static String useless() {
		return "hi";
	}
}
