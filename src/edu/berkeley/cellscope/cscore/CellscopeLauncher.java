package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class CellscopeLauncher extends Activity {
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_cellscope_launcher);
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
    	Intent intent = new Intent(this,CameraActivity.class);
    	startActivity(intent);
    }
    
    public void goToPhotoLibrary(View v) {
    	Intent intent = new Intent(this,PhotoLibraryActivity.class);
    	startActivity(intent);
    }
    
    public void goToVideoLibrary(View v) {
    	Intent intent = new Intent(this,VideoLibraryActivity.class);
    	startActivity(intent);
    }

}
