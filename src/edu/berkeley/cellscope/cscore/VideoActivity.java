package edu.berkeley.cellscope.cscore;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.VideoView;

public class VideoActivity extends Activity {
	VideoView video;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        video = (VideoView)findViewById(R.id.video);
        Intent intent = getIntent();
        String path = intent.getStringExtra(VideoLibraryActivity.PATH_INFO);
        System.out.println(path);
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        video.setVideoURI(uri);
        video.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
        return true;
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
			video.start();
		return true;
	}
    
}
