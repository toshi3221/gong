package jp.newfish.gong;

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
	public static final int ACTION_TYPE_GONG_TIMER_SERVICE_STATE = 3;
	public static final String ACTION_EXTRA_CURRENT_TIME = "currentTime";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE = "gongTimerServiceState";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_START = "start";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_SUSPEND = "suspend";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_RESUME = "resume";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_STOP = "stop";
	private Timer lightningTimer = null;
	private GongTimerTask gongTimerTask = null;
	final private long gongTimerTaskIntervalMtime = 50;
	private long gongIntervalMtime = 300*1000;
	private long gongStartTimeMillis = 0;
	private long previousTotalMtime = 0;
	private long gongTimes = 11;
	final static long NEXT_GONG_TIME_ALREADY_FINISHED = -1;
	private long gongStopTimeMtime = gongIntervalMtime * gongTimes;

	@Override
	public IBinder onBind(final Intent intent) {
		return new GongTimerBinder();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		gongIntervalMtime = Prefs.getGongInterval(this).longValue() * 1000;
		gongTimes = Prefs.getGongTimes(this).longValue();
		gongStopTimeMtime = gongIntervalMtime * gongTimes;
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

		final String timerServiceState = previousTotalMtime == 0 ?
				GongTimerService.ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_START :
				GongTimerService.ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_RESUME;
		notifyGongTimerState(timerServiceState);

		gongStartTimeMillis = SystemClock.elapsedRealtime();
		checkPrefs();

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

	public boolean isFinished() {
		return getCurrentMtime() >= gongStopTimeMtime ? true : false;
	}

	public boolean isPrefsChanged() {
		return (gongIntervalMtime != (Prefs.getGongInterval(this).longValue() * 1000) ||
			gongTimes != (Prefs.getGongTimes(this).longValue()));
	}

	public long getCurrentMtime() {
		return previousTotalMtime + (isRunning() ? (SystemClock.elapsedRealtime()-gongStartTimeMillis) : 0);
	}

	public long getGongNextMtime() {
		if (getCurrentMtime() >= gongStopTimeMtime) {
			return 0;
		}
		return (getCurrentMtime()/gongIntervalMtime + 1)*gongIntervalMtime;
	}

	public long getTotalGongTimes() {
		return getCurrentMtime()/gongIntervalMtime;
	}

	public long getGongTimes() {
		return gongTimes;
	}

	public void stop() {
		if (isRunning()) {
			previousTotalMtime = getCurrentMtime();
			clearGongNotification();
			cancelGongAlerm();
			cancelGongTimer();
			if (previousTotalMtime < gongStopTimeMtime) {
				notifyGongTimerState(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_SUSPEND);
			} else {
				notifyGongTimerState(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_STOP);
			}
		}
	}
	
	public void reset() {
		previousTotalMtime = 0;
		checkPrefs();
		notifyCurrentMtime(0);
	}

	private void checkPrefs() {
		gongIntervalMtime = Prefs.getGongInterval(this).longValue() * 1000;
		gongTimes = Prefs.getGongTimes(this).longValue();
		gongStopTimeMtime = gongIntervalMtime * gongTimes;
	}

	private void notifyGongTimerState(final String gongTimerState) {
		final Intent gongIntervalIntent = new Intent(ACTION);
		gongIntervalIntent.putExtra(ACTION_EXTRA_ACTION_TYPE, GongTimerService.ACTION_TYPE_GONG_TIMER_SERVICE_STATE);
		gongIntervalIntent.putExtra(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE, gongTimerState);
		gongIntervalIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		sendBroadcast(gongIntervalIntent);		
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
			long notifyCurrentMtime = getCurrentMtime();
			if (notifyCurrentMtime >= gongStopTimeMtime) {
				previousTotalMtime = gongStopTimeMtime;
				notifyCurrentMtime = gongStopTimeMtime;
				stop();
			}
			notifyCurrentMtime(notifyCurrentMtime);
		}
		
	}
}

