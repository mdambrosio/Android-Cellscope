package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;
/*
 * Autofocus algorithm:
 * 1. Move up 8 levels.
 * 2. Switch direction.
 * 3. Scan 16 levels. Keep track of the highest and lowest scores.
 * 4. If scores increase to more than twice the lowest score, then decrease to less than twice the lowest score, stop.
 * 		4a. If 16 steps are reached without this happening, stop.
 * 5. Compare the highest score to the best score. If better, replace.
 * 6. Halve level size.
 * 7. Repeat 2 thru 6 until stopped, or until level size drops below a minimum.
 *  
 * Scores are calculated via edge detection. The scores peak in a range fo about 128 steps,
 * and is noisy on either side of the peak. Start the initial level size at 64 steps.
 */
public class Autofocus {
	private TouchSwipeControl stage;
	private Autofocusable callback;
	private boolean busy;
	private int stepSize, direction;
	private int currentPosition, targetPosition;
	private int stepsTaken;
	private int waitFrames, state;
	private int bestScore, bestNetScore, lowestNetScore;
	private boolean passedPeak;
	
	private final Object lockStatus;
	
	private static final int INITIAL_STEP = 64; //Step size cannot be greater than 127, due to size limitations on byte
	private static final int Z_RANGE = 16; //Check 4 levels on either side of the current position
	private static final int MINIMUM_STEP = 2;
	private static final double STRICTNESS = 0.85; //0~1. How close to perfect do we stop at? Autofocus will be more likely to fail
													//and overshoot if this is too high, but will stop out of focus when too low
	private static final int PAUSE = 1; //Number of frames to wait after motion stops for the camera to catch up.
	private static final double EDGE_THRESHOLD_RATIO = 1;
	private static final double EDGE_LOWER_THRESHOLD = 64;
	private static final int STATE_READY = 0; //At rest, preparing for direct movement
	private static final int STATE_MOVING = 1; //Currently in direct movement
	private static final int STATE_STEPPING = 2; //In movement, stopping at intervals for analysis
	
	private static final int NO_CALCULATION = -1;
	
	private static final int SCORE_PEAK_SIZE = 2;
	
	private static final Size BLUR = new Size(2, 2);
	
	public static final String SUCCESS_MESSAGE = "Autofocus successful";
	public static final String FAILURE_MESSAGE = "Autofocus failed";
	
	public Autofocus(TouchSwipeControl s) {
		stage = s;
		busy = false;
		lockStatus = new Object();
	}
	
	public void setCallback(Autofocusable a) {
		callback = a;
	}
	
	public void focus() {
		if (busy || !stage.bluetoothConnected())
			return;
		System.out.println("begin focus");
		synchronized(lockStatus) {
			busy = true;
			stepsTaken = 0;
			bestNetScore = bestScore = lowestNetScore = 0;
			direction = TouchSwipeControl.zUpMotor;
			passedPeak = false;
			stepSize = INITIAL_STEP;
			state = STATE_READY;
			run();
		}
	}
	
	//This is the main method, called whenever the stage finishes moving.
	public synchronized void run() {
		if (!busy)
			return;
		if (!stage.bluetoothConnected()) {
			focusFailed();
			return;
		}
		if (stepSize < MINIMUM_STEP) {
			focusComplete();
		}
		if (state == STATE_READY) {
			System.out.println("Moving to starting position...");
			moveInZ(stepSize * Z_RANGE / 2);
			state = STATE_MOVING;
		}
		if (state == STATE_MOVING) {
			boolean stop = zMoveStep();
			if (stop) {
				switchDirection();
				state = STATE_STEPPING;
			}
		}
		if (state == STATE_STEPPING) {
			synchronized(lockStatus) {
				waitFrames = PAUSE; 
			}
		}
	}
	
	public synchronized void queueFrame(Mat mat) {
		synchronized(lockStatus) {
			if (!busy || waitFrames == NO_CALCULATION || state != STATE_STEPPING)
				return;
			if (waitFrames > 0) {
				waitFrames --;
				return;
			}
			waitFrames = NO_CALCULATION;
		}
		if (stepsTaken > Z_RANGE)
			focusFailed();
		stepsTaken ++;
		Mat data = new Mat(mat.size(), mat.type());
		mat.copyTo(data);
		if (!calculateFocus(data))
			stage.swipe(direction, stepSize);
	}
	
	private void switchDirection() {
		if (direction == TouchSwipeControl.zUpMotor)
			direction = TouchSwipeControl.zDownMotor;
		else
			direction = TouchSwipeControl.zUpMotor;
	}
	
	private void moveInZ(int position) {
		currentPosition = 0;
		targetPosition = Math.abs(position);
	}
	
	//return true when target loctaion is reached
	private boolean zMoveStep() {
		System.out.println("moving " + currentPosition + " " + targetPosition);
		if (currentPosition >= targetPosition)
			return true;
		currentPosition += stepSize;
		stage.swipe(direction, stepSize);
		return false;
	}
	
	public boolean isFocusing() {
		return busy;
	}
	
	public void focusFailed() {
		busy = false;
		if (callback != null)
			callback.focusCallback(false);
		System.out.println("focus failed");
	}
	
	public void focusComplete() {
		busy = false;
		if (callback != null)
			callback.focusCallback(true);
		System.out.println("focus completed");
	}
	
	private void calculationComplete() {
		switchDirection();
		stepsTaken = 0;
		stepSize /= 2;
		System.out.println("new step size " + stepSize);
		bestScore = lowestNetScore;
		passedPeak = false;
		stage.swipe(direction, stepSize);
	}
	
	//return true when the peak is passed
	public boolean calculateFocus(Mat img) {
		Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
		Imgproc.blur(img, img, BLUR);
		Imgproc.Canny(img, img, EDGE_LOWER_THRESHOLD, EDGE_LOWER_THRESHOLD * EDGE_THRESHOLD_RATIO);
		int score = Core.countNonZero(img);
		
		System.out.println("[score=" + score + ", high=" + bestScore + ", best=" + bestNetScore + ", low=" + lowestNetScore + ", direction=" + direction);
		
		if (score == 0)
			return false;
		
		if (lowestNetScore > score || lowestNetScore == 0)
			lowestNetScore = score;
		if (score > bestScore || bestScore == 0) {
			bestScore = score;
			if (bestNetScore < bestScore)
				bestNetScore = bestScore;
		}
		if (passedPeak && score < bestScore) {
			calculationComplete();
			return true;
		}
		else if (bestScore > lowestNetScore * SCORE_PEAK_SIZE && score >= bestNetScore * STRICTNESS) {
			if (stepSize <= MINIMUM_STEP && score <= bestScore) {
				calculationComplete();
				return true;
			}
			passedPeak = true;
		}
		return false;
	}
	
	
	public static interface Autofocusable {
		public void queueAutofocusFrame(Mat m);
		public void notifyAutofocus(int message);
		public void focusCallback(boolean success);
	}
}
