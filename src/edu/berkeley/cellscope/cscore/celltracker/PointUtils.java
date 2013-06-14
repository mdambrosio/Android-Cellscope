package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;

/* Various useful mathematical operations for points/vectors
 * that are not included in OpenCV's Point class.
 */
public class PointUtils {
	
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
