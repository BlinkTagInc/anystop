package org.busbrothers.anystop.agencytoken.datacomponents;

import java.util.ArrayList;


public class SmartSort {

	//private static Log _log = new WriterLog();
	
	public static int compare(String basis, String compareTo, boolean letterFirst) {
		int rn1 = calcRouteSortOrderOnDrugs(basis, letterFirst);
		int rn2 = calcRouteSortOrderOnDrugs(compareTo, letterFirst);
		
		//_log.info(basis + ": " + rn1 + " and " + compareTo + ": " + rn2);
		
		if (rn1==rn2) return 0;
		if (rn1>rn2) return 1;
		if (rn1<rn2) return -1;
		
		return 0;		
	}
    /**
     * More ambitious calculation of order for showing routes in selection list.
     *
     * For routeIDs that start with a letter, order them by that letter, but allocate 
     *     1000 slots for numeric variations of it.  Hope that is enough for everybody.
     *     Then if there is a numeric part after the letter, add the value of that 
     *     number.  (If it's greater than 999, throw a warning!)
     *
     * For routeIDs that start with a number, order by that number's value, 
     *     AND allocate 26 slots for each such value for letter variations of 
     *     that number.  Then test for such letter suffixes and add their 
     *     value (sequence in 'A' to 'Z').
     *
     * ALSO, give caller option of list letter-first routes before OR after
     * the number-first routes.  
     * So, the algorithm:
     *
     *     IF letter-routes-first:
     *         Do the letter-routes
     *         Do the number-routes starting at 26000.
     *     ELSE:
     *         Do the number-routes
     *         Do the letter-routes starting at 26000
     *
     *     Finally do the special-tag routes by testing for special tags:
     *     IF routeID has the special tag, add 52000 to it.
     *
     * @param logger
     * @param routeID
     * @param orderLastTags List of strings marking route for going at end of list
     * @param letterRoutesFirst Whether letter-first routeID appear first or last
     * @return int value for sortOrder attribute of Route
     */
	private static int calcRouteSortOrderOnDrugs(String routeID, 
	       boolean letterRoutesFirst) {

	        // Implementation of plan above:
	        // set letterFirstRoutesBase = letterRoutesFirst? 0 : 26000;
	        // set numberFirstRoutesBase = letterRoutesFirst? 26000 : 0;
	        //
	        // IF routeID begins with a letter:
	        //     Get order for a letter-first route, starting at letterFirstRoutesBase
	        // ELSE
	        //     Get order for a number-first route, starting at numberFirstRoutesBase
	        //
	        // If routeID contains any string in orderLastTags, add 52000 to its result)
	        int letterFirstRoutesBase = letterRoutesFirst ? 0 : 26000;
	        int numberFirstRoutesBase = letterRoutesFirst ? 26000 : 0;

	        int result;
	        char firstCh = routeID.toUpperCase().charAt(0);
	        if (firstCh >= 'A' && firstCh <= 'Z') {
	            result = calcLetterFirstRouteOrder(routeID, letterFirstRoutesBase);
	        }
	        else {
	            result = calcNumberFirstRouteOrder(routeID, numberFirstRoutesBase);
	        }
	        return result;
	    }
	
    /*
     * For routeIDs that start with a letter, order them by that letter, but allocate 
     *     1000 slots for numeric variations of it.  Hope that is enough for everybody.
     *     Then if there is a numeric part after the letter, add the value of that 
     *     number.
     */
    private static int calcLetterFirstRouteOrder(String routeID, int base) {
        int result = 0;
        char firstCh = routeID.toUpperCase().charAt(0);
        result = firstCh - 'A';
        result *= 1000;
        int numericPartInt = getNumericPartOfRouteID(routeID);
        result += numericPartInt;
        return base + result;
    }
    
    private static int getNumericPartOfRouteID(String routeID) {
        int numericPartInt;
        
        String buff = routeID;
//        for (int i=32; i<48; i++) {
//        	buff=buff.replace((char)i, (char)0);
//        }
//        for (int i=58; i<127; i++) {
//        	buff=buff.replace((char)i, (char)0);
//        }
        StringBuffer b = new StringBuffer();
        for (int i=0; i<routeID.length(); i++) {
        	char c = routeID.charAt(i);
        	if (!(48 > c || c > 57 )) {
        		b.append(c);
        	}
        }
        buff=b.toString();
        
        //_log.info("numpart of " + routeID + " is " + buff + "|");
        
        try {
            numericPartInt = Integer.parseInt(buff);
        }
        catch (NumberFormatException nfe) {

        	//_log.info("NFE: " + nfe.getLocalizedMessage());
            numericPartInt = 0;
        }
        if (numericPartInt > 999) {

        }
        return numericPartInt;
    }
    
    /*
     * For routeIDs that start with a number, order by that number's value, AND 
     * allocate 26 slots for letter variants of that number.  
     * Then test for such letter suffixes and add their value (sequence in 'A' to 'Z').
     */
    private static int calcNumberFirstRouteOrder(String routeID, int base) {
        int result = 0;
        result = getNumericPartOfRouteID(routeID);

        // Leave room for 26 letter-variants for each number
        result *= 26;
        //_log.info(routeID + " result post-mult: " + result);
        
        // Then get alphabetic part of ID, consider only first letter for sorting
        // There *are* other ways to get a numeric value from strings for sorting, but
        // they would require lots more than 26 slots.
        // "\d" is regex for digit
        //String alphabeticPart = routeID.replaceAll("\\d", "");
        String buffer = routeID;
        for (int i=48; i<58; i++) {
        	buffer = buffer.replace((char)i, (char)0);
        }
        String alphabeticPart = buffer;
        //_log.info(routeID + " alphpart: " + alphabeticPart);
        
        if (alphabeticPart != null && alphabeticPart.length() > 0) {
            char firstCh = alphabeticPart.toUpperCase().charAt(0);
            result += firstCh - 'A';
        }
        //_log.info(routeID + " base returning: " + (base+result));
        return base + result;
    }
    
    
}
