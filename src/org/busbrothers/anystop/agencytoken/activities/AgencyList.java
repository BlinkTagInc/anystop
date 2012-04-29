package org.busbrothers.anystop.agencytoken.activities;

import java.util.Collections;
import java.util.List;

import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.R;
import org.busbrothers.anystop.agencytoken.Manager;
import org.busbrothers.anystop.agencytoken.activities.StopsTime.DataThread;
import org.busbrothers.anystop.agencytoken.datacomponents.Agency;
import org.busbrothers.anystop.agencytoken.datacomponents.Favorites;
import org.busbrothers.anystop.agencytoken.datacomponents.NoneFoundException;
import org.busbrothers.anystop.agencytoken.datacomponents.ServerBarfException;
import org.busbrothers.anystop.agencytoken.uicomponents.CustomList;
import org.busbrothers.anystop.agencytoken.uicomponents.Effects;
import org.busbrothers.anystop.agencytoken.uicomponents.IndexCheck;

import org.busbrothers.anystop.agencytoken.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AgencyList extends CustomList {

	static String keybuffer;
	
	static String agency;

	 
	//TextView selection;
	List<Agency> arr;
	Button mapButton;
	TextView search;
	ProgressDialog pd;
	@Override
	public void onCreate(Bundle icicle) {

		_usageMessages= new String[] {"Choose an agency to view all of its routes.",
				 "You can add an agency to your favorites by clicking on the heart!",
				 "You can turn off screen instructions in the Preferences."
		};
				 
		arr = Manager.agencies;
		if (arr==null) {
			this.setResult(-1);
			this.finish();
		} else {
			Collections.sort(arr);
			setContentView(R.layout.stoplist);
			setListAdapter(new IconicAdapter(this));
		}
		
		super.onCreate(icicle);
	}
	

	public void onListItemClick(ListView parent, View v, int position, long id) {
		super.onListItemClick(parent, v, position, id);
		
		Manager.positionTracker=position;
		this.agency=arr.get(position).name;
		Manager.currAgency = arr.get(position);
		Manager.tableTracker=arr.get(position).table;
		
		v.postDelayed(new Runnable() {
            public void run() {
        		try {
        			handler.sendEmptyMessage(0);
        		} catch (Exception e) {
        			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
        					"Please try again later!", Toast.LENGTH_LONG);
        			t.show();
        		}
            }
        }, super.getAnimationTime());
	}

	class IconicAdapter extends ArrayAdapter<Agency> {
		Activity context;

		IconicAdapter(Activity context) {
			super(context, R.layout.agency_check_sched, arr);

			this.context=context;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater=LayoutInflater.from(context);
			Agency agency = arr.get(position);
			View row=inflater.inflate(R.layout.agency_check_sched, null);
			TextView label=(TextView)row.findViewById(R.id.dir_label);
			IndexCheck check=(IndexCheck)row.findViewById(R.id.favbox);
			TextView content=(TextView)row.findViewById(R.id.dir_subinfo);
			
			check.index = position;
			check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					Agency ag = arr.get(((IndexCheck)buttonView).index);
					if (isChecked) {
						Favorites.getInstance().addAgency(ag);
					} else {
						Favorites.getInstance().removeAgency(ag);
					}
				}
			});
			Favorites favs = Favorites.getInstance();
			check.stopUpdate=true;
			check.setChecked(favs.checkAgency(agency));
			check.stopUpdate=false;
			
			TextView sched = (TextView)row.findViewById(R.id.dir_sched);
			
			if (position % 2 == 0) {
				row.setBackgroundColor(Color.GRAY);
				row.setBackgroundResource(R.drawable.cust_list_selector);
			}

			if (agency.isRT) {
				sched.setText("Real Time Predictions");
				sched.setTextColor(0xFF00DD00);
			} else if (agency.isSpecial) {
				sched.setText("Real-Time & Schedule");
				sched.setTextColor(0xFFA0C700);
			} else {
				sched.setText("Schedule Info");
				sched.setTextColor(0xFFE0B000);
			} 
			content.setText(agency.city + "\n" + agency.state);

			label.setText(agency.name.trim());
			ImageView icon=(ImageView)row.findViewById(R.id.icon);
			icon.setImageResource(R.drawable.rail);
			return(row);
		}
	}

	
	class DataThread extends Thread {
		public DataThread() {

		}
		public void run() {
				try {
					Manager.loadAgencyRoutes(arr.get(Manager.positionTracker));
				} catch (ServerBarfException e) {
					errorHandler.sendEmptyMessage(1); return;
				}
				pd.dismiss();
				stact.sendEmptyMessage(0);

		}
	}
	private Handler handler = new Handler() {
		
		
		@Override
		public void handleMessage(Message msg) {
			
        	DataThread d = new DataThread();
        	pd = ProgressDialog.show(me, "Getting Agency Routes", "Contacting the Server:\nRoutes List for:\n"+agency, true, false);
        	d.start();
		}
	};
	private Handler stact = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==1) {
    			Toast t = Toast.makeText(me, "There seems to be a problem with the Server at this time." +
    					"Please try again later!", Toast.LENGTH_LONG);
    			t.show();
    			return;
			}
				Intent i = new Intent(me, AgencyRouteList.class);
				startActivityForResult(i,0);
		}
	};
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode==-1) {
			showError();
		}
	}
	
	private void showError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Agency Error")
        .setIcon(R.drawable.ico)
        .setMessage("It seems that we cannot find routes for your agency; " +
        		"this is most likely because you are near an agency we do not support, " +
        		"or if could be a problem with the underlying data.\n" +
        		"Our data is constanly improving, so please check back later!")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}

	
	private Handler errorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==0) showError();
			if (msg.what==1) showServError();
			try {
				pd.dismiss();
			} catch (Exception ex) {}
		}
	};
	private void showServError() {
        Builder b = new AlertDialog.Builder(this)
        .setTitle("Problem")
        .setIcon(R.drawable.ico )
        .setMessage("It seems that you are having trouble contacting the server!\n" +
        		"Please try again soon.")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Put your code in here for a positive response
                }
        });
        
        b.show();
	}


	
}
