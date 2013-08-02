package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import edu.berkeley.cellscope.cscore.celltracker.Colors;

public class CellDetection {
	public static final int CHANNEL_RED = 2;
	public static final int CHANNEL_GREEN = 1;
	public static final int CHANNEL_BLUE = 0;
	private static final Point FILL_SEED = new Point(0, 0);
	
	public static ContourData filterImage(Mat img, int channel, int threshold) {
		System.out.println(channel);
		List<Mat> channels = new ArrayList<Mat>(3);;
		Core.split(img.clone(), channels);
		Mat gray = channels.get(channel);
		Mat bw = Mat.zeros(gray.size(), CvType.CV_8UC1);
		
		Imgproc.threshold(gray, bw, threshold, 255, Imgproc.THRESH_BINARY);
		Core.rectangle(bw, new Point(0, 0), new Point(bw.cols(), bw.rows()), Colors.BLACK, 2);
		Mat bw2 = new Mat(bw.size(), bw.type());
		bw.copyTo(bw2);
		//Extract contours
		List<MatOfPoint> allContours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(bw, allContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		bw.release();
		gray.release();
		img.release();
		return new ContourData(bw2, hierarchy, allContours);
	}
	
	public static ContourData filterImage(Bitmap image, int channel, int threshold) {
		Mat img = new Mat();
		Utils.bitmapToMat(image, img);
		ContourData data = filterImage(img, channel, threshold);
		img.release();
		return data;
	}
	
	public static ContourData removeNoise(ContourData data, int threshold) {
		//Eliminate small debris
		int elements = data.whiteContours.size();
		for (int i = 0; i < elements; i ++) {
			MatOfPoint contour = data.whiteContours.get(i);
			if (Imgproc.contourArea(contour) <= threshold) {
				Imgproc.drawContours(data.bw, data.whiteContours, i, Colors.BLACK, Core.FILLED); //erase from image
				data.whiteContours.remove(i);
				elements --;
				i --;
			}
		}
		return data;
	}
	
	public static ContourData removeDebris(ContourData contours, double boundary) {
		double min = getMedian(contours) * boundary;
		//Eliminate any regions smaller than specified range
		Mat erasedDebris = Mat.zeros(contours.bw.size(), contours.bw.type());
		int elements = contours.whiteContours.size();
		int[] data = new int[4];
		for (int i = 0; i < elements; i ++) {
			double area = Imgproc.contourArea(contours.whiteContours.get(i));
			if (area < min) {
				//Imgproc.drawContours(bw, whiteContours, i, BLACK, Core.FILLED);
				addRegion(erasedDebris, contours.allContours, contours.hierarchy, data, contours.allContours.indexOf(contours.whiteContours.get(i)));
				contours.whiteContours.remove(i);
				i --;
				elements --;
			}
		}
		Core.subtract(contours.bw, erasedDebris, contours.bw);
		erasedDebris.release();
		return contours;
	}
	
	public static ContourData removeBackground(ContourData contours, double boundary) {
		double max = getMedian(contours) * boundary;
		//Eliminate any regions greater than the specified range.
		//Erased regions are saved separately for further processing
		Mat background = Mat.zeros(contours.bw.size(), contours.bw.type());
		List<MatOfPoint> backgroundContours = new ArrayList<MatOfPoint>();
		int elements = contours.whiteContours.size();
		int[] data = new int[4];
		for (int i = 0; i < elements; i ++) {
			double area = Imgproc.contourArea(contours.whiteContours.get(i));
			if (area > max) {
				addRegion(background, contours.allContours, contours.hierarchy, data,contours. allContours.indexOf(contours.whiteContours.get(i)));
				backgroundContours.add(contours.whiteContours.remove(i));
				i --;
				elements --;
			}
		}
		background.copyTo(contours.background);
		Core.subtract(contours.bw, background, contours.bw);
		background.release();
		return contours;
	}
	
	public static ContourData isolateCells(ContourData contours, int magnitude) {
		/*if (magnitude == 0)
			return contours;
		Mat holes = new Mat(contours.bw.size(), contours.bw.type());
		contours.background.copyTo(holes);
		imfill(holes);
		Core.subtract(holes, contours.background, holes);
		
		Mat isolated = Mat.zeros(holes.size(), holes.type());
		Mat closed = new Mat(holes.size(), holes.type());
		Mat closedFill = new Mat(holes.size(), holes.type());
		Mat stepResult = new Mat(holes.size(), holes.type());
		int increment = magnitude / 8;
		if (increment < 1) increment = 1;
		for (int i = 1; i < magnitude + increment; i += increment)  {
			if (i > magnitude)
				i = magnitude;
			System.out.println(i);
			Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(i, i));
			Imgproc.morphologyEx(holes, closed, Imgproc.MORPH_CLOSE, kernel);
			closed.copyTo(closedFill);
			imfill(closedFill);
			Core.subtract(closedFill, closed, stepResult);
			Core.add(isolated, stepResult, isolated);
		}
		List<MatOfPoint> newContours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Mat isolatedCopy = isolated.clone();
		
		
		Core.add(isolated, contours.bw, contours.bw);
		Imgproc.findContours(contours.bw.clone(), newContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		contours.allContours.clear();
		contours.whiteContours.clear();
		int[] data = new int[4];
		for (int i = 0; i < newContours.size(); i ++) {
			hierarchy.get(0, i, data);
			if (data[3] == -1) {
				contours.whiteContours.add(newContours.get(i));
			}
			contours.allContours.add(newContours.get(i));
		}
		holes.copyTo(contours.background);
		holes.release();
		isolated.release();
		closed.release();
		closedFill.release();
		stepResult.release();
		hierarchy.release();
		newContours.clear();
		isolatedCopy.release();
		return contours;*/
		return contours;
	}
	
	public static ContourData removeOblong(ContourData contours, double threshold) {
		Mat erasedOblong = Mat.zeros(contours.bw.size(), contours.bw.type());
		int elements = contours.whiteContours.size();
		int[] data = new int[4];
		for (int i = 0; i < elements; i ++) {
			MatOfPoint contour = contours.whiteContours.get(i);
			MatOfPoint2f points = new MatOfPoint2f(contour.toArray());
			RotatedRect rect = Imgproc.minAreaRect(points);
			Size size = rect.size;
			double width = size.width;
			double height = size.height;
			double ratio = (width > height) ? (width / height) : (height / width); //major:minor
			if (ratio >= threshold) {
				//Imgproc.drawContours(bw, whiteContours, i, BLACK, Core.FILLED); //paint the oblong debris black to remove it
				int index = contours.allContours.indexOf(contour);
				contours.hierarchy.get(0, index, data);
				addRegion(erasedOblong, contours.allContours, contours.hierarchy, data, index);
				contours.whiteContours.remove(i);
				i --;
				elements --;
			}
		}
		Core.subtract(contours.bw, erasedOblong, contours.bw);
		erasedOblong.release();
		return contours;
	}
	
	public static double getMedian(ContourData data) {
		List<Double> areaList = new ArrayList<Double>();
		for (MatOfPoint p: data.whiteContours)
			areaList.add(Imgproc.contourArea(p));
		Double[] areas = areaList.toArray(new Double[0]);
		Arrays.sort(areas);
		double median = (areas.length > 0) ? areas[Math.round(areas.length / 2)]: 0;
		return median;
	}
	
	public static Bitmap drawMat(Bitmap bmp, Mat mat) {
		Utils.matToBitmap(mat, bmp);
		return bmp;
	}
	
	static class MultiChannelContourData {
		private Mat accumulated;
		public MultiChannelContourData(Mat bw) {
			accumulated = Mat.zeros(bw.size(), bw.type());
		}
		
		public List<Rect> getRects() {
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Mat hierarchy = new Mat();
			Mat copy = new Mat(accumulated.size(), accumulated.type());
			accumulated.copyTo(copy);
			Imgproc.findContours(copy, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
			List<Rect> result = new ArrayList<Rect>();
			for (MatOfPoint p: contours)
				result.add(Imgproc.boundingRect(p));
			return result;
		}
		
		public void getRects(List<Rect> result) {
			result.clear();
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Mat hierarchy = new Mat();
			Mat copy = new Mat(accumulated.size(), accumulated.type());
			accumulated.copyTo(copy);
			Imgproc.findContours(copy, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
			for (MatOfPoint p: contours)
				result.add(Imgproc.boundingRect(p));
		}
		
		public void add(ContourData data) {
			Core.add(accumulated, data.bw, accumulated);
		}
	}
	
	static class ContourData {
		public Mat hierarchy, bw, background;
		public List<MatOfPoint> allContours, whiteContours;
		public ContourData(Mat b, Mat h, List<MatOfPoint> list) {
			bw = b;
			background = Mat.zeros(bw.size(), bw.type());
			hierarchy = h;
			allContours = list;
			int elements = allContours.size();
			whiteContours = new ArrayList<MatOfPoint>();
			int[] data = new int[4];
			for (int i = 0; i < elements; i ++) {
				hierarchy.get(0, i, data);
				if (data[3] == -1) {
					whiteContours.add(allContours.get(i));
				}
			}
		}
		
		public void release() {
			hierarchy.release();
			bw.release();
			background.release();
			allContours.clear();
			whiteContours.clear();
		}
		
		private ContourData() {
			allContours = new ArrayList<MatOfPoint>();
			whiteContours = new ArrayList<MatOfPoint>();
		}
		
		public ContourData copy() {
			ContourData copy = new ContourData();
			copy.allContours.addAll(allContours);
			copy.whiteContours.addAll(whiteContours);
			copy.hierarchy = hierarchy.clone();
			copy.bw = bw.clone();
			copy.background = background.clone();
			return copy;
		}
		
		public List<Rect> getRects() {
			List<Rect> result = new ArrayList<Rect>();
			for (MatOfPoint p: whiteContours)
				result.add(Imgproc.boundingRect(p));
			return result;
		}
		
		public void getRects(List<Rect> result) {
			for (MatOfPoint p: whiteContours)
				result.add(Imgproc.boundingRect(p));
		}
		
		public void addContours(Mat image) {
			Core.add(image, bw, image);
		}
		
		public MultiChannelContourData generateMultiChannelData() {
			return new MultiChannelContourData(bw);
		}
	}
	

	
	public static void isolateRegion(Mat dst, List<MatOfPoint> contours, Mat hierarchy, int[] data, int index) {
		/* Drawing white contours will cause them to be completely filled, holes included.
		 * Holes are recorded as child contours.
		 * Drawing the child contours as black on top of the white contour will reveal the holes.
		 */
		Imgproc.drawContours(dst, contours, index, Colors.WHITE, Core.FILLED);
		hierarchy.get(0, index, data);
		int black = data[2]; //First child
		//Continue looping while there are more holes
		while (black != -1) {
			//Draw the hole
			Imgproc.drawContours(dst, contours, black, Colors.BLACK, Core.FILLED);
			Imgproc.drawContours(dst, contours, black, Colors.WHITE, 1); //Fill enlarges the hole by one pixel.
			hierarchy.get(0, black, data);
			black = data[0]; //next sibling
		}
	}
	
	public static void addRegion(Mat dst, List<MatOfPoint> contours, Mat hierarchy, int[] data, int index) {
		Mat isolated = Mat.zeros(dst.size(), dst.type());
		isolateRegion(isolated, contours, hierarchy, data, index);
		Core.add(dst, isolated, dst);
		isolated.release();
	}

	public static void imfill(Mat src) {
		Mat fill = new Mat(src.size(), src.type());
		src.copyTo(fill);
		Mat mask = Mat.zeros(src.rows() + 2, src.cols() + 2, src.type());
		src.copyTo(mask.submat(1, 1, src.rows() + 1, src.cols() + 1));
		Imgproc.floodFill(fill, mask, FILL_SEED, Colors.WHITE);
		Core.subtract(fill, src, src);
		Mat white = new Mat(src.size(), src.type(), Colors.WHITE);
		Core.subtract(white, src, src);
		fill.release();
		white.release();
		mask.release();
	}
}