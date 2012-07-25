package org.busbrothers.anystop.agencytoken.datacomponents;

import java.io.Serializable;

import org.busbrothers.anystop.agencytoken.Manager;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

/** SimpleStop implements an object representing a single transit stop. It is a Comparable, Serializable object. It stores a variety
 * of data about the stop, including the name of the route that it is on, what intersection it is on, and other String information. Also
 * it stores prediction (pred) and the lat/lon physical location of the stop.
 * @author Anonymous
 *
 */

public class SimpleStop implements Comparable<SimpleStop>, Serializable {

	public String routeName, dirName, headSign, intersection, agency, diruse, direction, table, isRTstr;
	public String stopcode; /**< This variable is used for real-time feeds, so the app knows what Nextbus URL to go to – to get the real-time transit arrivals.
		Each stop has a unique URL to access the real-time arrival information It's only used in the database anystop_realtime (not schedule). 
		I forget the exact name of the databases.*/
	public Prediction pred; //!< Prediction associated with this stop.  
	public int lat, lon;
	
	public String getTo() {
		if (diruse.equalsIgnoreCase("headsign")) {
			return headSign;
		} else if (diruse.equalsIgnoreCase("direction")) {
			return direction;
		} else {
			return null;
		}
	}
	
	
	
	/** This function will check whether this SimpleStop object contains the String 'key' in one of it's searched fields. The searched fields
	 * are:
	 * 
	 * 1. routeName
	 * 2. dirName
	 * 3. intersection
	 * 4. direction
	 * 5. stopcode
	 * 
	 *  This function is intended to make searching a data structure of SimpleStop objects, looking for String key, useful.
	 * @param key The String which we will search for, in the searchable fields.
	 * @return boolean representing whether key is contained in *any* of the searchable fields.
	 */
	public boolean containsForSearch(String key) {
		boolean returned = false;
		
		if(key == null) return(false);
		
		String keyi = new String(key.toLowerCase());
		
		if(routeName != null) returned = returned || routeName.toLowerCase().contains(keyi);
		if(dirName != null) returned = returned || dirName.toLowerCase().contains(keyi);
		if(intersection != null) returned = returned || intersection.toLowerCase().contains(keyi);
		if(direction != null) returned = returned || direction.toLowerCase().contains(keyi);
		if(stopcode != null) returned = returned || stopcode.toLowerCase().contains(keyi);
		
		return(returned);
	}

	
	/** The compareTo function compares this SimpleStop to another, either by intersection or by routeName. If we are in the ROUTE
	 * 	view, we compare by intersection (using SmartSort's compare method). Otherwise, we compare by routeName.
	 * @see SmartSort
	 */
	public int compareTo(SimpleStop another) {
		if (Manager.viewing==Manager.ROUTES || Manager.viewing==Manager.FAVSTOPS) {
			return SmartSort.compare(this.intersection, another.intersection, true);
		} else {
			//return SmartSort.compare(this.intersection, another.intersection, true);
			return SmartSort.compare(this.routeName, another.routeName, true);
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agency == null) ? 0 : agency.hashCode());
		result = prime * result + ((dirName == null) ? 0 : dirName.hashCode());
		result = prime * result
				+ ((direction == null) ? 0 : direction.hashCode());
		result = prime * result + ((diruse == null) ? 0 : diruse.hashCode());
		result = prime * result
				+ ((headSign == null) ? 0 : headSign.hashCode());
		result = prime * result
				+ ((intersection == null) ? 0 : intersection.hashCode());
		result = prime * result
				+ ((routeName == null) ? 0 : routeName.hashCode());
		result = prime * result
				+ ((stopcode == null) ? 0 : stopcode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleStop other = (SimpleStop) obj;
		if (agency == null) {
			if (other.agency != null)
				return false;
		} else if (!agency.equals(other.agency))
			return false;
		if (dirName == null) {
			if (other.dirName != null)
				return false;
		} else if (!dirName.equals(other.dirName))
			return false;
		if (direction == null) {
			if (other.direction != null)
				return false;
		} else if (!direction.equals(other.direction))
			return false;
		if (diruse == null) {
			if (other.diruse != null)
				return false;
		} else if (!diruse.equals(other.diruse))
			return false;
		if (headSign == null) {
			if (other.headSign != null)
				return false;
		} else if (!headSign.equals(other.headSign))
			return false;
		if (intersection == null) {
			if (other.intersection != null)
				return false;
		} else if (!intersection.equals(other.intersection))
			return false;
		if (routeName == null) {
			if (other.routeName != null)
				return false;
		} else if (!routeName.equals(other.routeName))
			return false;
		if (stopcode == null) {
			if (other.stopcode != null)
				return false;
		} else if (!stopcode.equals(other.stopcode))
			return false;
		return true;
	}
}
