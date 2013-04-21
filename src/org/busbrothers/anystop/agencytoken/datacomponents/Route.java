package org.busbrothers.anystop.agencytoken.datacomponents;
import android.util.Log;

public class Route implements Comparable<Route> {
	public String sName; //!< sName is the string that gets displayed when we for example list routes.
	public String lName; //!< lName is a string that gets used to query a Route from the Manager class, basically it is used to identify a unique Route.
	public boolean isRT;
	
	/** Clone returns a (deep) copy of this Route.
	 * @return A deep copy of the Route we call this on.
	 */
	public Route clone() {
		Route returned = new Route();
		
		returned.sName = new String(sName);
		returned.lName = new String(lName);
		returned.isRT = isRT;
		
		return(returned);
		
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof Route)) return false;
		
		Route otherRoute = (Route) other;
		if(otherRoute.sName.equals(sName) && 
				otherRoute.lName.equals(lName)) return true;
		
		return false;
	}

	/** compareTo() compares two Routes. This is done using the compareToForRouteName() method. A description of the algorithm 
	 * used is available in the header (method description?) of compareToForRouteName().
	 * @param another The other Route to compare this to.
	 * @return 1 if this route is "greater" lexicographically than the other, 0 if they are the same, -1 if this route is "less" than the other
	 */
	public int compareTo(Route another) {
		//return this.sName.compareTo(another.sName);
		int retval;
		
		if(this.lName == null) return -1;
		
		//No reason to expect compareToForRouteName() to fail, but just in case...
		try {
			retval = compareToForRouteName(this, another);
			
		}
		catch (Exception e) {
			Log.e("Route", "compareTo() got error for smart sort, reverting to classic sort.");
			
			if(another.lName == null) return -1;
			retval = lName.compareTo(another.lName);
		}
		
		return(retval);
	}
	
	/** This function will compare two Route Names (Strings) based on a sorting function. 
	* This function is defined as follows: 
	*	1. If both strings have integers, sort by that, then by rORN.
	*	2. If only one has integer, non-integer is greater.
	*	3. If one is blank, it is greater.
	*	4. If both are blank, they are equal.
	*	5. If neither have integers, sort by rORN.
	*	
	* rORN is the rest of the route name after the leading non-alphanumeric-symbols and 
	* leading integer are stripped away. 
	* @param one The first route to compare.
	* @param one The second route to compare.
	* @return -1 if one<two, 0 if one==two, 1 if one>two
	*/
	
	public static int compareToForRouteName(Route one, Route two) {
		return(compareToForRouteName(one.lName, two.lName));
	}

	public static int compareToForRouteName(String one, String two) {
		//Strip non-alphanumeric leading symbols in each route name
		String routeNameOne = stripNonAlphaNum(one);
		String routeNameTwo = stripNonAlphaNum(two);
		
		//Sorting algorithm rules
		//	1. If both strings have integers, sort by that, then by rORN.
		int routeNumberOne = getLeadingInteger(routeNameOne);
		int routeNumberTwo = getLeadingInteger(routeNameTwo);
		if(routeNumberOne != -1 && routeNumberTwo != -1) {
			if(routeNumberOne > routeNumberTwo) return 1;
			else if(routeNumberOne < routeNumberTwo) return -1;
			else {
				String restOfRouteNameOne = stripLeadingInteger(routeNameOne);
				String restOfRouteNameTwo = stripLeadingInteger(routeNameTwo);
				return(restOfRouteNameOne.compareTo(restOfRouteNameTwo));
			}
		}
		//	2. If only one has integer, non-integer is greater.
		else if(routeNumberOne == -1 && routeNumberTwo != -1) return(1);
		else if(routeNumberOne != -1 && routeNumberTwo == -1) return(-1);
		
		//	2.1 If both start with the same non-integer, sort by trailing integer
		String routeNumberOneLead = stripTrailingInteger(one);
		String routeNumberTwoLead = stripTrailingInteger(two);
		if(routeNumberOneLead.equals(routeNumberTwoLead)) {
			int tail_one = getTrailingInteger(one);
			int tail_two = getTrailingInteger(two);
			
			if(tail_one > tail_two) return 1;
			else if(tail_one < tail_two) return -1;
			else return(routeNameOne.compareTo(routeNameTwo));
		}
		
		//	3. If both are blank, they are equal.
		if(routeNameOne.equals("") && routeNameTwo.equals("")) return(0);
		//	4. If one is blank, it is greater.
		if(routeNameOne.equals("") && (!routeNameTwo.equals(""))) return(1);
		if((!routeNameOne.equals("")) && routeNameTwo.equals("")) return(-1);
		//	5. If neither have integers, sort by rORN.
		return(routeNameOne.compareTo(routeNameTwo));
	}
	
	/** This function returns the given string, with all leading non-alpha-numeric characters stripped off
	 * 
	 * @param input The string to strip leading non-alpha-numeric characters from.
	 * @return The input string, with non-alpha-numeric things stripped off.
	 */
	public static String stripNonAlphaNum(String input) {
		int index = 0;
		while(index < input.length()) {
			String currCharacter = input.substring(index, index+1);
			//if(currCharacter.matches("[a-z]") || currCharacter.matches("[A-Z]") || currCharacter.matches("[0-9]"))
			char curr_char = currCharacter.toCharArray()[0];
			if( ((curr_char >= 'A') && (curr_char <= 'Z')) || 
				((curr_char >= 'a') && (curr_char <= 'z')) || 
				((curr_char >= '0') && (curr_char <= '9')) ) {
				return(input.substring(index));
			}
			else index++;
		}
		
		return(new String(""));
	}
	
	/**
	 * This function returns an int which is the value represented by the leading digits of an input string. If the 
	 * input has no leading digits we return -1.
	 * @param input The input string to parse the leading digits from.
	 * @return An int equal to the value represented by the leading digit characters of input.
	 */
	public static int getLeadingInteger(String input) {
		String integerString = new String("");
		
		int index = 0;
		
		//We continue the loop so long as we do not hit the end of input
		//and so long as the current character is numeric
		while(index < input.length()) {
			String currCharacter = input.substring(index, index+1);
			char curr_char = currCharacter.toCharArray()[0];
			if( ((curr_char >= '0') && (curr_char <= '9')) ) {
			//if(currCharacter.matches("[0-9]")) {
				integerString = integerString + currCharacter;
				index++;
			}
			else break;
		}
		
		if(integerString.length() > 0) return Integer.parseInt(integerString);
		else return -1;
	}
	
	public static int getTrailingInteger(String input) {
		String integerString = new String("");
		
		int index = input.length()-1;
		
		//We continue the loop so long as we do not hit the end of input
		//and so long as the current character is numeric
		while(index >= 0) {
			String currCharacter = input.substring(index, index+1);
			char curr_char = currCharacter.toCharArray()[0];
			if( ((curr_char >= '0') && (curr_char <= '9')) ) {
			//if(currCharacter.matches("[0-9]")) {
				integerString = currCharacter + integerString;
				index--;
			}
			else break;
		}
		
		if(integerString.length() > 0) return Integer.parseInt(integerString);
		else return -1;
	}
	
	/** This function strips the leading digits from the input string and returns the balance.
	 * 
	 * @param input The string to strip the leading digits from.
	 * @return The input sring with leading digits stripped.
	 */
	public static String stripLeadingInteger(String input) {
		int index = 0;
		
		//We continue the loop so long as we do not hit the end of input
		//and so long as the current character is numeric
		while(index < input.length()) {
			String currCharacter = input.substring(index, index+1);
			
			//if(currCharacter.matches("[0-9]")) {
			char curr_char = currCharacter.toCharArray()[0];
			if( ((curr_char >= '0') && (curr_char <= '9')) ) {
				index++;
			}
			else break;
		}
		
		return(input.substring(index));
	}
	
	public static String stripTrailingInteger(String input) {
		int index = input.length() - 1;
		
		//We continue the loop so long as we do not hit the end of input
		//and so long as the current character is numeric
		while(index >= 0) {
			String currCharacter = input.substring(index, index+1);
			
			//if(currCharacter.matches("[0-9]")) {
			char curr_char = currCharacter.toCharArray()[0];
			if( ((curr_char >= '0') && (curr_char <= '9')) ) {
				index--;
			}
			else break;
		}
		
		return(input.substring(0, index+1));
	}
	
	/*public int compareTo(Route another) {
		return SmartSort.compare(this.sName, another.sName, true);
		
	}*/
}
