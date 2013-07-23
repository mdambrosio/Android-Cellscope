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
	String file;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cell_detect);
		Intent intent = getIntent();
		file = intent.getStringExtra(InitialCameraActivity.TEMP_PATH_INFO);
		try {
			FileInputStream fis = openFileInput(file);
			image = BitmapFactory.decodeStream(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//image = BitmapFactory.decodeResource(getResources(), R.drawable.celltest);
		display = image.copy(Bitmap.Config.ARGB_8888, true);
		preview = (ImageView)(findViewById(R.id.cell_preview));
		preview.setImageBitmap(display);
		
		options = (ImageProcessView)(findViewById(R.id.image_process_view));
		options.init(this);
		
		preview.setOnTouchListener(this);
		displayVisible = false;
		drawImage();
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
		List<Rect> regions = options.next();
		if (regions != null)
			complete(regions);
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
			
		Intent intent = new Intent(this, ViewFieldActivity.class);
		intent.putExtra(InitialCameraActivity.TEMP_PATH_INFO, file);
		intent.putExtra(ViewFieldActivity.DATA_X_INFO, x);
		intent.putExtra(ViewFieldActivity.DATA_Y_INFO, y);
		intent.putExtra(ViewFieldActivity.DATA_W_INFO, w);
		intent.putExtra(ViewFieldActivity.DATA_H_INFO, h);
		startActivity(intent);
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
    		complete(null);
    	}
        return true;
    }
}
