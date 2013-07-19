package edu.berkeley.cellscope.cscore.celltracker.tracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.celltracker.tracker.CellDetection.ContourData;

public class ImageNoiseView extends RelativeLayout {
	SeekBar thresholder;
	TextView text;
	CellDetectActivity activity;
	Context context;
	ContourData contours, original;
	public ImageNoiseView(Context context) {
		super(context);
		this.context = context;
	}

	public ImageNoiseView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public ImageNoiseView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	public void init(CellDetectActivity act, ContourData con) {
		activity = act;
		original = con;
		contours = original.copy();
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.cell_noise, null);
		addView(v);

		thresholder = (SeekBar)(findViewById(R.id.noise_threshold));
		text = (TextView)(findViewById(R.id.noise_threshold_text));

		thresholder.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				text.setText("Noise Size: " + thresholder.getProgress());
			}
			public void onStartTrackingTouch(SeekBar seekbar) {}
			public void onStopTrackingTouch(SeekBar seekbar) {
				update();
			}
		});
		thresholder.setProgress(1);
		update();
	}

	public int getThreshold() {
		return thresholder.getProgress();
	}

	public void update() {
		contours.release();
		contours = CellDetection.removeNoise(original.copy(), getThreshold());
		activity.setDisplay(contours.bw);
		activity.drawDisplay();
	}
}
