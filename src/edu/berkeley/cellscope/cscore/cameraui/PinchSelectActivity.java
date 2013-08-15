package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.R;
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

	public void enablePinchZoom(View view) {
		setResult(SELECT_ZOOM);
		finish();
	}
	
	public void enablePinchExposure(View view) {
		setResult(SELECT_EXPOSURE);
		finish();
	}
}
