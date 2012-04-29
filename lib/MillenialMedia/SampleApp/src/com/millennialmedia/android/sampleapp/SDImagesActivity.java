/* Millennial Media Ad View Integration - Additional Features
 * 
 * In this file is code for integrating an MMAdView into your app programatically. The same 
 * requirements apply to creating an MMAdView programatically as it does in XML. Also refer to the 
 * basic implementation given in StaticImagesActivity.java
 *
 * Below is a brief reference for the MMAdView constructor:
 *
 * public MMAdView(final Activity context, String apid, String adType, int refreshInterval, Hashtable<String, String> metaMap, boolean accelerate)
 * 
 * **Required**:
 * 		- Context: The current activity to place the MMAdView in.
 * 		- APID: This number uniquely identifies your application and is given to you by Millennial Media.
 * 		- Ad Type: A string specifying which type of ad to use. See the documentation for more options.
 * 		- Refresh ads: refresh delay in seconds. Set to 0 for no refresh. Set to -1 for manual. Minimum is 30 seconds.
 * **Optional (See documentation for more info)**:
 * 		- Hashtable of meta values, e.g.: age, gender, marital status, zip code, income, latitude, longitude
 * 		- Accelerometer disable: boolean variable to disable accelerometer ads if your app uses the accelerometer.
 *
 * For convenience, after the MMAdView is created it is placed inside a FrameLayout that was 
 * reserved in the XML layout as a placeholder for the MMAdView.
 * 
 * In this example, the MMAdView has its refresh interval disabled and is only manually refreshed
 * using the callForAd method. While using the app press the menu button to get a refresh option.
 * This example also demonstrates a more thorough example of how to 
 * provide meta data, listener methods, and how to use conversion tracking.
 *
 * Also included in this example is methods for showing static interstitials using the old way and
 * the new way with 4.5+ methods fetch/check/display.
 *
 * See also: README.txt or http://wiki.millennialmedia.com/index.php/Android
 *
 */

package com.millennialmedia.android.sampleapp;

import com.millennialmedia.android.*;
import com.millennialmedia.android.MMAdView.MMAdListener;
import com.millennialmedia.android.MMAdViewSDK;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.Toast;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import java.util.Hashtable;

public class SDImagesActivity extends Activity implements MMAdListener
{
	// Declare your APID, given to you by Millennial Media
	public final static String MYAPID = "28911";
	public final static String MYGOALID = "12345";
	
	// The ad view object
	private MMAdView adView;
	private MMAdView fetchCheckDisplayAdView;
	private static MMAdView interAdView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// (SampleApp Only) Setup the main activity
		super.onCreate(savedInstanceState);
		requestWindowFeature(android.view.Window.FEATURE_PROGRESS);
		requestWindowFeature(android.view.Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.sdimages);
		
		/******** Millennial Media Ad View Integration ********/
		
		// Create the adview
		adView = new MMAdView(this, MYAPID, MMAdView.BANNER_AD_BOTTOM, MMAdView.REFRESH_INTERVAL_OFF);
		adView.setId(MMAdViewSDK.DEFAULT_VIEWID);

