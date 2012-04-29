package org.busbrothers.anystop.agencytoken.map;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class ChooseMapItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	
	public void clear() {
		mOverlays = new ArrayList<OverlayItem>();
	}
	
	public OverlayItem getMark() {
		return mOverlays.get(0);
	}
	
	public ChooseMapItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	@Override
	protected OverlayItem createItem(int i) {
	  return mOverlays.get(i);
	}

//	 @Override  
//	protected boolean onTap(int i) {  
//		StopMap.single().clickDisp("clikt");
//		return(true); 
//	}  
	
//	@Override
//	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
//		// TODO Auto-generated method stub
//		HelloMap.single().clickDisp("touchdown\n"+event.getX() + ", " + event.getY());
//		HelloMap.single().mvgraph(event.getX(), event.getY());
//		return false;
//	}

	public void addOverlay(OverlayItem overlay) {
	    mOverlays.add(overlay);
	    populate();

	}
	@Override
	public int size() {
		return mOverlays.size();
	}
	
}
