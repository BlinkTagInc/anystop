package org.busbrothers.anystop.agencytoken.datacomponents;

import org.busbrothers.anystop.agencytoken.Manager;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class LocationGetter extends Activity {
	public static Location loc = null;
	public float acc = 0;

	public class WhereamiLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			
			loc = location;
			Manager.currloc = location;
			if (location.hasAccuracy()) {
				acc=location.getAccuracy();
			} else {
				acc=1000;
			}

		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public void onResume() {
		super.onResume();
		WhereamiLocationListener listener = new WhereamiLocationListener();
		LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		long updateTimeMsec = 1000L;
		manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				updateTimeMsec, 0, listener);
		manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				updateTimeMsec, 0, listener);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

}