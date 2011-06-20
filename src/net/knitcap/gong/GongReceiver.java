package net.knitcap.gong;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

public class GongReceiver extends BroadcastReceiver {
	private static MediaPlayer mediaPlayer = null;
	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.d("GongReceiver::onReceive", "called.");
		if (mediaPlayer!=null) {
			mediaPlayer.release();
		}
		mediaPlayer = MediaPlayer.create(ctx, R.raw.dora);
		mediaPlayer.start();
	}
}
