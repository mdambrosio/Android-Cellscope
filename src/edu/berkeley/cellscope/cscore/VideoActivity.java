package edu.berkeley.cellscope.cscore;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.VideoView;

public class VideoActivity extends Activity implements Runnable {
	VideoView video;
	SeekBar seek;
	Thread thread;
	boolean running;
	public static final int TIMESTEP = 250;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        video = (VideoView)findViewById(R.id.videoView);
        seek = (SeekBar)findViewById(R.id.videoSeek);
        
        Intent intent = getIntent();
        String path = intent.getStringExtra(VideoLibraryActivity.PATH_INFO);
        System.out.println(path);
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        video.setVideoURI(uri);
        seek.setMax(video.getDuration());
        seek.setProgress(1);
        seek.incrementProgressBy(TIMESTEP);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        	boolean wasPlaying;
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				if (fromUser) {
					seekbar.setProgress(progress);
					video.seekTo(progress);
				}
			}

			public void onStartTrackingTouch(SeekBar seekbar) {
				if (video.isPlaying()) {
					video.pause();
					wasPlaying = true;
				}
				else
					wasPlaying = false;
			}

			public void onStopTrackingTouch(SeekBar seekbar) {
				if (wasPlaying) {
					startVideo();
					wasPlaying = false;
				}
				
			}
        	
        });
        thread = new Thread(this);
        thread.start();
        running = true;
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }

	@Override
	public boolean onTouchEvent(MotionEvent evt) {
		if (evt.getAction() != MotionEvent.ACTION_DOWN)
			return true;
		if (video.isPlaying())
			video.pause();
		else
			startVideo();
		return true;
	}
	
	private void startVideo() {
		video.start();
		seek.setMax((int)(video.getDuration() / TIMESTEP) * TIMESTEP);
	} 

	public void run() {
		while (running) {
			if (video.isPlaying()) {
				seek.setProgress(video.getCurrentPosition());
				seek.setMax((int)(video.getDuration() / TIMESTEP) * TIMESTEP);
			}
			try {
				Thread.sleep(TIMESTEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_image, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent returnIntent = new Intent();
    	setResult(RESULT_OK, returnIntent); 
    	finish();
    	return true;
    }
    
}
