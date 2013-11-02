package edu.berkeley.cellscope.cscore.celltracker.mobilestage;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import edu.berkeley.cellscope.cscore.celltracker.MathUtils;

/**
 * Contains data for each individual field of view.
 */
public class MobileFov {
	private Mat image;			//snapshot of the fov
	private Point location;		//the absolute location of fov's center
	private Point center; 		//the fov's center in the snapshot
	private double radius;		//radius of the fov
	private long time;			//time that this fov was taken
//	private List<MobileObj> objects; //list of objects located within this fov
	
	public MobileFov(Point cen, double rad, Mat im, Point loc, long t) {
		location = loc;
		image = im;
		center = cen;
		radius = rad;
		time = t;
	}
	
	public Mat getRegion(Rect roi) {
		return image.submat(roi);
	}
	
	public Point getAbsoluteLocation() {
		return location;
	}
	
	public Point getOffset(Point otherLocation) {
		Point offset = new Point();
		MathUtils.set(offset, location, otherLocation);
		return offset;
	}
	
	public boolean pointWithinBounds(Point pt, double frac) {
		double dstSqr = MathUtils.distSqr(pt, center);
		return dstSqr < Math.pow(radius * frac, 2);
	}
}
