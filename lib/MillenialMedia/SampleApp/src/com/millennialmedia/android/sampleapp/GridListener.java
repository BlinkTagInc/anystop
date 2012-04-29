package com.millennialmedia.android.sampleapp;

import android.app.Activity;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Environment;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.content.ContentResolver;
import java.util.concurrent.Semaphore;
import java.lang.ref.WeakReference;
import android.util.Log;

public class GridListener implements OnItemClickListener, MediaScannerConnection.MediaScannerConnectionClient
{
	private GridListener gridListener = this;
	private WeakReference<Activity> activityReference;
	private ImageAdapter imageAdapter;
	
	// Shared action variables
	private long selectedId;
	private int selectedPosition;
	private String selectedPath;
	
	// B&W Conversion variables
	private Semaphore semaphore;
	private MediaScannerConnection mediaScanner;
	private String scanPath;
	private double progress;
	private boolean sepia;
	private Activity activity; // Temporary only!!! Set to null after conversion

	public void onMediaScannerConnected()
	{
		mediaScanner.scanFile(scanPath, null);
	}
	
	public void onScanCompleted(String path, Uri uri)
	{
		Activity activity = (Activity)activityReference.get();

		mediaScanner.disconnect();
		mediaScanner = null;
		scanPath = null;
		if(activity != null)
		{
			activity.runOnUiThread(new Runnable() {
				public void run()
				{ imageAdapter.requery(); }
			});
		}
		semaphore.release();
	}

	private void onViewImage()
	{
		Intent intent;
		Activity activity = (Activity)activityReference.get();
		
		if(activity == null)
		{
			Log.e("SampleApp", "Weak reference failed.");
			return;
		}
		
		intent = new Intent().setClass(activity.getApplicationContext(), ImageViewActivity.class);
		if(selectedId != 0)
			intent.putExtra("id", (long)selectedId);
		else
			intent.putExtra("path", (String)selectedPath);
		activity.startActivity(intent);
	}
	
	private void onCreateBWImage()
	{
		Thread thread = new Thread()
		{
			@Override
			public void run()
			{
				Bitmap bitmap, bwBitmap;
				String pathHead, pathFile;
				File file, imagesPath;
				FileOutputStream fileOutputStream;
				String bwPath;
				int i, j;
				int r, g, b, a, gray;
				int []pixels;
				double step;
				float []hsv = new float[3];
				
				activity = (Activity)activityReference.get();
				if(activity == null)
				{
					Log.e("SampleApp", "Weak reference failed.");
					return;
				}
						
				try
				{
				semaphore.acquire();
				imagesPath = Environment.getExternalStorageDirectory();
				imagesPath = new File(imagesPath, "Pictures/");
				
				if(selectedId != 0)
				{
					pathFile = Long.toString(selectedId);
				}
				else
				{
					// Split the path into two pieces
					i = selectedPath.lastIndexOf('.');
					pathHead = selectedPath.substring(0, i);
					i = pathHead.lastIndexOf('/');
					pathFile = pathHead.substring(i + 1);
				}
				
				i = 0;
				do
				{
					if(i == 0)
						bwPath = String.format("%s_bw.png", pathFile);
					else
						bwPath = String.format("%s_bw_%d.png", pathFile, i);
					++i;
					file = new File(imagesPath, bwPath);
				}while(file.exists());
		
				if(selectedId != 0)
					bitmap = BitmapFactory.decodeResource(activity.getResources(), (int)selectedId);
				else
					bitmap = ImageAdapter.decodeBitmapFile(selectedPath, 512, 512);
				bwBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
				bitmap.recycle();
				
				// Convert all of the pixels to black and white
				pixels = new int [bitmap.getWidth()];
				progress = 0.0;
				step = 10000.0 / (double)bitmap.getHeight();
				activity.runOnUiThread(new Runnable() {
					public void run()
					{ activity.setProgress((int)progress); activity.setProgressBarVisibility(true); }
				});
				for(i = 0; i < bitmap.getHeight(); ++i)
				{
					bwBitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, i, bitmap.getWidth(), 1);
					for(j = 0; j < pixels.length; ++j)
					{
						r = Color.red(pixels[j]);
						g = Color.green(pixels[j]);
						b = Color.blue(pixels[j]);
						a = Color.alpha(pixels[j]);
						gray = ((r * 30 + g * 59 + b * 11) / 100);
						if(sepia)
						{
							Color.RGBToHSV(112, 66, 20, hsv);
							float amount = (float)gray/(float)255.0;
							amount -= (float)0.5;
							// Lighten by amount
							hsv[1] = hsv[1] - (hsv[1] * amount);
							hsv[2] = hsv[2] + (((float)1.0 - hsv[2]) * amount);
							pixels[j] = Color.HSVToColor(hsv);
						}
						else
						{
							pixels[j] = Color.argb(a, gray, gray, gray);
						}
					}
					bwBitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, i, bitmap.getWidth(), 1);
					progress += step;
					activity.runOnUiThread(new Runnable() {
						public void run()
						{ activity.setProgress((int)progress); activity.setProgressBarVisibility(true); }
					});
				}
				activity.runOnUiThread(new Runnable() {
					public void run()
					{ activity.setProgress((int)10000); activity.setProgressBarVisibility(false); }
				});
				
				// Save the bitmap to disk
				imagesPath.mkdirs();
				fileOutputStream = new FileOutputStream(file);
				bwBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
				fileOutputStream.close();
				bwBitmap.recycle();
				
				scanPath = file.getAbsolutePath();
				mediaScanner = new MediaScannerConnection(activity.getParent(), gridListener);
				mediaScanner.connect();
				
				} catch(Exception e) { System.out.println(e.getMessage()); }
				finally { activity = null; }
			}
		};
		thread.start();
	}
	
	private void onDeleteImage()
	{
		Activity activity = (Activity)activityReference.get();
		if(activity == null)
		{
			Log.e("SampleApp", "Weak reference failed.");
			return;
		}
		ContentResolver contentResolver = activity.getContentResolver();
		String where = android.provider.MediaStore.MediaColumns.DATA + "=?";
		contentResolver.delete(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, where, new String[] {selectedPath});
		imageAdapter.requery();
	}

	public GridListener(Activity gridActivity)
	{
		activityReference = new WeakReference<Activity>(gridActivity);
		semaphore = new Semaphore(1);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		CharSequence[] items;
		Activity activity;
		
		activity = (Activity)activityReference.get();
		if(activity == null)
		{
			Log.e("SampleApp", "Weak reference failed.");
			return;
		}

		imageAdapter = (ImageAdapter)parent.getAdapter();
		selectedId = id;
		selectedPosition = position;
		selectedPath = (String)imageAdapter.getItem(selectedPosition);
		if(selectedPath != null)
			selectedId = 0;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Action");
		if(!imageAdapter.isStaticImages())
			items = new CharSequence [] {"View", "Create B&W Copy", "Create Sepia Copy"};
		else
			items = new CharSequence [] {"View", "Create B&W Copy (SD)", "Create Sepia Copy (SD)"};
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) 
			{
				switch(item)
				{
					case 0:
						onViewImage();
						break;
					case 1:
						sepia = false;
						onCreateBWImage();
						break;
					case 2:
						sepia = true;
						onCreateBWImage();
						break;
					case 3:
						onDeleteImage();
						break;
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
