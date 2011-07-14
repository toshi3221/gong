package jp.newfish.gong;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

public class GongReceiver extends BroadcastReceiver {
	private static MediaPlayer mediaPlayer = null;
	private Context ctx;
	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.d("GongReceiver::onReceive", "called.");
		this.ctx = ctx;
		startDora();
		notifyGong();
	}

	private void startDora() {
		if (mediaPlayer!=null) {
			mediaPlayer.release();
		}
		mediaPlayer = MediaPlayer.create(ctx, R.raw.dora);
		mediaPlayer.start();
	}

	private void notifyGong() {
		final Intent gongIntervalIntent = new Intent(GongTimerService.ACTION);
		gongIntervalIntent.putExtra(GongTimerService.ACTION_EXTRA_ACTION_TYPE, GongTimerService.ACTION_TYPE_GONG);
		gongIntervalIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.sendBroadcast(gongIntervalIntent);
	}
}
