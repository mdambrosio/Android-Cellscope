package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.ScreenDimension;
import edu.berkeley.cellscope.cscore.cameraui.CompoundTouchListener;
import edu.berkeley.cellscope.cscore.cameraui.SlideableStage;
import edu.berkeley.cellscope.cscore.cameraui.TouchSlideControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchZoomControl;
import edu.berkeley.cellscope.cscore.cameraui.ZoomablePreview;
import edu.berkeley.cellscope.cscore.celltracker.Colors;
import edu.berkeley.cellscope.cscore.celltracker.MathUtils;

public class ViewFieldActivity extends Activity implements ZoomablePreview, SlideableStage {
	private List<Rect> regions;
	private Bitmap display, image;
	private Mat img;
	private ImageView view;
	private Point center;
	private int radius, imWidth, imHeight;
	private double cellSizeLower, cellSizeUpper;
	private int mode;
	private Rect selected;
	private int margin;


	public static final String FOV_X_INFO = "cx";
	public static final String FOV_Y_INFO = "cy";
	public static final String FOV_RADIUS_INFO = "cr";
	public static final String IMG_WIDTH_INFO = "imwidth";
	public static final String IMG_HEIGHT_INFO = "imheight";
	public static final String PARAM_MARGIN_INFO = "margin";
	public static final String PARAM_SIZE_LOWER_INFO = "size lower";
	public static final String PARAM_SIZE_UPPER_INFO = "size upper";
	
	private static final double RESIZE_SENSITIVITY = 0.1;
	static final int APPROXIMATE_TOUCH = 20;
	
