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

public class ImageFilterView extends RelativeLayout {
	SeekBar thresholder;
	TextView text;
	RadioGroup radio;
	CellDetectActivity activity;
	Context context;
	ContourData contours;
	public ImageFilterView(Context context) {
		super(context);
		this.context = context;
	}
	
	 public ImageFilterView(Context context, AttributeSet attrs) {
		 super(context, attrs);
		 this.context = context;
	 }
	 
	 public ImageFilterView(Context context, AttributeSet attrs, int defStyle) {
		 super(context, attrs, defStyle);
		 this.context = context;
	 }
	 
	 public void init(CellDetectActivity act) {
		 activity = act;
		 LayoutInflater inflater = LayoutInflater.from(context);
		 View v = inflater.inflate(R.layout.cell_filter, null);
		 addView(v);

		thresholder = (SeekBar)(findViewById(R.id.filter_threshold));
		text = (TextView)(findViewById(R.id.filter_threshold_text));
		radio = (RadioGroup)(findViewById(R.id.filter_group));
		
		radio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				update();
			}
		});

        thresholder.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				text.setText("Threshold: " + thresholder.getProgress());
				update();
			}

			public void onStartTrackingTouch(SeekBar seekbar) {}
			public void onStopTrackingTouch(SeekBar seekbar) {}
        	
        });
        thresholder.setProgress(128);
        update();
	 }
	 
	 public int getThreshold() {
		 return thresholder.getProgress();
	 }
	 
	 public int getChannel() {
		 int id = radio.getCheckedRadioButtonId();
		 if (id == R.id.filter_red)
			 return CellDetection.CHANNEL_RED;
		 else if (id == R.id.filter_green)
			 return CellDetection.CHANNEL_GREEN;
		 else
			 return CellDetection.CHANNEL_BLUE;
	 }
	 
	public void update() {
		if (contours != null)
			contours.release();
	    contours = CellDetection.filterImage(activity.image, getChannel(), getThreshold());
	    activity.setDisplay(contours.bw);
	    activity.drawDisplay();
	}

}
