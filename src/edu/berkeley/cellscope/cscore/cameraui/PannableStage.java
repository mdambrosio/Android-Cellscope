package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.BluetoothActivity;

public interface PannableStage extends TouchControllable {
    static final int xRightMotor = BluetoothActivity.xRightMotor;
    static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    static final int yBackMotor = BluetoothActivity.yBackMotor;
    static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    static final int zUpMotor = BluetoothActivity.zUpMotor;
    static final int zDownMotor = BluetoothActivity.zDownMotor;
    static final int stopMotor = 0;
	static final double PAN_THRESHOLD = 50;
	static final double Z_CONTROL_ZONE = 0.3;
	
	
	public void panStage(int newStage);
	public boolean panAvailable();
}
