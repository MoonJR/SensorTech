package net.jongrakko.sensortech.libs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by MoonJongRak on 2016. 3. 19..
 */
public class BluetoothManager {
    private static BluetoothManager instance;

    private OnBluetoothListener listener;
    private OnBluetoothStateChangeListener stateListener;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothBroadcastReceiver mBluetoothBroadcastReceiver;

    private Set<BluetoothDevice> devices;

    private boolean isClosed;

    public static BluetoothManager getInstance(Context mContext) {
        if (instance == null || instance.isClosed) {
            instance = new BluetoothManager(mContext);
        }
        return instance;
    }

    private BluetoothManager(Context mContext) {
        this.devices = new HashSet<>();
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = mContext;
        this.mBluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(mBluetoothAdapter);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        this.mContext.registerReceiver(this.mBluetoothBroadcastReceiver, mIntentFilter);
    }

    public void setOnBluetoothListener(OnBluetoothListener listener) {
        this.listener = listener;
    }

    public void setOnBluetoothStateChangeListener(OnBluetoothStateChangeListener listener) {
        this.stateListener = listener;
    }

    public boolean isUseBluetooth() {
        return mBluetoothAdapter != null;
    }

    public boolean isBluetoothEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    public void close() {
        mContext.unregisterReceiver(mBluetoothBroadcastReceiver);
        isClosed = true;
        instance = null;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void bluetoothEnable() {
        mBluetoothAdapter.enable();
    }

    public void startDeviceSearch() {
        devices.clear();
        mBluetoothAdapter.startLeScan(callback);
    }

    BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (devices.add(device) && listener != null) {
                listener.onFoundDevice(devices, rssi);
            }
        }
    };

    public void stopDeviceSearch() {
        mBluetoothAdapter.stopLeScan(callback);
    }


    public ArrayList<BluetoothDevice> getPairedDevice() {
        return new ArrayList<>(mBluetoothAdapter.getBondedDevices());
    }

    public static SensorTechBluetoothConnector getBluetoothConnectThread(Context mContext, BluetoothDevice mBluetoothDevice) {
        return new SensorTechBluetoothConnector(mContext, mBluetoothDevice);
    }

    public interface OnBluetoothListener {
        void onFoundDevice(Set<BluetoothDevice> mBluetoothDevice, int rssi);
    }

    public interface OnBluetoothStateChangeListener {
        void onChangeState(boolean on);
    }

    class BluetoothBroadcastReceiver extends BroadcastReceiver {

        private BluetoothAdapter mBluetoothAdapter;
        private Set<BluetoothDevice> mSetBlueToothDevice;

        public BluetoothBroadcastReceiver(BluetoothAdapter mBluetoothAdapter) {
            this.mBluetoothAdapter = mBluetoothAdapter;
            this.mSetBlueToothDevice = new HashSet<>();
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            Log.d("yeon", action);

            if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
            } else if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
            } else if (action.equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)) {
            } else if (action.equals(BluetoothAdapter.ACTION_REQUEST_ENABLE)) {
            } else if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    case BluetoothAdapter.STATE_OFF:
                        if (stateListener != null) {
                            stateListener.onChangeState(false);
                        }
                        Log.d("yeon", "블루투스 꺼짐.");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (stateListener != null) {
                            stateListener.onChangeState(true);
                        }
                        Log.d("yeon", "블루투스 켜짐");
                        break;
                }


            }

        }
    }


}
