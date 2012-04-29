package com.millennialmedia.android.sampleapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

// Simple ImageView activity
public class ImageViewActivity extends Activity
{
	private boolean loadOnFocus;
	private Bitmap bitmap;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		ImageView imageView;
		Intent intent;
		long resId;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.imageview);
		imageView = (ImageView)findViewById(R.id.imageView);

		intent = getIntent();
		resId = intent.getLongExtra("id", 0);
		if(resId != 0)
		{
			imageView.setImageResource((int)resId);
		}
		else
		{
			loadOnFocus = true;
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean windowInFocus)
	{
		ImageView imageView;
		Intent intent;
		long resId;
		
		if(loadOnFocus && windowInFocus)
		{
			intent = getIntent();
			resId = intent.getLongExtra("id", 0);
			if(resId == 0)
			{
				String path;
				path = intent.getStringExtra("path");
				imageView = (ImageView)findViewById(R.id.imageView);
				imageView.setImageBitmap(bitmap = ImageAdapter.decodeBitmapFile(path, imageView.getWidth(), imageView.getHeight()));
			}
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		/*SampleApp sampleAppActivity;
		sampleAppActivity = (SampleApp)getParent();
		sampleAppActivity.showInterstitial();*/
		if(bitmap != null)
			bitmap.recycle();
		finish();
		return true;
	}
}
