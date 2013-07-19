package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
		image = BitmapFactory.decodeResource(getResources(), R.drawable.celltest);
		display = image.copy(Bitmap.Config.ARGB_8888, true);
		deleteFile(file);
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
		options.next();
	}
    
}
