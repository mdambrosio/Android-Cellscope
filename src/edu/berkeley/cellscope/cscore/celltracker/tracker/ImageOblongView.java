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

public class ImageOblongView extends RelativeLayout {
	SeekBar thresholder;
	TextView text;
	CellDetectActivity activity;
	Context context;
	ContourData contours, original;
	private static final double STEP_SIZE = 0.5;
	public ImageOblongView(Context context) {
		super(context);
		this.context = context;
	}

	public ImageOblongView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public ImageOblongView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	public void init(CellDetectActivity act, ContourData con) {
		activity = act;
		original = con;
		contours = original.copy();
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.cell_oblong, null);
		addView(v);

		thresholder = (SeekBar)(findViewById(R.id.oblong_threshold));
		text = (TextView)(findViewById(R.id.oblong_threshold_text));

		thresholder.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				text.setText("Oblong Size: " + (int)(getThreshold() * 100) / 100d);
			}
			public void onStartTrackingTouch(SeekBar seekbar) {}
			public void onStopTrackingTouch(SeekBar seekbar) {
				update();
			}
		});
		update();
	}

	public double getThreshold() {
		return thresholder.getProgress() * STEP_SIZE + 1;
	}

	public void update() {
		contours.release();
		contours = CellDetection.removeOblong(original.copy(), getThreshold());
		activity.setDisplay(contours.bw);
		activity.drawDisplay();
	}
}
