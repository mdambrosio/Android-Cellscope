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
 * 3. Sweep 16 levels. Keep track of the highest and lowest scores.
 * 4. If scores increase to more than twice the lowest score, then starts to decrease, stop.
 * 		4a. If 16 steps are reached without this happening, quit and report failure.
 * 5. Compare the highest score to the best score. If better, replace.
 * 6. Halve level size.
 * 7. Repeat 2 thru 6 until stopped, or until level size drops below a minimum. If latter, quit and report success.
 *  
 * Scores are calculated via edge detection. The scores peak in a range fo about 128 steps,
 * and is noisy on either side of the peak. Start the initial level size at 64 steps.
 */
public class Autofocus implements RealtimeImageProcessor {
	private TouchSwipeControl stage;
	private AutofocusCallback callback;
	private boolean busy;
	private int stepSize, direction;
	private int currentPosition, targetPosition;
	private int stepsTaken;
	private int waitFrames, state;
	private int bestScore, bestNetScore, lowestNetScore, unfocusedScore;
	private boolean passedPeak;
	
	private final Object lockStatus;
	
	private static final int QUICK_INITIAL_STEP = 16;
	private static final int INITIAL_STEP = 32; //Step size cannot be greater than 127, due to size limitations on byte
	private static final int Z_RANGE = 16; //Check 4 levels on either side of the current position
	private static final int MINIMUM_STEP = 8;
	private static final double STRICTNESS = 0.9; //0~1. How close to perfect do we stop at? Autofocus will be more likely to fail
													//and overshoot if this is too high, but will stop out of focus when too low
	private static final int PAUSE = 3; //Number of frames to wait after motion stops for the camera to catch up.
	private static final double EDGE_THRESHOLD_RATIO = 1.5;
	private static final double EDGE_LOWER_THRESHOLD = 64;
	private static final int STATE_READY = 0; //At rest, preparing for direct movement
	private static final int STATE_MOVING = 1; //Currently in direct movement
	private static final int STATE_STEPPING = 2; //In movement, stopping at intervals for analysis
	private static final int STARTING_DIRECTION = TouchSwipeControl.zPositive;
	private static final int OPPOSITE_DIRECTION = TouchSwipeControl.zNegative;
	
	private static final int NO_CALCULATION = -1;
	
	private static final int SCORE_PEAK_SIZE = 2;
	
	private static final Size BLUR = new Size(3, 3);
	
	public static final String SUCCESS_MESSAGE = "Autofocus successful";
	public static final String FAILURE_MESSAGE = "Autofocus failed";
	
	public Autofocus(TouchSwipeControl s) {
		stage = s;
		busy = false;
		lockStatus = new Object();
	}
	
	public void setCallback(AutofocusCallback a) {
		callback = a;
	}
	
	public void start() {
		if (busy || !stage.bluetoothConnected())
			return;
		System.out.println("begin focus");
		synchronized(lockStatus) {
			busy = true;
			stepsTaken = 0;
			bestNetScore = bestScore = lowestNetScore = 0;
			direction = STARTING_DIRECTION;
			passedPeak = false;
			stepSize = INITIAL_STEP;
			state = STATE_READY;
			continueRunning();
		}
	}
	
	public void quickFocus() {
		if (busy || !stage.bluetoothConnected())
			return;
		System.out.println("begin focus");
		synchronized(lockStatus) {
			busy = true;
			stepsTaken = 0;
			bestNetScore = bestScore = lowestNetScore = 0;
			direction = STARTING_DIRECTION;
			passedPeak = false;
			stepSize = QUICK_INITIAL_STEP;
			state = STATE_READY;
			continueRunning();
		}
	}
	
	//This is the main method, called whenever the stage finishes moving.
	public synchronized void continueRunning() {
		if (!busy)
			return;
		if (!stage.bluetoothConnected()) {
			stop();
			return;
		}
		if (stepSize < MINIMUM_STEP) {
			complete();
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
	
	public synchronized void processFrame(Mat mat) {
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
			stop();
		stepsTaken ++;
		Mat data = new Mat(mat.size(), mat.type());
		mat.copyTo(data);
		if (!calculateFocus(data))
			stage.swipe(direction, stepSize);
	}
	
	private void switchDirection() {
		if (direction == STARTING_DIRECTION)
			direction = OPPOSITE_DIRECTION;
		else
			direction = STARTING_DIRECTION;
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
	
	public boolean isRunning() {
		return busy;
	}
	
	public void stop() {
		if (!busy)
			return;
		busy = false;
		if (callback != null)
			callback.focusCallback(false);
		System.out.println("focus failed");
	}
	
	public void complete() {
		if (!busy)
			return;
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
		//bestNetScore = bestScore;
		bestScore = lowestNetScore;
		passedPeak = false;
		if (stepSize <= MINIMUM_STEP)
			continueRunning();
		else
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
		
		if (lowestNetScore > score || lowestNetScore == 0) {
			lowestNetScore = score;
			if (lowestNetScore < unfocusedScore || unfocusedScore == 0)
				unfocusedScore = lowestNetScore;
		}
		if (score > bestScore || bestScore == 0) {
			bestScore = score;
			if (bestNetScore < bestScore)
				bestNetScore = bestScore;
		}
		if (passedPeak && score < bestScore) {
			//if (stepSize != INITIAL_STEP || stepsTaken == Z_RANGE) {
				calculationComplete();
				return true;
			//}
		}
		else if (bestScore > lowestNetScore * SCORE_PEAK_SIZE && score >= bestNetScore * STRICTNESS /*&& stepSize != INITIAL_STEP*/) {
			if (stepSize <= MINIMUM_STEP && score <= bestScore) {
				System.out.println("quick complete");
				calculationComplete();
				return true;
			}
			passedPeak = true;
		}
		//else if (stepSize == INITIAL_STEP && stepsTaken == Z_RANGE)
		//	passedPeak = true;
		/*else if (stepSize <= MINIMUM_STEP && score < bestScore) {
			System.out.println("quick complete");
			calculationComplete();
			return true;
		}*/
		return false;
	}
	
	public void displayFrame(Mat mat) {
		return;
	}
	
	public static interface AutofocusCallback {
		public void focusCallback(boolean success);
	}
}
