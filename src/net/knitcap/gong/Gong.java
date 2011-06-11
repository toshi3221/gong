package net.knitcap.gong;

import java.util.Timer;
import java.util.TimerTask;

import net.knitcap.gong.R;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Gong extends Activity implements OnClickListener {
	
	private MediaPlayer mediaPlayer = null;
	private Timer lightningTimer = null;
	private AsyncTask<String, Object, String> gongTimeSetTextAsyncTask = null;
	private GongTimerTask gongTimerTask = null;
	final private long gongTimerTaskIntervalMtime = 100;
	private long gongIntervalMtime = 300*1000;
	private long currentMtime = 0;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
   
        final View startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this);
        final View stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        final View resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(this);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
    	case R.id.settings:
    		startActivity(new Intent(this, Prefs.class));
    		return true;
    	}
    	return false;
    }

	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.start_button:
			start();
			break;
		case R.id.stop_button:
			stop();
			break;
		case R.id.reset_button:
			reset();
			break;
		}
	}
    
	private void start() {
		if (lightningTimer != null) {
			stop();
		}
		createGongTimeSetTextAsyncTask();
		lightningTimer = new Timer(true);
		gongTimerTask = new GongTimerTask();
		gongIntervalMtime = Prefs.getGongInterval(this).longValue() * 1000;
		lightningTimer.schedule(gongTimerTask, 0, gongTimerTaskIntervalMtime);
	}

	private void reset() {
		if (lightningTimer == null) {
			final TextView timeTextView = (TextView)findViewById(R.id.time);
			timeTextView.setText("00:00.0");
			currentMtime = 0;
		}
	}

	private void createGongTimeSetTextAsyncTask() {
		gongTimeSetTextAsyncTask = new AsyncTask<String, Object, String>() {
			@Override
			protected String doInBackground(String... params) {
				return params[0];
			}
			@Override
			protected void onPostExecute(String result) {
				final TextView timeTextView = (TextView)findViewById(R.id.time);
				if (timeTextView != null) {
					timeTextView.setText(result);
				}
				createGongTimeSetTextAsyncTask();
			}
		};
	}
	
	private void stop() {
		if (lightningTimer != null) {
			lightningTimer.cancel();
			lightningTimer = null;
		}
	}
	
	private void gongInterval() {
		if (currentMtime % gongIntervalMtime == 0) {
			mediaPlayer = MediaPlayer.create(this, R.raw.dora);
			mediaPlayer.start();
		}
		currentMtime += gongTimerTaskIntervalMtime;
		final long msec = currentMtime % 1000;
		final long sec = currentMtime / 1000 % 60;
		final long minute = currentMtime / 60 /1000;
		while (Status.PENDING != gongTimeSetTextAsyncTask.getStatus()) {
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
		gongTimeSetTextAsyncTask.execute(String.format("%02d:%02d.%01d", minute, sec, msec/100));
	}

	private class GongTimerTask extends TimerTask {

		@Override
		public void run() {
			gongInterval();
		}
		
	}
}