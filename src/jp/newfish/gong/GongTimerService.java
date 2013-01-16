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
	public static final int ACTION_TYPE_REMAIN_TIME = 1;
	public static final int ACTION_TYPE_GONG = 2;
	public static final int ACTION_TYPE_GONG_TIMER_SERVICE_STATE = 3;
	public static final String ACTION_EXTRA_REMAIN_TIME = "remainTime";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE = "gongTimerServiceState";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_START = "start";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_SUSPEND = "suspend";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_RESUME = "resume";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_STOP = "stop";
	public static final String ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_SKIP = "skip";
	private Timer lightningTimer = null;
	private GongTimerTask gongTimerTask = null;
	final private long gongTimerTaskIntervalMtime = 50;
	private long gongIntervalMtime = 300*1000;
	private long gongStartTimeMillis = 0;
	private long previousTotalMtime = 0;
	private long gongTimes = 1;
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
		start(false);
	}
	
	private void start(final boolean isSkip) {
		Log.d("GongTimerService::start", "called.");
		
		if (isRunning()) {
			Log.d("GongTimerService::start", "Gong Service has been already started.");
			return;
		}

		final String timerServiceState = previousTotalMtime == 0 ?
				GongTimerService.ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_START :
				GongTimerService.ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_RESUME;
		if (!isSkip) {
			notifyGongTimerState(timerServiceState);
		}

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
				(gongIntervalMtime - previousTotalMtime % gongIntervalMtime);
          
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstGongTime, gongIntervalMtime, sender);  
	}

	private void cancelGongAlerm() {
		final Intent intent = new Intent(this, GongReceiver.class);
		final PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

		final AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(sender);
	}

	synchronized private void startGongTimer() {
		if (lightningTimer == null) {
			lightningTimer = new Timer(false);
			gongTimerTask = new GongTimerTask();
			
			lightningTimer.schedule(gongTimerTask, 0, gongTimerTaskIntervalMtime);
		}
	}

	synchronized private void cancelGongTimer() {
		if (lightningTimer != null) {
			lightningTimer.cancel();
			lightningTimer = null;
		}
	}

	private void clearGongNotification() {
		final NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}
	
	public boolean isRunning() {
		return lightningTimer != null ? true : false;
	}

	public boolean isSuspending() {
		return previousTotalMtime > 0 && !isFinished() ? true : false;
	}

	public boolean isFinished() {
		return getCurrentMtime() >= gongStopTimeMtime ? true : false;
	}

	public boolean isPrefsChanged() {
		return (gongIntervalMtime != (Prefs.getGongInterval(this).longValue() * 1000) ||
			gongTimes != (Prefs.getGongTimes(this).longValue()));
	}

	public long getRemainMtime() {
		return gongIntervalMtime - (getCurrentMtime() % gongIntervalMtime);
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
		stop(false);
	}
	public void stop(final boolean isSkip) {
		if (isRunning()) {
			previousTotalMtime = getCurrentMtime();
			clearGongNotification();
			cancelGongAlerm();
			cancelGongTimer();
			if (isSkip) {
				return;
			} else if (previousTotalMtime < gongStopTimeMtime) {
				notifyGongTimerState(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_SUSPEND);
			} else {
				notifyGongTimerState(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_STOP);
			}
		}
	}
	
	public void skip() {
		stop(true);
		previousTotalMtime = getGongNextMtime();
		gongStartTimeMillis = SystemClock.elapsedRealtime();
		if (previousTotalMtime < gongStopTimeMtime) {
			start(true);
			notifyGongTimerState(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_SKIP);
		} else {
			notifyGongTimerState(ACTION_EXTRA_GONG_TIMER_SERVICE_STATE_STOP);
			notifyRemainMtime(0);
		}
	}

	public void reset() {
		previousTotalMtime = 0;
		checkPrefs();
		notifyRemainMtime(getRemainMtime());
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

	private void notifyRemainMtime(final long remainMtime) {
		final Intent gongIntervalIntent = new Intent(ACTION);
		gongIntervalIntent.putExtra(ACTION_EXTRA_ACTION_TYPE, GongTimerService.ACTION_TYPE_REMAIN_TIME);
		gongIntervalIntent.putExtra(ACTION_EXTRA_REMAIN_TIME, remainMtime);
		gongIntervalIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		sendBroadcast(gongIntervalIntent);
	}

	private class GongTimerTask extends TimerTask {

		@Override
		public void run() {
			final long currentMtime = getCurrentMtime();
			long notifyRemainMtime = getRemainMtime();
			if (currentMtime >= gongStopTimeMtime) {
				previousTotalMtime = gongStopTimeMtime;
				notifyRemainMtime = 0;
				stop();
			}
			notifyRemainMtime(notifyRemainMtime);
		}
		
	}
}

