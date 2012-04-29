package org.busbrothers.anystop.agencytoken.map;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MapItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	public int position;
	public void clear() {
		mOverlays = new ArrayList<OverlayItem>();
	}
	
	public OverlayItem getMark() {
		return mOverlays.get(0);
	}
	
	public MapItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	@Override
	protected OverlayItem createItem(int i) {
	  return mOverlays.get(i);
	}

	public void addOverlay(OverlayItem overlay) {
	    mOverlays.add(overlay);
	    populate();

	}
	@Override
	public int size() {
		return mOverlays.size();
	}
	
}
