package net.knitcap.gong;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
	
	public static Long getGongInterval(final Context context) {
		return Long.valueOf(
				PreferenceManager.getDefaultSharedPreferences(context).
					getString("gong_interval", "300"));
	}
}
