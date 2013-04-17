package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends Activity {

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    
	private Uri fileUri;
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
    
    /*
    public void goToCamera(View v) {
    	//Intent intent = new Intent(this,CameraActivity.class);
    	//startActivity(intent);
    	
    	// create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = CameraActivity.getOutputMediaFileUri(CameraActivity.MEDIA_TYPE_IMAGE); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }
    
    public void goToVideoRecorder(View v) {
    	//Intent intent = new Intent(this,CameraActivity.class);
    	//startActivity(intent);
    	
    	// create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        fileUri = CameraActivity.getOutputMediaFileUri(CameraActivity.MEDIA_TYPE_VIDEO); // create a file to save the image
        System.out.println(fileUri.getPath());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        
        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	System.out.println("DONE");
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image saved", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Video captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Video saved", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                // Video capture failed, advise user
            }
        }
    }
*/
}
