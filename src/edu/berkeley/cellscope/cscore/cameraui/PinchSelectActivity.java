package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class PinchSelectActivity extends Activity {
	public static final int SELECT_ZOOM = 1;
	public static final int SELECT_EXPOSURE = 2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pinch_select);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pinch_select, menu);
		return true;
	}

	public void enablePinchZoom(View view) {
		setResult(SELECT_ZOOM);
		finish();
	}
	
	public void enablePinchExposure(View view) {
		setResult(SELECT_EXPOSURE);
		finish();
	}
}
