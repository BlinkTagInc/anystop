package org.busbrothers.anystop.agencytoken.map;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

/** This MyView class is really just a MapView with a LocationListener built in. The location listener has no function other than 
 * making battery management slightly more difficult than before.
 * @author ivany
 *
 */

public class MyView extends MapView implements LocationListener {

	private static final int MIN_METERS_TO_REGISTER = 10;
	private static final int MIN_TIME_TO_REGISTER = 5*1000; //5 seconds
//	public static final double GEOFENCE_KM_OFF = 0.5d; //500 meters
//	public static final double GEOFENCE_KM_ON = 0.1d; //100 meters
//	public static final double GEOFENCE_KM_ON_SPECIAL = 0.5d; //500 meters
	public static final int VALID_ACCURACY_M = 100;
	
	public MyView(Context context, String apiKey) {
		super(context, apiKey);
		initLoc();
		// TODO Auto-generated constructor stub
	}

	public MyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initLoc();
		// TODO Auto-generated constructor stub
	}

	public MyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initLoc();
		// TODO Auto-generated constructor stub
	}
	public void setZoomControlsCanonical() {
		super.setBuiltInZoomControls(true);
	}

	/** This method should be called when the parent Activity goes out of focus, either in onPause() (preferred) or in onStop().
	 * It will cause this MyView to no longer receive location updates thereby saving power for the user
	 */
	public void initLoc() {
		LocationManager lm = (LocationManager) this.getContext().getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_TO_REGISTER, 
				MIN_METERS_TO_REGISTER, this);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_TO_REGISTER, 
				MIN_METERS_TO_REGISTER, this);
	}
	
	/** This method should be called when the parent Activity goes out of focus, either in onPause() (preferred) or in onStop().
	 * It will cause this MyView to no longer receive location updates thereby saving power for the user
	 */
	public void stopLoc() {
		LocationManager lm = (LocationManager) this.getContext().getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);
	}

	public void onLocationChanged(Location location) {
		Toast.makeText(this.getContext(), "location changed", 1000);
		this.invalidate();
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
	
	
//	public boolean dispatchTouchEvent(MotionEvent event)
//	{
//        int posX=0;
//        int posY=0;
//        posX = (int) event.getX();
//        posY = (int) event.getY();
//        //StopMap.single().textgeo();
//        Projection p = this.getProjection();
//        GeoPoint g = p.fromPixels(posX, posY);
//        Toast t = Toast.makeText(getContext(), "lat:" + g.getLatitudeE6() + " lon:" + g.getLongitudeE6(), 1000);
//        t.show();
//        super.onTouchEvent(event);
//        return true;
//	}
	
}
