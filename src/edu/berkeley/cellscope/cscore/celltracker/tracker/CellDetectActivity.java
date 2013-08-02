package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import edu.berkeley.cellscope.cscore.R;

public class CellDetectActivity extends Activity implements View.OnTouchListener {
	Bitmap image;
	Bitmap display;
	ImageProcessView options;
	ImageView preview;
	boolean displayVisible;
	int channel;
	Intent nextIntent;
	
	public static final String DETECT_COLOR_INFO = "filter color";
	public static final String DETECT_GRAYSCALE_INFO = "filter threshold";
	public static final String DETECT_NOISE_INFO = "noise threshold";
	public static final String DETECT_DEBRIS_INFO = "debris threshold";
	public static final String DETECT_BACKGROUND_INFO = "background threshold";
	public static final String DETECT_OBLONG_INFO = "oblong";
	public static final String DATA_X_INFO = "x";
	public static final String DATA_Y_INFO = "y";
	public static final String DATA_W_INFO = "width";
	public static final String DATA_H_INFO = "height";
	public static final String CHANNEL_INFO = "channels";
	public static final String[] CHANNEL_INFO_TAG = new String[]{"c1", "c2", "c3"};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cell_detect);
		Intent intent = getIntent();
		String file = intent.getStringExtra(InitialCameraActivity.TEMP_PATH_INFO);
		try {
			FileInputStream fis = openFileInput(file);
			image = BitmapFactory.decodeStream(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//image = BitmapFactory.decodeResource(getResources(), R.drawable.tracking);
		display = image.copy(Bitmap.Config.ARGB_8888, true);
		preview = (ImageView)(findViewById(R.id.cell_preview));
		preview.setImageBitmap(display);
		
		options = (ImageProcessView)(findViewById(R.id.image_process_view));
		options.init(this);
		
		preview.setOnTouchListener(this);
		displayVisible = false;
		drawImage();
		channel = 0;
	}
	public void drawImage() {
		preview.setImageBitmap(image);
		displayVisible = false;
	}
	
	public void drawDisplay() {
		preview.setImageBitmap(display);
		displayVisible = true;
	}
	
	public void setDisplay(Mat mat) {
	    CellDetection.drawMat(display, mat);
	}
	
	public boolean onTouch(View view, MotionEvent event) {
		if (event.getActionMasked() != MotionEvent.ACTION_DOWN)
			return false;
		if (displayVisible)
			drawImage();
		else
			drawDisplay();
		return true;
	}
	
	public void next(View view) {
		boolean result = options.next();
		if (result) {
			saveParams();
			channel ++;
			if (channel < CHANNEL_INFO_TAG.length)
				options.init(this);
			else
				complete(options.getRects());
		}
	}
	
	public void saveParams() {
		
		if (nextIntent == null) {
			nextIntent = new Intent(this, ViewFieldActivity.class);
			nextIntent.putExtras(getIntent());
		}
		nextIntent.putExtra(DETECT_COLOR_INFO + CHANNEL_INFO_TAG[channel], options.getColorChannel());
		nextIntent.putExtra(DETECT_GRAYSCALE_INFO + CHANNEL_INFO_TAG[channel], options.getColorThreshold());
		nextIntent.putExtra(DETECT_NOISE_INFO + CHANNEL_INFO_TAG[channel], options.getNoiseThreshold());
		nextIntent.putExtra(DETECT_DEBRIS_INFO + CHANNEL_INFO_TAG[channel], options.getDebrisThreshold());
		nextIntent.putExtra(DETECT_BACKGROUND_INFO + CHANNEL_INFO_TAG[channel], options.getBackgroundThreshold());
		nextIntent.putExtra(DETECT_OBLONG_INFO + CHANNEL_INFO_TAG[channel], options.getOblongThreshold());
		nextIntent.putExtra(CHANNEL_INFO, channel);
	}
	
	public void complete(List<Rect> regions) {
		int size = 0;
		if (regions != null)
			size = regions.size();

		int[] x = new int[size];
		int[] y = new int[size];
		int[] w = new int[size];
		int[] h = new int[size];
		for (int i = 0; i < size; i ++) {
			Rect r = regions.get(i);
			x[i] = r.x;
			y[i] = r.y;
			w[i] = r.width;
			h[i] = r.height;
		}

		nextIntent.putExtra(DATA_X_INFO, x);
		nextIntent.putExtra(DATA_Y_INFO, y);
		nextIntent.putExtra(DATA_W_INFO, w);
		nextIntent.putExtra(DATA_H_INFO, h);
		startActivity(nextIntent);
		finish();
	}
	

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_celldetect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.cell_detect_skip) {
    		if (channel != 0) {
    			complete(options.getRects());
    		}
    	}
        return true;
    }
}