	private static double CELL_SIZE_LOWER = 0.31;
	private static double CELL_SIZE_UPPER = 3.24;//1.8 times along one dimension
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fieldview);
		view = (ImageView)(findViewById(R.id.fieldview_display));

		Intent intent = getIntent();
		String file = intent.getStringExtra(InitialCameraActivity.TEMP_PATH_INFO);
		try {
			FileInputStream fis = openFileInput(file);
			image = BitmapFactory.decodeStream(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		imWidth = image.getWidth();
		imHeight = image.getHeight();
	
		img = new Mat();
		regions = new ArrayList<Rect>();
		int[] x = intent.getIntArrayExtra(CellDetectActivity.DATA_X_INFO);
		int[] y = intent.getIntArrayExtra(CellDetectActivity.DATA_Y_INFO);
		int[] w = intent.getIntArrayExtra(CellDetectActivity.DATA_W_INFO);
		int[] h = intent.getIntArrayExtra(CellDetectActivity.DATA_H_INFO);
		
		int size = x.length;
		double aveSize = 0;
		for (int i = 0; i < size; i ++) {
			regions.add(new Rect(x[i], y[i], w[i], h[i]));
			aveSize += w[i] * h[i];
		}
		aveSize /= size;
		cellSizeLower = aveSize * CELL_SIZE_LOWER;
		cellSizeUpper = aveSize * CELL_SIZE_UPPER;
		
		
		CompoundTouchListener compound = new CompoundTouchListener();
		TouchZoomControl ctrl = new TouchZoomControl(this);
		ctrl.setEnabled(true); //We use TouchZoomControl for its response to pinches.
		compound.addTouchListener(ctrl);
		TouchSlideControl slide = new TouchSlideControl(this, this);
		slide.setEnabled(true);
		compound.addTouchListener(slide);
		view.setOnTouchListener(compound);
		
		center = new Point(imWidth / 2, imHeight /2 );
		radius = imHeight / 2;
		

		updateDisplay();
	}
	
	public void updateDisplay() {
		if (display != null)
			display.recycle();
		display = image.copy(Bitmap.Config.ARGB_8888, true);
		Utils.bitmapToMat(display, img);
		Core.circle(img, center, radius, Colors.GREEN);
		for (Rect r: regions) {
			if (r == selected)
				Core.rectangle(img, r.tl(), r.br(), Colors.BLUE, 2);
			else if (MathUtils.circleContainsRect(r, center, radius))
				Core.rectangle(img, r.tl(), r.br(), Colors.GREEN, 2);
			else
				Core.rectangle(img, r.tl(), r.br(), Colors.RED, 2);
				
		}
		Utils.matToBitmap(img, display);
		view.setImageBitmap(display);
	}
	
	@Override
	public void finish() {
		img.release();
		image.recycle();
		display.recycle();
		super.finish();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_viewfield, menu);
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int id = item.getItemId();
    	if (id == R.id.viewfield_finish)
    		complete();
    	else {
	    	mode = id;
	    	selected = null;
	    	updateDisplay();
    	}
        return true;
    }
    
    public void complete() {
/*    	int size = regions.size();

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
			*/
		Intent intent = new Intent(this, CellTrackerActivity.class);
		Intent source = getIntent();
		source.removeExtra(CellDetectActivity.DATA_X_INFO);
		source.removeExtra(CellDetectActivity.DATA_Y_INFO);
		source.removeExtra(CellDetectActivity.DATA_W_INFO);
		source.removeExtra(CellDetectActivity.DATA_H_INFO);
		intent.putExtras(source);
		/*intent.putExtra(CellDetectActivity.DATA_X_INFO, x);
		intent.putExtra(CellDetectActivity.DATA_Y_INFO, y);
		intent.putExtra(CellDetectActivity.DATA_W_INFO, w);
		intent.putExtra(CellDetectActivity.DATA_H_INFO, h);*/
		intent.putExtra(FOV_X_INFO, (int)center.x);
		intent.putExtra(FOV_Y_INFO, (int)center.y);
		intent.putExtra(FOV_RADIUS_INFO, (int)radius);
		intent.putExtra(IMG_HEIGHT_INFO, imHeight);
		intent.putExtra(IMG_WIDTH_INFO, imWidth);
		intent.putExtra(PARAM_MARGIN_INFO, margin);
		intent.putExtra(PARAM_SIZE_UPPER_INFO, cellSizeUpper);
		intent.putExtra(PARAM_SIZE_LOWER_INFO, cellSizeLower);
		startActivity(intent);
		finish();
    }

	public double getDiagonal() {
		return ScreenDimension.getScreenDiagonal(this);
	}

	public double getMaxZoom() {
		int width = imWidth;
		int height = imHeight;
		if (height < width)
			return height * RESIZE_SENSITIVITY;
		else
			return width * RESIZE_SENSITIVITY;
	}

	public void zoom(int amount) {
		if (mode == R.id.viewfield_resize_all) {
			amount = amount / 2 * 2;
			margin += amount;
			for (Rect r: regions) {
				resizeRect(r, amount);
			}
		}
		else if( mode == R.id.viewfield_fieldvision) {
			radius += amount;
			if (radius < 0)
				radius = 0;
			if (radius > imHeight / 2)
				radius = imHeight / 2;
			else if (radius > imWidth / 2 && imWidth < imHeight)
				radius = imWidth / 2;
			if (center.x - radius <= 0 && center.x < radius)
				center.x -= (center.x - radius) / 2;
			if (center.y - radius <= 0 && center.y < radius)
				center.y -= (center.y - radius) / 2;
			if (center.x + radius > imWidth && imWidth - center.x < radius)
				center.x -= (center.x + radius - imWidth) / 2;
			if (center.y + radius > imHeight && imHeight - center.y < radius)
				center.y -= (center.y + radius - imHeight)/ 2;
		}
		updateDisplay();
	}
	
	public void slide(double x, double y) {
		if (mode == R.id.viewfield_fieldvision) {
			if (Math.abs(x) > Math.abs(y)) {
				center.x += x * RESIZE_SENSITIVITY;
				if (center.x < radius)
					center.x = radius;
				else if (center.x > imWidth - radius)
					center.x = imWidth - radius;
			}
			else {
				center.y += y * RESIZE_SENSITIVITY;
				if (center.y < radius)
					center.y = radius;
				else if (center.y > imHeight - radius)
					center.y = imHeight - radius;
			}
		}
		updateDisplay();
	}
	
	public void resizeRect(Rect rect, int amount) {
		resizeRect(rect, amount, amount);
	}
	
	public void resizeRect(Rect rect, int x, int y) {
		MathUtils.resizeRect(rect, x, y);
		MathUtils.cropRectToRegion(rect, imWidth, imHeight);
	}
	
	
	public void translateRect(Rect rect, int x, int y) {
		rect.x += x;
		rect.y += y;
		MathUtils.cropRectToRegion(rect, imWidth, imHeight);
	}
}
