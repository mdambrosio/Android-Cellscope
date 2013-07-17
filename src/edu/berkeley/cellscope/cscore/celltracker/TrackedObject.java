package edu.berkeley.cellscope.cscore.celltracker;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


/*
 * Holds information about a tracked object's appearance and location over time.
 */
public class TrackedObject {
	final List<Point> path; //center
	private final Size size;
	//Fields that start with "t" store tentative data from update(), which can be confirmed using confirmUpdate();
	Point position; //top left
	private Point tPosition;
	boolean tracked; //False when position is unknown
	Rect boundingBox, roi;
	private Rect tBoundingBox, tRoi;
	private double currentRoi, tCurrentRoi, minimumRoi;
	private Mat image, tImage; //clipped image of object used for cross-correlation
	double match, tMatch; //Correlation coefficient. Used to resolve conflicts with two objects tracking to the same spot
	
	private Mat corrResult; //Used to store the result of cross-correlation
	
	private static final double TOLERATED_OVERLAP = 0.0;
	private static final double MATCH_TOLERANCE = 0.00001;
	private static final double MATCH_THRESHOLD = 0.95;
	static final int MATCH = 0;
	static final int BETTER = 1;
	static final int WORSE = -1;
	private static final double ROI_SIZE = 4; //Region about each object to check for new positions.
											//Increase to track objects that accelerate suddenly, but will result in longer processing time.
											//0 to disable.
	private static final double MINIMUM_ROI = 3; //Relative to own size.
	private static final int ROI_VARIABILITY = 4; //Number of steps to average to find the updated ROI. Decrease for larger changes.
													//Decrease if object being tracked has high acceleration (but smooth).
	public TrackedObject(Rect location, Mat field) {
		path = new ArrayList<Point>();
		size = location.size();
		position = location.tl();
		path.add(MathUtils.getRectCenter(position, size));
		boundingBox = location.clone();
		image = field.submat(location);
		match = 1;
		currentRoi = 0;
		tracked = true;
		if (size.width < size.height)
			minimumRoi = (int)(size.height * MINIMUM_ROI);
		else
			minimumRoi = (int)(size.width * MINIMUM_ROI);
		int result_cols =  field.cols() - image.cols() + 1;
		int result_rows = field.rows() - image.rows() + 1;
		corrResult = new Mat(result_rows, result_cols, CvType.CV_32FC1);
		roi = null;
	}
	
	//Tenatively updates the position of the object. confirmUpdate() must be called to finalize.
	public void update(Mat field) {
		//Check the entire image
		tracked = true;
		if (ROI_SIZE == 0 || roi == null) {
			Imgproc.matchTemplate(field, image, corrResult, Imgproc.TM_CCORR_NORMED);
			Core.MinMaxLocResult minMax = Core.minMaxLoc(corrResult);
			tPosition = minMax.maxLoc;
			tBoundingBox = new Rect(tPosition, size);
			TrackedField.cropRectToMat(tBoundingBox, field);
			tMatch = minMax.maxVal;
			tImage = field.submat(tBoundingBox);
			updateRoi(field);
		}
		//Check only a subset of the image, determined by roi
		else {
			Mat limitedField = field.submat(roi);
			Imgproc.matchTemplate(limitedField, image, corrResult, Imgproc.TM_CCORR_NORMED);
			Core.MinMaxLocResult minMax = Core.minMaxLoc(corrResult);
			tPosition = minMax.maxLoc;
			tPosition = MathUtils.add(tPosition, roi.tl());
			tBoundingBox = new Rect(tPosition, size);
			TrackedField.cropRectToMat(tBoundingBox, field);
			tMatch = minMax.maxVal;
			tImage = field.submat(tBoundingBox);
			updateRoi(field);
		}
		if (tMatch < MATCH_THRESHOLD)
			invalidateUpdate();
	}
	
	public void updateRoi(Mat field) {
		//System.out.println("UPDATE ROI");
		double range = newStepDistance() * ROI_SIZE * 2;
		if (range == 0)
			return;
		if (tCurrentRoi != 0) {
			int totalPts = path.size();
			if (totalPts > ROI_VARIABILITY)
				totalPts = ROI_VARIABILITY;
			tCurrentRoi = (currentRoi * (totalPts) + range) / (totalPts + 1);
		}
		else
			tCurrentRoi = range;
		if (tCurrentRoi < minimumRoi)
			tCurrentRoi = minimumRoi;
		int roiSize = (int)tCurrentRoi;
		int roiX = (int)(tPosition.x + size.width / 2);
		int roiY = (int)(tPosition.y + size.height / 2);
		tRoi = MathUtils.createCenteredRect(roiX, roiY, roiSize, roiSize);
		TrackedField.cropRectToMat(tRoi, field);
		//System.out.println(tRoi);
	}
	
	public double newStepDistance() {
		if (tPosition == null || position == null)
			return -1;
		return MathUtils.dist(position, tPosition);
	}
	
	public boolean newPosInFov(Point center, double radius) {
		return MathUtils.circleContainsRect(tBoundingBox, center, radius);
	}
	
	public boolean overlapViolation(TrackedObject obj) {
		double x = Math.abs(obj.tPosition.x - this.tPosition.x);
		double y = Math.abs(obj.tPosition.y - this.tPosition.y);
		double w = (obj.size.width > this.size.width) ? obj.size.width :  this.size.width;
		double h = (obj.size.height > this.size.height) ? obj.size.height :  this.size.height;
		return (x < w * TOLERATED_OVERLAP) && (y < h * TOLERATED_OVERLAP);
	}
	/*
	 * Returns false if it is determined that this object is incorrectly tracking onto another object.
	 * Returns true if vice versa.
	 */
	public boolean trackingViolation(TrackedObject obj) {
		//Examine cross-correlation coefficients. Worse coefficient is considered incorrect.
		//If the two coefficients are too similar, examine distance instead.
		double difference = Math.abs(this.tMatch - obj.tMatch);
		if (difference > MATCH_TOLERANCE) {
			if (this.tMatch > obj.tMatch)
				return true;
			else
				return false;
		}
		
		//Examine distance. The object that moved further is considered incorrect.
		if (obj.newStepDistance() > this.newStepDistance())
			return true;
		else
			return false;
	}
	
	public void confirmUpdate() {
		synchronized(this) {
			position = tPosition;
			boundingBox = tBoundingBox;
			roi = tRoi;
			currentRoi = tCurrentRoi;
			image = tImage;
			path.add(MathUtils.getRectCenter(position, size));
			tracked = true;
		}
	}
	
	public void invalidateUpdate() {
		synchronized(this) {
			path.add(null);
			tracked = false;
		}
	}
	
	public void drawInfo(Mat display) {
		synchronized(this) {
			if (!tracked)
				Core.rectangle(display, boundingBox.tl(), boundingBox.br(), TrackedField.RED, 2);
			else
				Core.rectangle(display, boundingBox.tl(), boundingBox.br(), TrackedField.GREEN, 2);
			Point last = path.get(0);
			boolean jump = false;
			for (int i = 1; i < path.size(); i ++) {
				if (path.get(i) != null) {
					if (jump) {
						Core.line(display, path.get(i), last, TrackedField.RED);
						jump = false;
					}
					else
						Core.line(display, path.get(i), last, TrackedField.GREEN);
					last = path.get(i);
				}
				else
					jump = true;
			}
			if (roi != null)
				Core.rectangle(display, roi.tl(), roi.br(), TrackedField.BLUE);
			if (!tracked)
				Core.rectangle(display, tBoundingBox.tl(), tBoundingBox.br(), TrackedField.RED);
		}
	}
}