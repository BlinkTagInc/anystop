/* Millennial Media Ad View Integration
 * 
 * Please refer to comments and documentation provided in the code files for more information.
 *
 * StaticImagesActivity.java - Demonstrates the minimum requirements to integrate an MMAdView into
 * your app. This sample creates an MMAdView in its XML layout file (staticimages.xml)
 *
 * SDImagesActivity.java - Creates an MMAdView programatically and adds meta data, listener 
 * callbacks, conversion tracking, and fetch/check/display interstitial capabilities.
 *
 * See also: README.txt or http://wiki.millennialmedia.com/index.php/Android
 *
 */

package com.millennialmedia.android.sampleapp;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class SampleAppActivity extends TabActivity
{
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
		TabHost tabHost;					// The activity TabHost
		TabHost.TabSpec spec;				// Resusable TabSpec for each tab
		Intent intent;						// Reusable Intent for each tab
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(android.view.Window.FEATURE_PROGRESS);
		requestWindowFeature(android.view.Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
		
		tabHost = getTabHost();
		
		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("static");
		spec.setIndicator("Static Images", getResources().getDrawable(android.R.drawable.ic_menu_gallery));
		intent = new Intent().setClass(this, StaticImagesActivity.class);
		spec.setContent(intent);
		tabHost.addTab(spec);
		
		spec = tabHost.newTabSpec("sd");
		spec.setIndicator("SD Card Images", getResources().getDrawable(android.R.drawable.ic_menu_save));
		intent = new Intent().setClass(this, SDImagesActivity.class);
		spec.setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setCurrentTab(0);
    }	
}
