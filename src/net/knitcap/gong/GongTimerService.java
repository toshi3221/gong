package net.knitcap.gong;

import java.util.Timer;
import java.util.TimerTask;

import net.knitcap.gong.R;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class GongTimerService extends Service {

	class GongTimerBinder extends Binder {
		
		GongTimerService getService() {
			return GongTimerService.this;
		}
		
	}
	
	public static final String ACTION = "Gong Timer Service";
	private MediaPlayer mediaPlayer = null;
	private Timer lightningTimer = null;
	private GongTimerTask gongTimerTask = null;
	final private long gongTimerTaskIntervalMtime = 100;
	private long gongIntervalMtime = 300*1000;
	private long currentMtime = 0;

	@Override
	public IBinder onBind(final Intent intent) {
		return new GongTimerBinder();
	}

	@Override
	public void onStart(Intent intent, int startId) {
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stop();
	}

	public void start() {
		if (lightningTimer == null) {
			final String m = currentMtime == 0 ?
					"Lightning Gong start..." :
					"Lightning Gong resume...";
			Toast.makeText(this, m, Toast.LENGTH_LONG).show();
		} else {
			lightningTimer.cancel();
		}
		lightningTimer = new Timer(true);
		gongTimerTask = new GongTimerTask();
		gongIntervalMtime = Prefs.getGongInterval(this).longValue() * 1000;
		lightningTimer.schedule(gongTimerTask, 0, gongTimerTaskIntervalMtime);
	}
	
	private void clearGongNotification() {
		final NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}
	
	public boolean isRunning() {
		return lightningTimer != null ? true : false;
	}

	public long getCurrentMtime() {
		return currentMtime;
	}

	public void stop() {
		if (lightningTimer != null) {
			lightningTimer.cancel();
			lightningTimer = null;
			clearGongNotification();
		}
	}
	
	public void reset() {
		currentMtime = 0;
		notifyCurrentMtime();
	}

	private void gongInterval() {
		if (currentMtime % gongIntervalMtime == 0) {
			mediaPlayer = MediaPlayer.create(this, R.raw.dora);
			mediaPlayer.start();
		}
		currentMtime += gongTimerTaskIntervalMtime;
		notifyCurrentMtime();
	}

	private void notifyCurrentMtime() {
		final Intent gongIntervalIntent = new Intent(ACTION);
		gongIntervalIntent.putExtra("currentMtime", currentMtime);
		sendBroadcast(gongIntervalIntent);
	}

	private class GongTimerTask extends TimerTask {

		@Override
		public void run() {
			gongInterval();
		}
		
	}
}