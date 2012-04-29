package com.millennialmedia.android.sampleapp;

import com.millennialmedia.android.*;
import com.millennialmedia.android.MMAdView.MMAdListener;
import android.util.Log;

/* Generic MMAdListener
 * Demonstrates methods to implement as part of the listener interface
 */
public class AdListener implements MMAdListener 	
{
	public AdListener() {}
	
	public void MMAdFailed(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad View Failed" );
	}

	public void MMAdReturned(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad View Success" );
	}
	
	public void MMAdClickedToNewBrowser(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad clicked, new browser launched" );
	}
	
	public void MMAdClickedToOverlay(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad Clicked to overlay" );
	}
	
	public void MMAdOverlayLaunched(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad Overlay Launched" );
	}
	
	public void MMAdRequestIsCaching(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad caching request" );
	}
	
	public void MMAdCachingCompleted(MMAdView adview, boolean success)
	{
		Log.i("SampleApp", "Millennial Ad caching completed successfully: " + success);
	}
}
