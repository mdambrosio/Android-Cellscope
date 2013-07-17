package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;

/* Various useful mathematical operations for points/vectors
 * that are not included in OpenCV's Point class.
 */
public class MathUtils {
	
	public static Rect set(Rect rect, Point pt, Size s) {
		rect.x = (int)(pt.x);
		rect.y = (int)(pt.y);
		rect.width = (int)(s.width);
		rect.height = (int)(s.height);
		return rect;
	}
	
	public static boolean circleContainsRect(Rect rect, Point center, double radius) {
		double radius2 = radius * radius;
		Point tl = rect.tl();
		Point br = rect.br();
		if (distSqr(center, tl) > radius2 || 
				distSqr(center, br) > radius2 ||
				distSqr(center, tl.x, br.y) > radius2 ||
				distSqr(center, br.x, tl.y) > radius2)
			return false;
		return true;
	}
	
	public static Point getRectCenter(Point pt, Size s) {
		return new Point(pt.x + s.width / 2, pt.y + s.height / 2);
	}
	
	public static Rect createCenteredRect(Point pt, Size s) {
		return createCenteredRect(pt, s.width, s.height);
	}
	
	public static Rect createCenteredRect(Point pt, double width, double height) {
		return createCenteredRect(pt.x, pt.y, width, height);
	}
	
	public static Rect createCenteredRect(double x, double y, double width, double height) {
		return new Rect((int)(x - width / 2), (int)(y - height / 2), 
				(int)(width), (int)(height));
	}
	
	public static Point add(Point pt, double x, double y) {
		pt.x += x;
		pt.y += y;
		return pt;
	}
	
	public static Point add(Point pt, Point val) {
		return add(pt, val.x, val.y);
	}
	
	public static Point addPolar(Point pt, double r, double a) {
		return add(pt, Math.cos(a) * r, Math.sin(a) * r);
	}
	
	public static Point subtract(Point pt, double x, double y) {
		pt.x -= x;
		pt.y -= y;
		return pt;
	}
	
	public static Point subtract(Point pt, Point val) {
		return subtract(pt, val.x, val.y);
	}

	public static Point subtractPolar(Point pt, double r, double a) {
		return subtract(pt, Math.cos(a) * r, Math.sin(a) * r);
	}
	
	public static Point multiply(Point pt, double d) {
		pt.x *= d;
		pt.y *= d;
		return pt;
	}
	
	public static Point divide(Point pt, double d) {
		return multiply(pt, 1.0 / d);
	}
	
	public static double dist(Point a, Point b) {
		return Math.hypot(a.x - b.x, a.y - b.y);
	}
	
	public static double distSqr(Point a, Point b) {
		double x = a.x - b.x;
		double y = a.y - b.y;
		return x * x + y * y;
	}
	
	public static double distSqr(Point a, double bx, double by) {
		double x = a.x - bx;
		double y = a.y - by;
		return x * x + y * y;
	}
	
	public static double angle(Point pt) {
		return Math.atan2(pt.y, pt.x);
	}
	
	public static double angle(Point a, Point b) {
		return Math.atan2(b.x - a.x, b.y - a.y);
	}
	
	public static Point set(Point pt, double x, double y) {
		pt.x = x;
		pt.y = y;
		return pt;
	}
	
	public static Point set(Point pt, Point val) {
		return set(pt, val.x, val.y);
	}
	
	public static Point set(Point pt, Point start, Point end) {
		return set(pt, end.x - start.x, end.y - start.y);
	}
	
}
