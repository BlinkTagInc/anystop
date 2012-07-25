package org.busbrothers.anystop.agencytoken.map;

import java.util.List;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.activities.EditPrefs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.flurry.android.FlurryAgent;
import com.google.ads.AdView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.sensedk.AswAdLayout;


public class StopMap extends MapActivity {
	
	static StopMap me;
	
	private ProgressDialog pd;
	
	private static final int START=234, END=237, LOAD=2;
	public static StopMap single() {
		return me;
	}
	Button backButton;
	LinearLayout linearLayout;
	MyView mapView;
	View routeView;
	ZoomControls mZoom;
	List<Overlay> mapOverlays;
	Drawable start, end;
	ChooseMapItemizedOverlay startOverlay, endOverlay;
	OverlayItem startItem, endItem;
	Projection p;
	MyLocationOverlay myLocationOverlay;
	Dialog dialog;
    static GeoPoint startpoint = null;
    static GeoPoint endpoint = null;
    
    static String activityNameTag = "StopMap";
    
    private boolean flurryEventLogged;
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        me=this;
        
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        
        linearLayout = (LinearLayout) findViewById(R.id.zoomview);
        mapView = (MyView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
        GeoPoint tempGP;
        tempGP = mapView.getMapCenter();
        Log.v(activityNameTag, "Mapview thinks location is " + tempGP.getLatitudeE6() + ", " + tempGP.getLongitudeE6());
        
        backButton = (Button) findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	me.finish();
            }
        }); 

        p = mapView.getProjection();
        mapView.layout(0, 0, 320, 400);
        mapOverlays = mapView.getOverlays();
        //linearLayout.addView(mZoom);

        startpoint = new GeoPoint(Manager.lat, Manager.lon);
        start = this.getResources().getDrawable(R.drawable.stop);
        startOverlay = new ChooseMapItemizedOverlay(start);
        startItem = new OverlayItem(startpoint, "", "");
        startOverlay.addOverlay(startItem);
        mapOverlays.add(startOverlay);
        
        Log.v(activityNameTag, "Setting position of stop to " + startpoint.getLatitudeE6() + ", " + startpoint.getLongitudeE6());
        
        
        int currlat;
        int currlon;
        GeoPoint curr;
        try {
        	Manager.currloc = Manager.getMyLoc(this);
	        currlat = (int)(Manager.currloc.getLatitude()*10e5);
	        currlon = (int)(Manager.currloc.getLongitude()*10e5);
	        curr = new GeoPoint(currlat, currlon);
        } catch (Exception ex) {
	        currlat = startpoint.getLatitudeE6();
	        currlon = startpoint.getLongitudeE6();
	        curr = startpoint;
        }
        MapController m = mapView.getController();
        int centerlat = (int)(startpoint.getLatitudeE6()+currlat)/2;
        int centerlon = (int)(startpoint.getLongitudeE6()+currlon)/2;
        GeoPoint center = new GeoPoint(centerlat, centerlon);
        //m.animateTo(center);
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        mapOverlays.add(myLocationOverlay);
        myLocationOverlay.enableMyLocation();
        m.zoomToSpan(Math.abs(startpoint.getLatitudeE6()-currlat), Math.abs(startpoint.getLongitudeE6()-currlon));
        
        //Make sure we don't zoom in any more than we're allowed to
        int spanZoomLevel = mapView.getZoomLevel();
        String minimumZoomStr = (String) this.getString(R.string.agencyStopMapZoomLevel);
        int minimumZoomLevel = (minimumZoomStr != null) ? Integer.parseInt(minimumZoomStr) : -999;
        if(spanZoomLevel > minimumZoomLevel) m.setZoom(minimumZoomLevel); //zoom level greater -> zoomed in closer
        
        m.animateTo(center);
        
        flurryEventLogged = false;
    }
    
	@Override
	protected void onStart() {
		super.onStart();
		if (Manager.isUseUsage(this)) {
			Toast t = Toast.makeText(this, "The map shows your position and chosen bus stop.", Toast.LENGTH_LONG);
			t.show();
			
			t = Toast.makeText(this, "Hit the button below to get back to your prediction list.", Toast.LENGTH_SHORT);
			t.show();
		}
		
		//Report opening of StopsTime to Flurry Analytics
		FlurryAgent.onStartSession(this, Manager.getFlurryAPIK());
		if(! flurryEventLogged) {
			Manager.flurryActivityOpenEvent(activityNameTag);
			flurryEventLogged = true;
		}
	}
    
    @Override
    protected void onStop() {
    	super.onStop();
		FlurryAgent.onEndSession(this);	
    }


	@Override
    protected boolean isRouteDisplayed() {
        return false;
    }
	@Override
    protected boolean isLocationDisplayed() {
        return true;
    }
    
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mapmenu, menu);
	    return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.v(activityNameTag, "stopping location provider...");
		this.myLocationOverlay.disableMyLocation();
		this.mapView.stopLoc();
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		Log.v(activityNameTag, "starting location provider...");
		this.myLocationOverlay.enableMyLocation();
		this.mapView.initLoc();
		
		LinearLayout adLayoutParent = (LinearLayout) findViewById(R.id.dlist_ad_holder); //this LinearLayout will contain the AdView
		
		//The below code sets up Advertisement-related stuff for this activity
		if(Manager.adTypeUsing() == Manager.ADMOB_AD) {			
			//The below block of code sets up the AdView for this Activity, and adds it to the Layout
			AdView googleAdMobAd = Manager.setupAdView(this); //have Manager set up the AdView for us :)
			adLayoutParent.addView(googleAdMobAd);
		}
		else if(Manager.adTypeUsing() == Manager.ADDIENCE_AD) {
			AswAdLayout adView = Manager.makeAddienceAd(this);
			adLayoutParent.addView(adView);
		}
		
		adLayoutParent.invalidate();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.prefs:
				startActivity(new Intent(this, EditPrefs.class));  
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}

}