		// Add the adview to the view layout
		FrameLayout adFrameLayout = (FrameLayout)findViewById(R.id.adFrameLayout);
		adFrameLayout.addView(adView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));

		// (Optional/Recommended) Set meta data (will be applied to subsequent ad requests)
		Hashtable<String, String> metaData = SDImagesActivity.createMetaData();
		metaData.put("height", "53");
		metaData.put("width", "320");
		adView.setMetaValues(metaData);

		// (Optional) Set the listener to receive events about the adview
		adView.setListener(this);
		
		// (Optional) Start conversion tracking
		MMAdView.startConversionTrackerWithGoalId(this, MYGOALID);
		
		// (Optional) New Millennial Media Ad View Interstitial Integration using fetch/check/display
		
		fetchCheckDisplayAdView = new MMAdView(this, MYAPID, MMAdView.FULLSCREEN_AD_TRANSITION, true, SDImagesActivity.createMetaData());
		fetchCheckDisplayAdView.setId(MMAdViewSDK.DEFAULT_VIEWID + 1);
		
		Button button = (Button)findViewById(R.id.fetchButton);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				String message = "Fetching cached ad...";
				Log.i("SampleApp", "Fetch - " + message);
				Toast toast = Toast.makeText(SDImagesActivity.this, message, Toast.LENGTH_SHORT);
				toast.show();
				fetchCheckDisplayAdView.fetch();
			}
		});
		
		button = (Button)findViewById(R.id.checkButton);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				boolean cached = fetchCheckDisplayAdView.check();
				String message = cached?"Cached ad is ready to view.":"Cached ad is not ready to view.";
				Log.i("SampleApp", "Check - " + message);
				Toast toast = Toast.makeText(SDImagesActivity.this, message, Toast.LENGTH_SHORT);
				toast.show();
			}
		});

		button = (Button)findViewById(R.id.displayButton);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				boolean displayed = fetchCheckDisplayAdView.display();
				String message = "Cached ad is now displaying...";
				if(!displayed)
				{
					message = "Cached ad is not ready to view.";
					Toast toast = Toast.makeText(SDImagesActivity.this, message, Toast.LENGTH_SHORT);
					toast.show();
				}
				Log.i("SampleApp", "Display - " + message);
			}
		});
		
		/******************************************************/

		// (SampleApp Only) Setup the image grid layout
		GridView gridView = (GridView)findViewById(R.id.sdGridView);
		ImageAdapter imageAdapter = new ImageAdapter(this, null);
		gridView.setAdapter(imageAdapter);
		gridView.setOnItemClickListener(new GridListener(this));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sd_images_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch(item.getItemId())
		{
			case R.id.refreshItem:
				setProgress(android.view.Window.PROGRESS_END);
				setProgressBarIndeterminateVisibility(true);
				adView.callForAd();
				return true;
			case R.id.showInterstitialItem:
				SDImagesActivity.showInterstitial(this);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public static void showInterstitial(Activity activity)
	{
		if(activity != null)
		{			
			/******** Old Millennial Media Ad View Interstitial Integration ********/
			if(interAdView == null)
			{
				interAdView = new MMAdView(activity, MYAPID, MMAdView.FULLSCREEN_AD_TRANSITION, true, SDImagesActivity.createMetaData());
				interAdView.setId(MMAdViewSDK.DEFAULT_VIEWID + 2);
			}
			interAdView.callForAd();
			/******************************************************/
		}
	}
    
    private static Hashtable<String, String> createMetaData()
	{
		// Below are some of the meta data values you can use
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("age", "45");
		map.put("gender", "male");
		map.put("zip", "21224");
		map.put("marital", "single");
		map.put("orientation", "straight");
		map.put("ethnicity", "hispanic");
		map.put("income", "50000");
		map.put("keywords", "soccer");
		return map;
	}
    
    /******** (Optional) Millennial Media Ad View Listener Integration ********/
	
	public void MMAdReturned(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad View Success");
		runOnUiThread(new Runnable() {
			public void run()
			{ setProgressBarIndeterminateVisibility(false); }
		});
	}
	
	public void MMAdFailed(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad View Failed");
		runOnUiThread(new Runnable() {
			public void run()
			{ setProgressBarIndeterminateVisibility(false); }
		});
	}
	
	public void MMAdClickedToNewBrowser(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad clicked, new browser launched");
	}
	
	public void MMAdClickedToOverlay(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad Clicked to overlay");
	}
	
	public void MMAdOverlayLaunched(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad Overlay Launched");
	}
	
	public void MMAdRequestIsCaching(MMAdView adview)
	{
		Log.i("SampleApp", "Millennial Ad caching request");
		
		runOnUiThread(new Runnable() {
			public void run() {
				Toast toast = Toast.makeText(SDImagesActivity.this, "Caching started.", Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}
	
	public void MMAdCachingCompleted(MMAdView adview, boolean success)
	{		
		Log.i("SampleApp", "Millennial Ad caching completed successfully: " + success);
		
		runOnUiThread(new Runnable() {
			public void run() {
				Toast toast = Toast.makeText(SDImagesActivity.this, "Interstitial has finished caching", Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}

	/* Use this if you wish to be notified that an advertisement was closed */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
        case 0:
			if(resultCode == RESULT_CANCELED)
			{
				Log.i("SampleApp", "Millennial Ad Overlay Closed");
			}	
		}
	}
	
	/******************************************************/
	
}
