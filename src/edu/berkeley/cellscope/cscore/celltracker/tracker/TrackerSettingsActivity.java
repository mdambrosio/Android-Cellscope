package edu.berkeley.cellscope.cscore.celltracker.tracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import edu.berkeley.cellscope.cscore.R;

public class TrackerSettingsActivity extends Activity {
	CheckBox toggle;
	EditText nameField, intervalField;
	public static final String SAVE_INFO = "save";
	public static final String INTERVAL_INFO = "interval";
	public static final String TIMELAPSE_INFO = "timelapse";
	public static final int DEFAULT_INTERVAL = 1000;
	public static final int MINIMUM_INTERVAL = 250;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracker_settings);
		toggle = (CheckBox)(findViewById(R.id.tracker_timelapse_checkbox));
		nameField = (EditText)(findViewById(R.id.tracker_name_field));
		intervalField = (EditText)(findViewById(R.id.tracker_timelapse_field));
	}

	public void proceed(View v) {
		Intent intent = new Intent(this, InitialCameraActivity.class);
		String save = nameField.getText().toString();
		String timeStr = intervalField.getText().toString();
		int interval = DEFAULT_INTERVAL;
		if (timeStr.length() > 0) interval = Integer.parseInt(timeStr);
		if (interval < MINIMUM_INTERVAL) interval = MINIMUM_INTERVAL;
		boolean timelapse = toggle.isChecked();
		intent.putExtra(SAVE_INFO, save);
		intent.putExtra(INTERVAL_INFO, interval);
		intent.putExtra(TIMELAPSE_INFO, timelapse);
		startActivity(intent);
	}
}
