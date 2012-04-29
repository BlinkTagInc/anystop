package org.busbrothers.anystop.agencytoken.uicomponents;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.activities.EditPrefs;

//import com.adwhirl.AdWhirlLayout.AdWhirlInterface;
import com.flurry.android.FlurryAgent;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.widget.ListView;
import android.widget.Toast;

//Use this one if you're using AdWhirl
//public abstract class CustomList extends ListActivity implements AdWhirlInterface {
public abstract class CustomList extends ListActivity {

	protected static CustomList me;
	private long _animationMillis;
	protected String[] _usageMessages;
	protected boolean flurryEventLogged; //Will be set to true once we tell Flurry Analytics that this event has been started
	
	private static final String activityNameTag = "CustomList";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		me=this;
		
		//Apply new typeface to all Views in this listview
		Manager.applyFonts((View) findViewById(R.id.rootlayout));
		
		Animation set = Effects.inFromRight();
		LayoutAnimationController controller =
            new LayoutAnimationController(set, .6f);
        
		getListView().setLayoutAnimation(controller);
		
		flurryEventLogged = false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		for(int i = 0; i< getListView().getChildCount(); i++) {
				getListView().getChildAt(i).setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
		
		Log.v(activityNameTag, "Ending Flurry session.");
	}
	
	/** 
	 * This method creates a cool fade-out effect for all of the list items that weren't selected. That's about all it does.
	 */
	public void onListItemClick(ListView parent, View v, int position, long id) {
		Animation unselectedEffect = Effects.fadeOut();
		Animation selectedEffect = Effects.goRight();
		System.out.println("number indices:" + parent.getChildCount());
	
		for(int i = 0; i<=parent.getCount()-1; i++) {
			try {
				if (parent.getChildAt(i)==v) {
//					parent.getChildAt(i).startAnimation(selectedEffect);
//					parent.getChildAt(i).setVisibility(View.INVISIBLE);
				} else {
					parent.getChildAt(i).startAnimation(unselectedEffect);
					parent.getChildAt(i).setVisibility(View.INVISIBLE); 
				}
			} catch (NullPointerException npe) {
				// Iteration weirdness here! Android bug.
			}
		}

		_animationMillis = selectedEffect.computeDurationHint();
	}
	
	/** This method undoes the "fade out" effect that onListItemClick instigates. */
	public void undoListItemClick(ListView parent, int position) {
		Animation ununselectedEffect = Effects.fadeOut();
		
		for(int i = 0; i<=parent.getCount()-1; i++) {
			try {
				if (i==position) { //don't do anything for the currently selected List item 
//					parent.getChildAt(i).startAnimation(selectedEffect);
//					parent.getChildAt(i).setVisibility(View.INVISIBLE);
				} else {
					parent.getChildAt(i).startAnimation(ununselectedEffect);
					parent.getChildAt(i).setVisibility(View.VISIBLE); 
				}
			} catch (NullPointerException npe) {
				// Iteration weirdness here! Android bug.
			}
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mapmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.prefs:
				startActivity(new Intent(this, EditPrefs.class));  
				return(true);
				
			case R.id.about:
				Manager.aboutDialog(this).show();
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}
	
	public long getAnimationTime() {
		return _animationMillis;
	}
	
	private static boolean first=true;
	protected void onStart() {
		super.onStart();
		if (Manager.isUseUsage(this) && first) {
			Log.d(activityNameTag, "Decided that isUseUsage(this) must be true?? It was " + Manager.isUseUsage(this) + ".");
			
			
			Toast t;
			for (String s : _usageMessages) {
				int len = (s.length()<60 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
				t = Toast.makeText(this, s, len);
				t.setGravity(Gravity.CENTER, 0, 0);
				t.show();
			}		
			first=false;
		}
		else {
			Log.d(activityNameTag, "Decided that isUseUsage(this) must be false, or first is false?? IsUseUsage was " + Manager.isUseUsage(this) + ", first was " + first + ".");
		}
		waitmessage.sendEmptyMessage(0);
		
		FlurryAgent.onStartSession(this, Manager.getFlurryAPIK());
		Log.v(activityNameTag, "Starting Flurry session with APIK " + Manager.getFlurryAPIK());
	}
	
	private Handler waitmessage = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Toast t;
			while (!Manager.messageQueue.empty()) {
				t= Toast.makeText(me, Manager.messageQueue.pop(), Toast.LENGTH_LONG);
				t.show();
			}
		}
	};
	
	public static CustomList single() {
		return me;
	}
	
	/** This is added to make this AdWhirlInterface class implemented.
	 * 
	 */
	/*public void adWhirlGeneric() {
		// TODO Auto-generated method stub
		
	}*/
	
}
