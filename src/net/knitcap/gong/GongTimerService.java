package net.knitcap.gong;

import java.util.Timer;
import java.util.TimerTask;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class GongTimerService extends Service {

	class GongTimerBinder extends Binder {
		
		GongTimerService getService() {
			return GongTimerService.this;
		}
		
	}
	
	public static final String ACTION = "Gong Timer Service";
	public static final String ACTION_EXTRA_ACTION_TYPE = "actionType";
	public static final int ACTION_TYPE_CURRENT_TIME = 1;
	public static final int ACTION_TYPE_GONG = 2;
	public static final String ACTION_EXTRA_CURRENT_TIME = "currentTime";
	private Timer lightningTimer = null;
	private GongTimerTask gongTimerTask = null;
	final private long gongTimerTaskIntervalMtime = 50;
	private long gongIntervalMtime = 300*1000;
	private long gongStartTimeMillis = 0;
	private long previousTotalMtime = 0;

	@Override
	public IBinder onBind(final Intent intent) {
		return new GongTimerBinder();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stop();
	}

	public void start() {
		Log.d("GongTimerService::start", "called.");
		
		if (isRunning()) {
			Log.d("GongTimerService::start", "Gong Service has been already started.");
			return;
		}

		final CharSequence m = previousTotalMtime == 0 ?
				getText(R.string.lightning_gong_start) :
					getText(R.string.lightning_gong_resume);
		Toast.makeText(this, m, Toast.LENGTH_SHORT).show();

		gongStartTimeMillis = SystemClock.elapsedRealtime();
		gongIntervalMtime = Prefs.getGongInterval(this).longValue() * 1000;

		setGongAlerm();
		startGongTimer();
	}
	
	private void setGongAlerm() {
		Log.d("GongTimerService::setGongAlerm", "called.");
		final Intent intent = new Intent(this, GongReceiver.class);
		final PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

		long firstGongTime =
			SystemClock.elapsedRealtime() +
				(previousTotalMtime == 0 ? 0 : (gongIntervalMtime - previousTotalMtime % gongIntervalMtime));
          
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstGongTime, gongIntervalMtime, sender);  
	}

	private void cancelGongAlerm() {
		final Intent intent = new Intent(this, GongReceiver.class);
		final PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

		final AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(sender);
	}

	private void startGongTimer() {
		lightningTimer = new Timer(false);
		gongTimerTask = new GongTimerTask();
		
		lightningTimer.schedule(gongTimerTask, 0, gongTimerTaskIntervalMtime);
	}

	private void cancelGongTimer() {
		lightningTimer.cancel();
		lightningTimer = null;		
	}

	private void clearGongNotification() {
		final NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}
	
	public boolean isRunning() {
		return lightningTimer != null ? true : false;
	}

	public long getCurrentMtime() {
		return previousTotalMtime + (isRunning() ? (SystemClock.elapsedRealtime()-gongStartTimeMillis) : 0);
	}

	public void stop() {
		if (isRunning()) {
			previousTotalMtime = getCurrentMtime();
			cancelGongAlerm();
			cancelGongTimer();
			clearGongNotification();
			Toast.makeText(this, getText(R.string.lightning_gong_suspend), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void reset() {
		previousTotalMtime = 0;
		notifyCurrentMtime(0);
	}

	private void notifyCurrentMtime(final long currentMtime) {
		final Intent gongIntervalIntent = new Intent(ACTION);
		gongIntervalIntent.putExtra(ACTION_EXTRA_CURRENT_TIME, currentMtime);
		gongIntervalIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		sendBroadcast(gongIntervalIntent);
	}

	private class GongTimerTask extends TimerTask {

		@Override
		public void run() {
			notifyCurrentMtime(getCurrentMtime());
		}
		
	}
}

