package org.busbrothers.anystop.agencytoken.datacomponents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.Log;

public class Favorites implements Serializable {	
	private static final long serialVersionUID = 8237238462726170234L;
	
	private Set<SimpleStop> favStops = new HashSet<SimpleStop>();
	private transient Set<String> favStopsStr= new HashSet<String>();
	
	private Set<String> favRoutes = new HashSet<String>(); //!< Favorite Routes get stored as Strings. We store the Route.lName for each Route.
	
	private Set<Agency> favAgencies= new HashSet<Agency>();
	
	private static final transient String filename="AnyStopFaves";
	private static transient Favorites instance = null;
	public static transient Context context;
	
	private Favorites() {}
	
	public static Favorites getInstance() {
		if (instance==null) {
			instance = create();
		}
		if (instance==null) {
			instance = new Favorites();
		}
		
		return instance;
	}
	
	private static Favorites create() {
		Favorites favs = null;
		
			FileInputStream fis = null;
			ObjectInputStream in = null;
		try {
			fis = context.openFileInput(filename);
			in = new ObjectInputStream(fis);
			favs = (Favorites) in.readObject();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		if (favs!=null) {
			favs.favStopsStr = new HashSet<String>();
			for (SimpleStop stop : favs.favStops) {
				favs.favStopsStr.add(stop.agency+stop.intersection);
			}
		}
		return favs;
	}
	
	private void writeout() {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = context.openFileOutput(filename, Context.MODE_WORLD_WRITEABLE);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	public boolean addStop(SimpleStop s) {
		boolean ret = favStops.add(s);
		favStopsStr.add(s.agency+s.intersection);
		writeout();
		return ret;
	}
	public boolean addRoute(Route r) {
		boolean ret = favRoutes.add(r.lName);
		writeout();
		return ret;
	}
	public boolean addRoute(String r_lName) {
		boolean ret = favRoutes.add(r_lName);
		writeout();
		return ret;
	}
	public boolean addAgency(Agency a) {
		boolean ret = favAgencies.add(a);
		writeout();
		return ret;
	}
	
	public boolean removeStop(SimpleStop s) {
		boolean ret = favStops.remove(s);
		favStopsStr.remove(s.agency+s.intersection);
		writeout();
		return ret;
	}
	public boolean removeRoute(Route r) {
		if(r==null) return false;
		boolean ret = favRoutes.remove(r.lName);
		writeout();
		return ret;
	}
	public boolean removeRoute(String r_lName) {
		boolean ret = favRoutes.remove(r_lName);
		writeout();
		return ret;
	}
	public boolean removeAgency(Agency a) {
		boolean ret = favAgencies.remove(a);
		writeout();
		return ret;
	}
	
	public boolean checkRoute(Route r) {
		if(r==null) return false;
		
		if(r.lName == null) Log.e("Favorites", "r.lName was null in checkRoute.");
		if(favRoutes == null) Log.e("Favorites", "favRoutes was null in checkRoute.");
		
		return(favRoutes.contains(r.lName));
	}
	public boolean checkRoute(String r_lName) {
		return(favRoutes.contains(r_lName));
	}
	
	public boolean checkStop(SimpleStop s) {
		return(checkStop(s.agency+s.intersection));
	}	
	public boolean checkStop(String s) {
		boolean ret = favStopsStr.contains(s);
		return ret;
	}
	public boolean checkAgency(Agency a) {
		boolean ret = favAgencies.contains(a);
		return ret;
	}

	public Set<SimpleStop> getFavStops() {
		return favStops;
	}

	public Set<Agency> getFavAgencies() {
		return favAgencies;
	}
	
	public Set<String> getFavRoutes() {
		return favRoutes;
	}
}
