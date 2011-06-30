package net.knitcap.gong;

import net.knitcap.gong.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class Gong extends Activity implements OnClickListener {

	private class GongTimerReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getIntExtra("actionType", GongTimerService.ACTION_TYPE_CURRENT_TIME)) {
			case GongTimerService.ACTION_TYPE_CURRENT_TIME:
				setTimerText(intent.getLongExtra(GongTimerService.ACTION_EXTRA_CURRENT_TIME, 0));
				break;
			case GongTimerService.ACTION_TYPE_GONG:
				Log.d("Gong::GongTimerReceiver.onReceive()", "called.");
				shakeView();
			}
			
		}
	}
	
	private GongTimerService gongTimerService = null;
	private final GongTimerReceiver receiver = new GongTimerReceiver();
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d("ServiceConnection::onServiceConnected", "called.");
			gongTimerService = ((GongTimerService.GongTimerBinder)service).getService();
			initTimerView();
		}
		
		public void onServiceDisconnected(final ComponentName className) {
			gongTimerService = null;
		}
		
	};

    @Override
    public void onCreate(final Bundle savedInstanceState) {
    	Log.d("Gong::onCreate", "called.");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); 
   
        final View mainView = findViewById(R.id.main);
        mainView.setOnClickListener(this);
        
        startGongService();
    }

    @Override
    public void onDestroy() {
    	Log.d("Gong::onDestroy", "called.");
    	unregisterReceiver(receiver);
    	unbindService(serviceConnection);
    	super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
    	super.onCreateOptionsMenu(menu);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.reset:
    		resetTimer();
    		break;
    	case R.id.settings:
    		startActivity(new Intent(this, Prefs.class));
    		return true;
    	}
    	return false;
    }

	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.main:
			if (gongTimerService.isRunning()) {
				stop();
			} else {
				start();
			}
			break;
		}
	}
    
	private void start() {
		gongTimerService.start();
		notifyGongRunning();
	}

	private void initTimerView() {
		setTimerText(gongTimerService.getCurrentMtime());
	}
	
	private void setTimerText(final long currentMtime) {
		final long msec = currentMtime % 1000;
		final long sec = currentMtime / 1000 % 60;
		final long minute = currentMtime / 60 /1000;
		final TextView timeTextView = (TextView)findViewById(R.id.time);
		timeTextView.setText((String.format("%02d:%02d.%01d", minute, sec, msec/100)));
	}

	private void shakeView() {
		final View backgroundView = findViewById(R.id.main);
		backgroundView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
	}
	private void startGongService() {
		final Intent intent = new Intent(Gong.this, GongTimerService.class);
		startService(intent);
		final IntentFilter filter = new IntentFilter(GongTimerService.ACTION);
		registerReceiver(receiver, filter);
		
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	private void notifyGongRunning() {
		final NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification n = new Notification(
									R.drawable.icon,
									getText(R.string.lightning_gong_is_running),
									System.currentTimeMillis());
		final CharSequence contentTitle = getText(R.string.app_name);
		final CharSequence contentText = getText(R.string.lightning_gong_is_running);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, getIntent(), 0);
		n.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
		n.flags |= Notification.FLAG_ONGOING_EVENT;
		nm.notify(1, n);
	}
	
	private void clearGongNotification() {
		final NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

	private void resetTimer() {
		if (gongTimerService.isRunning()) {
			stop();
		}
		gongTimerService.reset();
	}
	
	private void stop() {
		gongTimerService.stop();
		clearGongNotification();
	}
}