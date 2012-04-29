package com.millennialmedia.android.sampleapp;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.util.Log;

public class ImageAdapter extends BaseAdapter
{
	static private Cursor imageCursor;
	
	private final int THUMBNAIL_SIZE = 85;
	private WeakReference<Activity> activityReference;
	private int []images;
	private Hashtable<String, Bitmap> thumbnails = new Hashtable<String, Bitmap>();
	
	// http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue
	public static Bitmap decodeBitmapFile(String path, int maxWidth, int maxHeight)
	{
		Bitmap bitmap;
		int scaleWidth = 1, scaleHeight = 1;

		if(path == null)
			return null;

		// Decode image size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		if(options.outHeight > maxHeight)
			scaleHeight = (int)Math.pow(2.0, Math.round(Math.log((double)options.outHeight / (double)maxHeight) / Math.log(2)));
			
		if(options.outWidth > maxWidth)
			scaleWidth = (int)Math.pow(2.0, Math.round(Math.log((double)options.outWidth / (double)maxWidth) / Math.log(2)));
		
		// Decode with inSampleSize
		options = new BitmapFactory.Options();
		//options.inTempStorage = new byte[16*1024];
		options.inSampleSize = Math.max(scaleWidth, scaleHeight);
		bitmap = BitmapFactory.decodeFile(path, options);
		return bitmap;
	}
	
	public ImageAdapter(Activity a, int []imagesArray)
	{
		activityReference = new WeakReference<Activity>(a);
		if((imagesArray != null) && (imagesArray.length != 0))
		{
			images = imagesArray;
		}
		else
		{
			imageCursor = a.managedQuery(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
			// Preload thumbnails from the cursor
			new Thread(new Runnable() {
				public void run() {
					Bitmap bitmap;
					for(int pos = 0; pos < imageCursor.getCount(); ++pos)
						bitmap = getThumbnail(pos);
				}
			}).start();
		}
	}
	
	public boolean isStaticImages()
	{
		return (images != null);
	}
	
	public void requery()
	{
		if(!isStaticImages())
		{
			if(imageCursor == null)
			{
				Activity a = (Activity)activityReference.get();
				if(a != null)
					imageCursor = a.managedQuery(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);	
			}
		}
		if(imageCursor != null)
			imageCursor.requery();
		notifyDataSetChanged();
	}
	
	public int getCount()
	{
		if(isStaticImages())
		{
			return images.length;
		}
		else
		{
			if(imageCursor == null)
				return 0;
			return imageCursor.getCount();
		}
	}
	
	public Object getItem(int position)
	{
		if(isStaticImages())
		{
			return null;
		}
		else
		{
			imageCursor.moveToPosition(position);
			int col = imageCursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA);
			return imageCursor.getString(col);
		}
	}
	
	public long getItemId(int position)
	{
		if(isStaticImages())
			return images[position];
		return position;
	}
	
	private Bitmap getThumbnail(int pos) throws CursorIndexOutOfBoundsException
	{
		String []projection = { android.provider.MediaStore.Images.Thumbnails.DATA };
		int col;
		long id;
		String selection;
		Cursor thumbCursor;
		Bitmap bitmap;
		Activity a = (Activity)activityReference.get();
		
		if(a == null)
			return null;
		
		if(pos >= 0)
			imageCursor.moveToPosition(pos);
		col = imageCursor.getColumnIndex(android.provider.MediaStore.MediaColumns._ID);
		id = imageCursor.getLong(col);

		selection = android.provider.MediaStore.Images.Thumbnails.IMAGE_ID + "=?";
		thumbCursor = a.getContentResolver().query(android.provider.MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, selection, new String[] { Long.toString(id) }, android.provider.MediaStore.Images.Thumbnails.HEIGHT);
		
		if((thumbCursor == null) || (thumbCursor.getCount() == 0))
		{
			if(thumbCursor != null)
				thumbCursor.close();
			projection = new String[] { android.provider.MediaStore.Images.Media.DATA };
			selection = "_ID=?";
			thumbCursor = a.getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, new String[] { Long.toString(id) }, null);
		}
		
		if(thumbCursor != null)
		{
			if(thumbCursor.getCount() >= 1)
			{
				thumbCursor.moveToFirst();
				col = thumbCursor.getColumnIndex(projection[0]);
				String path = thumbCursor.getString(col);
				thumbCursor.close();
				bitmap = thumbnails.get(path);
				if(bitmap == null)
				{
					bitmap = ImageAdapter.decodeBitmapFile(path, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
					if(bitmap != null)
						thumbnails.put(path, bitmap);
				}
				return bitmap;
			}
			thumbCursor.close();
		}
		return null;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		ImageAdapterView imageView;
		Activity a;
		Resources res;
		
		if(convertView == null)
		{
			a = (Activity)activityReference.get();
			if(a == null)
				return null;
			imageView = new ImageAdapterView(a);
			imageView.setLayoutParams(new GridView.LayoutParams(THUMBNAIL_SIZE, THUMBNAIL_SIZE));
		}
		else
		{
			imageView = (ImageAdapterView)convertView;
		}
	
		if(isStaticImages())
		{
			imageView.setPosition(-1);
			imageView.setResource(images[position]);
		}
		else
		{
			imageView.setPosition(position);
			imageView.setResource(0);
		}

		return imageView;
	}
	
	private class ImageAdapterView extends ImageView
	{
		private int position;
		private int resource;
		private Bitmap bitmap;
		
		public ImageAdapterView(Activity activity) { super(activity); }
		
		public void setPosition(int position)
		{
			this.position = position;
			bitmap = null;
		}
		
		public void setResource(int resource)
		{
			this.resource = resource;
			bitmap = null;
		}
		
		@Override
		protected void onDraw(Canvas canvas)
		{
			if(position == -1)
			{
				Activity a = (Activity)activityReference.get();
				if(a != null)
				{
					setImageResource(resource);
					super.onDraw(canvas);
				}
			}
			else
			{
				try
				{
				
				bitmap = getThumbnail(position);
				if(bitmap != null)
					canvas.drawBitmap(bitmap, 0, 0, null);
				}catch(CursorIndexOutOfBoundsException e){ Log.w("SampleApp", "ImageAdapter index out of bounds."); }
			}
		}
	}
}
