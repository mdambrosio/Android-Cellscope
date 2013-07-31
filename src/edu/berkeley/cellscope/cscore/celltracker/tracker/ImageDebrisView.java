package edu.berkeley.cellscope.cscore.celltracker.tracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.celltracker.tracker.CellDetection.ContourData;

public class ImageDebrisView extends RelativeLayout {
	SeekBar thresholder;
	TextView text;
	CellDetectActivity activity;
	Context context;
	ContourData contours, original;
	private static final double STEP_SIZE = 0.2;
	public ImageDebrisView(Context context) {
		super(context);
		this.context = context;
	}

	public ImageDebrisView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public ImageDebrisView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	public void init(CellDetectActivity act, ContourData con) {
		activity = act;
		original = con;
		contours = original.copy();
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.cell_debris, null);
		addView(v);

		thresholder = (SeekBar)(findViewById(R.id.debris_threshold));
		text = (TextView)(findViewById(R.id.debris_threshold_text));

		thresholder.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				text.setText("Debris Size: " + (int)(getThreshold() * 100) / 100d);
			}
			public void onStartTrackingTouch(SeekBar seekbar) {}
			public void onStopTrackingTouch(SeekBar seekbar) {
				update();
			}
		});
		thresholder.setProgress((int) (1 / STEP_SIZE));
		update();
	}

	public double getThreshold() {
		return thresholder.getProgress() * STEP_SIZE;
	}

	public void update() {
		contours.release();
		contours = CellDetection.removeDebris(original.copy(), getThreshold());
		activity.setDisplay(contours.bw);
		activity.drawDisplay();
	}
}
