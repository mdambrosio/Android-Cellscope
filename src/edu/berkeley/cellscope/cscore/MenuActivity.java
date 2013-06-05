package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import edu.berkeley.cellscope.cscore.celltracker.OpenCVCameraActivity;

public class MenuActivity extends Activity {
    
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_menu);
    }
    
    /*
     * This is automatically called when the application is opened
     * or resumed.
     */
    public void onResume() {
    	super.onResume();
    }
    
    public void onPause() {
    	super.onPause();
    }
    
    public void goToCamera(View v) {
    	Intent intent = new Intent(this, CameraActivity.class);
    	startActivity(intent);
    }
    
    public void goToPhotoLibrary(View v) {
    	Intent intent = new Intent(this, PhotoLibraryActivity.class);
    	startActivity(intent);
    }
    
    public void goToVideoLibrary(View v) {
    	Intent intent = new Intent(this, VideoLibraryActivity.class);
    	startActivity(intent);
    }
    
    public void goToBluetooth(View v) {
    	Intent intent = new Intent(this, BluetoothActivity.class);
    	startActivity(intent);
    }
    
    public void goToOpenCVCamera(View v) {
    	Intent intent = new Intent(this, OpenCVCameraActivity.class);
    	startActivity(intent);
    }
}
