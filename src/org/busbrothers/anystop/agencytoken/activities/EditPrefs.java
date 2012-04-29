package org.busbrothers.anystop.agencytoken.activities;

import org.busbrothers.anystop.agencytoken.R;
import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class EditPrefs extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.prefs);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		
		String stres = "off";
		String s = preference.getKey();
		boolean res = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(s, false);
		stres = res ? "on" : "off";
		
		if (s.equalsIgnoreCase("usage_enabled")) {
			Toast.makeText(this , "Usage messages are now " + stres, Toast.LENGTH_SHORT).show();
		}
		
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	
	
}
