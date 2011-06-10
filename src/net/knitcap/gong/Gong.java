package net.knitcap.gong;

import java.util.Timer;
import java.util.TimerTask;

import net.knitcap.gong.R;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Gong extends Activity implements OnClickListener {
	
	private MediaPlayer mediaPlayer = null;
	private Timer lightningTimer = null;
	private AsyncTask<String, Object, String> gongTimeSetTextAsyncTask = null;
	private GongTimerTask gongTimerTask = null;
	final private long gongTimerTaskIntervalMtime = 100;
	final private long gongIntervalMtime = 10*1000;
	private long currentMtime = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
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

	public void onClick(View v) {
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