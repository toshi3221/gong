package net.knitcap.gong;

import java.util.Timer;
import java.util.TimerTask;

import net.knitcap.gong.R;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class Gong extends Activity implements OnClickListener {
	
	private MediaPlayer mediaPlayer = null;
	private Timer lightningTimer = null;
	private GongTimerTask gongTimerTask = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final View startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this);
        final View stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        
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
		}
	}
    
	private void start() {
		if (lightningTimer != null) {
			stop();
		}
		lightningTimer = new Timer(true);
		gongTimerTask = new GongTimerTask();
		final long gongIntervalMtime = 10*1000;
		lightningTimer.schedule(gongTimerTask, 0, gongIntervalMtime);
	}

	private void stop() {
		lightningTimer.cancel();
	}
	
	private void gong() {
		mediaPlayer = MediaPlayer.create(this, R.raw.dora);
		mediaPlayer.start();
	}

	private class GongTimerTask extends TimerTask {

		@Override
		public void run() {
			gong();
		}
		
	}
}