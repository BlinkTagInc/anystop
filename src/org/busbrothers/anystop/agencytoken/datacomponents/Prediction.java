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
	public Prediction(int[] predsSecs, boolean isRT) {
		super();
		Date now = new Date();
		this.predsSecs = new ArrayList<Long>();
		for (int p : predsSecs) {
			this.predsSecs.add(now.getTime()+p*60*1000);
		}
		this.isRT = isRT;
	}
	
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
