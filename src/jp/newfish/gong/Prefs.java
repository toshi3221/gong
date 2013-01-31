package jp.newfish.gong;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {

	final static String GONG_INTERVAL_DEFAULT = "300";
	final static String GONG_TIMES_DEFAULT = "1";

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
	
	public static Long getGongInterval(final Context context) {
		String interval = PreferenceManager.getDefaultSharedPreferences(context)
				.getString("gong_interval", GONG_INTERVAL_DEFAULT);
		if (interval == null || interval.length() == 0) { interval = GONG_INTERVAL_DEFAULT; }
		return Long.valueOf(interval);
	}

	public static Long getGongTimes(final Context context) {
		
		String gongTimes = PreferenceManager.getDefaultSharedPreferences(context)
				.getString("gong_times", GONG_TIMES_DEFAULT);
		if (gongTimes == null || gongTimes.length() == 0) { gongTimes = GONG_TIMES_DEFAULT; }
		return Long.valueOf(gongTimes);
	}
}
