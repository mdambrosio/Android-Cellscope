package edu.berkeley.cellscope.cscore.cameraui;

import android.content.Intent;

public interface BluetoothConnectable {
    static final int REQUEST_CONNECT_DEVICE = BluetoothConnector.REQUEST_CONNECT_DEVICE;
    static final int REQUEST_ENABLE_BT = BluetoothConnector.REQUEST_ENABLE_BT;
    
    public void bluetoothUnavailable();
	public void bluetoothConnected();
	public void bluetoothDisconnected();
	public void updateStatusMessage(int id);
	public void onActivityResult(int requestCode, int resultCode, Intent data);
}
