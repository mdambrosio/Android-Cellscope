package edu.berkeley.cellscope.cscore.celltracker.tracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class ImageProcessView extends RelativeLayout {
	Context context;
	ImageFilterView filter;
	ImageNoiseView noise;
	ImageDebrisView debris;
	ImageBackgroundView background;
	ImageIsolateView isolate;
	ImageOblongView oblong;
	CellDetectActivity activity;
	int index;
	
	private static final int FILTER = 0;
	private static final int NOISE = 1;
	private static final int DEBRIS = 2;
	private static final int BACKGROUND = 3;
	private static final int ISOLATE = 4;
	private static final int OBLONG = 5;
	
	public ImageProcessView(Context context) {
		super(context);
		this.context = context;
	}
	
	public ImageProcessView(Context context, AttributeSet attrs) {
		 super(context, attrs);
		 this.context = context;
	}
	 
	public ImageProcessView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	public void init(CellDetectActivity act) {
		activity = act;
	 	filter = new ImageFilterView(context);
	 	
	 	filter.init(act);
	 	addView(filter);
	 	index = 0;
	 	filter.setVisibility(View.VISIBLE);
	 	
	 	noise = new ImageNoiseView(context);
	 	addView(noise);
	 	noise.setVisibility(View.GONE);
	 	
	 	debris = new ImageDebrisView(context);
	 	addView(debris);
	 	debris.setVisibility(View.GONE);
	 	
	 	background = new ImageBackgroundView(context);
	 	addView(background);
	 	background.setVisibility(View.GONE);
	 	
	 	isolate = new ImageIsolateView(context);
	 	addView(isolate);
	 	isolate.setVisibility(View.GONE);

	 	oblong = new ImageOblongView(context);
	 	addView(oblong);
	 	oblong.setVisibility(View.GONE);
	 	
	}
	 
	 public void next() {
		 index ++;
		 if (index == NOISE) {
			 filter.setVisibility(View.GONE);
			 noise.init(activity, filter.contours);
			 noise.setVisibility(View.VISIBLE);
		 }
		 else if (index == DEBRIS) {
			 noise.setVisibility(View.GONE);
			 debris.init(activity, noise.contours);
			 debris.setVisibility(View.VISIBLE);
		 }
		 else if (index == BACKGROUND) {
			 debris.setVisibility(View.GONE);
			 background.init(activity, debris.contours);
			 background.setVisibility(View.VISIBLE);
		 }
		 else if (index == BACKGROUND) {
			 debris.setVisibility(View.GONE);
			 background.init(activity, debris.contours);
			 background.setVisibility(View.VISIBLE);
		 }
		 else if (index == ISOLATE) {
			 background.setVisibility(View.GONE);
			 isolate.init(activity, background.contours);
			 isolate.setVisibility(View.VISIBLE);
		 }
		 else if (index == OBLONG) {
			 isolate.setVisibility(View.GONE);
			 oblong.init(activity, isolate.contours);
			 oblong.setVisibility(View.VISIBLE);
		 }
		 else {
			 index --;
		 }
	 }
}