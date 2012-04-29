package org.busbrothers.anystop.agencytoken.map;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationWrapper {

	private static LocationWrapper single = null;
	private LocationManager loc;
	
	public static LocationWrapper getInstance() {
		if (single == null) {
			single = new LocationWrapper();
		}
		return single;
	}
	
	public void setLocManager(LocationManager loc) {
		this.loc = loc;
	}
	
	private LocationWrapper() {
		
	}
	public void initGPS() {
	}
}
