/** This class implements a storage container for transit predictions. Generally, a prediction
 * is associated with a particular stop or other transit terminal. The predicted transit arrival
 * timer are stored in the data structure Prediction.predSecs, where each Long Object indicates
 * the predicted arrival time of a transit vehicle (bus, train, tram, etc). The arrival times
 * are measured in seconds; the server returns in "seconds from now" and we add the current 
 * time based on Date.getTime().
 * 
 */

package org.busbrothers.anystop.agencytoken.datacomponents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.busbrothers.anystop.agencytoken.Manager;

public class Prediction implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9035403481950432311L;
	public ArrayList<Long> predsSecs;
	public boolean isRT;
	
	/** This constructor creates the Prediction object.
	 * 
	 * @param predsSecs Predicted transit arrival times, given in seconds from 
	 * now
	 * @param isRT Whether this prediction is real time or not (has no effect on
	 * the stored data.
	 */
	public Prediction(int[] predsSecs, boolean isRT) {
		super();
		Date now = new Date();
		this.predsSecs = new ArrayList<Long>();
		
		for (int p : predsSecs) {
			this.predsSecs.add(now.getTime()+p*60*1000);
		}
		this.isRT = isRT;
	}
	
	/** Return a List of Strings representing this Prediction object. Each value
	 * in the List lists a particular arrival time; the value is given both in 
	 * relative minutes and an an absolute time.
	 * @return List of Strings representing predicted arrival times of a transit
	 * vehicle at a particular transit terminal.
	 */
	public ArrayList<String> format() {
		ArrayList<String> str = new ArrayList<String>();
		Date d = new Date();
		Calendar c = new GregorianCalendar();
		String time = null;
		for (Long i : predsSecs) {
			StringBuilder s = new StringBuilder("");
			s.append((int)(d.getTime()-i)/-60/1000 + " min - ");
			c.setTime(new Date(i));
			time=(c.get(Calendar.HOUR)==0?12:c.get(Calendar.HOUR)) + ":" + Manager.twoform(c.get(Calendar.MINUTE)+"") + " " + (c.get(Calendar.AM_PM)==0?"AM":"PM");
			s.append(time);
			str.add(s.toString());
		}
		return str;
	}
}
