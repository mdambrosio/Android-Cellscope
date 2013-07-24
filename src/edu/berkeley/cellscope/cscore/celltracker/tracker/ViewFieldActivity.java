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
import android.view.MotionEvent;
import android.view.View;
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

public class ViewFieldActivity extends Activity implements ZoomablePreview, SlideableStage, View.OnTouchListener {
	List<Rect> regions;
	String file;
	Bitmap display, image;
	Mat img;
	ImageView view;
	Point center;
	int radius;
	int imWidth, imHeight;
	int mode;
	Rect selected;
	double touchX, touchY;
	int zoom, exposure;

	private int screenWidth, screenHeight;
	
	public static final String DATA_X_INFO = "x";
	public static final String DATA_Y_INFO = "y";
	public static final String DATA_W_INFO = "width";
	public static final String DATA_H_INFO = "height";
	public static final String FOV_X_INFO = "cx";
	public static final String FOV_Y_INFO = "cy";
	public static final String FOV_RADIUS_INFO = "cr";
	public static final String IMG_WIDTH_INFO = "imwidth";
	public static final String IMG_HEIGHT_INFO = "imheight";
	
	private static final double NEW_DEFAULT_SIZE = 40;
	private static final double RESIZE_SENSITIVITY = 0.1;
	private final int firstTouchEvent = -1;
	private static final int APPROXIMATE_TOUCH = 20;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fieldview);
		view = (ImageView)(findViewById(R.id.fieldview_display));

		Intent intent = getIntent();
		file = intent.getStringExtra(InitialCameraActivity.TEMP_PATH_INFO);
		try {
			FileInputStream fis = openFileInput(file);
			image = BitmapFactory.decodeStream(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		imWidth = image.getWidth();
		imHeight = image.getHeight();
	
		zoom = intent.getIntExtra(InitialCameraActivity.CAM_ZOOM_INFO, 0);
		exposure = intent.getIntExtra(InitialCameraActivity.CAM_EXPOSURE_INFO, 0);
		
		img = new Mat();
		regions = new ArrayList<Rect>();
		int[] x = intent.getIntArrayExtra(DATA_X_INFO);
		int[] y = intent.getIntArrayExtra(DATA_Y_INFO);
		int[] w = intent.getIntArrayExtra(DATA_W_INFO);
		int[] h = intent.getIntArrayExtra(DATA_H_INFO);
		
		int size = x.length;
		for (int i = 0; i < size; i ++) {
			regions.add(new Rect(x[i], y[i], w[i], h[i]));
		}
		
		CompoundTouchListener compound = new CompoundTouchListener();
		TouchZoomControl ctrl = new TouchZoomControl(this);
		ctrl.setEnabled(true); //We use TouchZoomControl for its response to pinches.
		compound.addTouchListener(ctrl);
		TouchSlideControl slide = new TouchSlideControl(this, this);
		slide.setEnabled(true);
		compound.addTouchListener(slide);
		compound.addTouchListener(this);
		view.setOnTouchListener(compound);
		
		center = new Point(imWidth / 2, imHeight /2 );
		radius = imHeight / 2;
		

		screenWidth = ScreenDimension.getScreenWidth(this);
		screenHeight = ScreenDimension.getScreenHeight(this);
		
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
    	int size = regions.size();

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
			
		Intent intent = new Intent(this, CellTrackerActivity.class);
		intent.putExtra(InitialCameraActivity.TEMP_PATH_INFO, file);
		intent.putExtra(InitialCameraActivity.CAM_ZOOM_INFO, zoom);
		intent.putExtra(InitialCameraActivity.CAM_EXPOSURE_INFO, exposure);
		intent.putExtra(ViewFieldActivity.DATA_X_INFO, x);
		intent.putExtra(ViewFieldActivity.DATA_Y_INFO, y);
		intent.putExtra(ViewFieldActivity.DATA_W_INFO, w);
		intent.putExtra(ViewFieldActivity.DATA_H_INFO, h);
		intent.putExtra(ViewFieldActivity.FOV_X_INFO, (int)center.x);
		intent.putExtra(ViewFieldActivity.FOV_Y_INFO, (int)center.y);
		intent.putExtra(ViewFieldActivity.FOV_RADIUS_INFO, (int)radius);
		intent.putExtra(ViewFieldActivity.IMG_HEIGHT_INFO, imHeight);
		intent.putExtra(ViewFieldActivity.IMG_WIDTH_INFO, imWidth);
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
		int newW = rect.width + x;
		int newH = rect.height + y;
		if (newH < 2)
			newH = 2;
		if (newW < 2)
			newW = 2;
		rect.x -= (newW - rect.width) / 2;
		rect.y -= (newH - rect.height) / 2;
		rect.width = newW;
		rect.height = newH;
		fitRect(rect);
	}
	
	private void fitRect(Rect rect) {
		if (rect.x < 0) {
			rect.width += rect.x;
			rect.x = 0;
		}
		if (rect.y < 0) {
			rect.height += rect.y;
			rect.y = 0;
		}
		if (rect.x + rect.width >= imWidth)
			rect.width = imWidth - rect.x - 1;
		if (rect.y + rect.height >= imHeight)
			rect.height = imHeight - rect.y - 1;
	}
	
	public void translateRect(Rect rect, int x, int y) {
		rect.x += x;
		rect.y += y;
		fitRect(rect);
	}
	
	public boolean onTouch(View view, MotionEvent evt) {
		int action = evt.getActionMasked();
		int pointers = evt.getPointerCount();
		if (mode == R.id.viewfield_remove && action == MotionEvent.ACTION_DOWN && pointers == 1) {
			Point pt = convertPoint(evt.getX(), evt.getY());
			if (selected == null) {
				for (Rect r: regions) {
					if (r.contains(pt))
						selected = r;
				}
				if (selected == null) {
					for (Rect r: regions)
						if (Math.abs(pt.x - (r.x + r.width / 2)) < r.width / 2 + APPROXIMATE_TOUCH &&
								Math.abs(pt.y - (r.y + r.height / 2)) < r.height/ 2 + APPROXIMATE_TOUCH)
							selected = r;
				}
			}
			else {
				if (selected.contains(pt))
					regions.remove(selected);
				selected = null;
			}
			updateDisplay();
		}
		else if (mode == R.id.viewfield_add && action == MotionEvent.ACTION_DOWN && pointers == 1) {
			Point pt = convertPoint(evt.getX(), evt.getY());
			regions.add(MathUtils.createCenteredRect(pt, NEW_DEFAULT_SIZE, NEW_DEFAULT_SIZE));
			updateDisplay();
		}
		else if (mode == R.id.viewfield_edit) {
			Point pt = convertPoint(evt.getX(), evt.getY());
			if (pointers == 1) {
				if (action == MotionEvent.ACTION_DOWN) {
					for (Rect r: regions)
						if (r.contains(pt))
							selected = r;
					if (selected != null) {
						touchX = pt.x;
						touchY = pt.y;
					}
					else {
						for (Rect r: regions)
							if (Math.abs(pt.x - (r.x + r.width / 2)) < r.width / 2 + APPROXIMATE_TOUCH &&
									Math.abs(pt.y - (r.y + r.height / 2)) < r.height/ 2 + APPROXIMATE_TOUCH)
								selected = r;
					}
				}
				else if (selected != null && action == MotionEvent.ACTION_MOVE && touchX != firstTouchEvent && touchY != firstTouchEvent) {
					double x = pt.x - touchX;
					double y = pt.y - touchY;
					if (Math.abs(x) > Math.abs(y))
						translateRect(selected, (int)(x * RESIZE_SENSITIVITY), 0);
					else
						translateRect(selected, 0, (int)(y * RESIZE_SENSITIVITY));
					touchX = pt.x;
					touchY = pt.y;
				}
				else {
					touchX = touchY = firstTouchEvent;
				}
			}
			else if (pointers == 2 && selected != null){
				double xDist = Math.abs(evt.getX(0) - evt.getX(1));
				double yDist = Math.abs(evt.getY(0) - evt.getY(1));
				if (action == MotionEvent.ACTION_MOVE) {
					if (touchX != firstTouchEvent && touchY != firstTouchEvent) { //Prevents jumping
						if (Math.abs(xDist) > Math.abs(yDist))
							resizeRect(selected, (int)( (xDist - touchX) * RESIZE_SENSITIVITY) / 2 * 2, 0);
						else
							resizeRect(selected, 0, (int)( (yDist - touchY) * RESIZE_SENSITIVITY) / 2 * 2);
						
					}
					touchX = xDist;
					touchY = yDist;
				}
				else {
					touchX = touchY = firstTouchEvent;
				}
			}
			updateDisplay();
		}
	
		return true;
	}
	
	public Point convertPoint(float tX, float tY) {
		float x = tX / screenWidth;
		float y = tY / screenHeight;
		return new Point(imWidth * x, imHeight * y);
	}
}
