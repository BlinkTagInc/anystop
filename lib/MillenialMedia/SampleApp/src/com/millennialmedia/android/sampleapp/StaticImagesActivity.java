/* Millennial Media Ad View Integration
 * 
 * Provided below is the corresponding Java code for integrating an MMAdView into 
 * your app using an XML layout. Please refer to staticimages.xml for the associated layout code.
 * In your layout you MUST specify apid and adType attributes for your MMAdView.
 * 
 * The code below demonstrates how you can get a reference to your MMAdView by using the 
 * findViewById method. It is then recommended that you provide meta data such as the desired 
 * height and width of ads, keywords describing the context, and demographic data about your users.
 *
 * You can also implement a listener to provide callback methods about events such as ad success or
 * ad failure.
 *
 * For a complete list of XML attributes, meta data keys, and listener methods please see consult
 * the Android SDK wiki reference: http://wiki.millennialmedia.com/index.php/Android
 *
 */

package com.millennialmedia.android.sampleapp;

import java.util.Hashtable;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

import com.millennialmedia.android.MMAdView;

public class StaticImagesActivity extends Activity
{	
	// The ad view object
	private MMAdView adview;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// (SampleApp Only) Setup the activity
		super.onCreate(savedInstanceState);
		requestWindowFeature(android.view.Window.FEATURE_PROGRESS);
		setContentView(R.layout.staticimages);
		
		/******** Millennial Media Ad View Integration via XML ********/
		
		// Find the ad view for reference
		adview = (MMAdView)findViewById(R.id.adView);
		
		// (Optional/Recommended) Set additional meta data
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put(MMAdView.KEY_KEYWORDS, "images,photography");
		map.put(MMAdView.KEY_HEIGHT, "53");
		map.put(MMAdView.KEY_WIDTH, "320");
		adview.setMetaValues(map);
		
		// (Optional) Set an event listener.
		// See AdListener.java for a basic implementation
		adview.setListener(new AdListener());
		
		/******************************************************/

		// (SampleApp Only) Setup the image grid layout
		GridView gridView = (GridView)findViewById(R.id.staticGridView);
		ImageAdapter imageAdapter = new ImageAdapter(this, new int[] { R.drawable.logo, R.drawable.android } );
		gridView.setAdapter(imageAdapter);
		gridView.setOnItemClickListener(new GridListener(this));
	}
}
