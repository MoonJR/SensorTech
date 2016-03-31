package net.jongrakko.sensortech.libs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;


public class ACSUtility {

    private Context context;
    private ArrayList<blePort> ports = null;
    private blePort currentPort = null;
    private int _lengthOfPackage = 10;
    private float _scanTime;
    private Boolean bScanning;
    private byte[] receivedBuffer;
    private final String tag = "ACSUtility";
    private final int ACSUTILITY_SCAN_TIMEOUT_MSG = 0x01;
    private BluetoothAdapter mBtAdapter;
    private ACSUtilityService mService;
    private IACSUtilityCallback userCallback;


    public ACSUtility() {
        Log.d(tag, "ACS Utility Constructor");
    }

    public ACSUtility(Context context, IACSUtilityCallback cb) {
        // TODO Auto-generated constructor stub
        //���캯������ʼ�����б���
        this.context = context;
        userCallback = cb;
        _lengthOfPackage = 10;
        bScanning = false;

        Log.d(tag, "acsUtility 1");

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            Log.d(tag, "error,mBtAdapter == null");
            return;
        }

        //context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent intent = new Intent();
        intent.setClass(context, ACSUtilityService.class);
        context.startService(intent);
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    public void setUserCallback(IACSUtilityCallback callback) {
        this.userCallback = callback;

    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(tag, "ACSUtilityService is connected!");
            mService = ((ACSUtilityService.ACSBinder) service).getService();
            mService.initialize();
            mService.addEventHandler(eventHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.d(tag, "ACSUtilityService is disConnected!");
            mService = null;
        }

    };
    boolean mIsPortOpen = false;
    private Handler eventHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Log.e(tag, "EventHandler got a message.flag is " + msg.what);
            if (userCallback == null) {
                Log.e(tag, "UserCallback is null! All event will not be handled!");
                return;
            }
            switch (msg.what) {
                case ACSUtilityService.EVENT_GATT_CONNECTED:

                    break;
                case ACSUtilityService.EVENT_GATT_DISCONNECTED:
                    userCallback.didClosePort(currentPort);
                    mIsPortOpen = false;
                    break;
                case ACSUtilityService.EVENT_GATT_SERVICES_DISCOVERED:

                    break;

                case ACSUtilityService.EVENT_OPEN_PORT_SUCCEED:

                    userCallback.didOpenPort(currentPort, true);
                    mIsPortOpen = true;
                    break;
                case ACSUtilityService.EVENT_OPEN_PORT_FAILED:

                    userCallback.didOpenPort(currentPort, false);
                    mIsPortOpen = true;
                    break;
                case ACSUtilityService.EVENT_DATA_AVAILABLE:
                    Bundle data = msg.getData();
                    byte[] receivedData = data.getByteArray(ACSUtilityService.EXTRA_DATA);
                    break;

                case ACSUtilityService.EVENT_HEART_BEAT_DEBUG:

                    break;

                case ACSUtilityService.EVENT_DATA_SEND_SUCEED:
                    userCallback.didPackageSended(true);
                    break;
                case ACSUtilityService.EVENT_DATA_SEND_FAILED:
                    userCallback.didPackageSended(false);
                    break;
                default:
                    break;
            }
        }

    };

    public void enumAllPorts(float time) {
        ports = null;
        _scanTime = time;
        if (bScanning) {
            Log.e(tag, "enum in progress,could not execute again");
            return;
        }
        Log.d(tag, "start scan now");
        mBtAdapter.stopLeScan(mLeScanCallback);
        mBtAdapter.startLeScan(mLeScanCallback);
        bScanning = true;
        Thread timerThread = new Thread(new myThread());
        timerThread.start();

    }

    public boolean isPortOpen(blePort port) {

        return (mIsPortOpen && port._device.equals(currentPort._device));

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            // TODO Auto-generated method stub
            Log.d(tag, "onScanResult() - deviceName = " + device.getName()
                    + ", rssi=" + rssi + ",lengthOfScanRecord is : "
                    + scanRecord.length + ",address : " + device.getAddress());

            if (!checkAddressExist(device)) {
                if (ports == null) {
                    ports = new ArrayList<blePort>();
                }

                Log.d(tag, "==== new Port add here ====");
                blePort newPort = new blePort(device);
                ports.add(newPort);


            }
        }
    };

    public void stopEnum() {
        bScanning = false;
        mBtAdapter.stopLeScan(mLeScanCallback);
    }

    public void openPort(final blePort port) {

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mService != null && port != null) {
                    currentPort = port;
                    mService.connect(port._device.getAddress());
                } else {
                    Log.e(tag, "ACSUtilityService or port is null!");
                }
            }
        }, 1000);


    }

    public void closePort() {
        mService.disconnect();
    }

    public void configurePort(blePort port, int lenghOfPackage) {
        _lengthOfPackage = lenghOfPackage;
    }

    public boolean writePort(byte[] value) {
        if (value != null && mIsPortOpen) {
            return mService.writePort(value);
        }
        Log.e(tag, "Write port failed...value is null...");

        return false;
    }

    public void closeACSUtility() {
        //BluetoothGattAdapter.closeProfileProxy(BluetoothGattAdapter.GATT, mBtGatt);
        mService.close();
        //closePort();
        mService.removeEventHandler();
        context.unbindService(conn);
        Intent intent = new Intent();
        intent.setClass(context, ACSUtilityService.class);
        context.stopService(intent);
    }


    private void openPortFailAction() {
        if (userCallback != null) {
            userCallback.didOpenPort(currentPort, false);
        }
    }

    private void openPortSuccessAction() {
        if (userCallback != null) {
            userCallback.didOpenPort(currentPort, true);
        }
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        stringBuilder.append('\n');
        return stringBuilder.toString();
    }

    private Boolean checkAddressExist(BluetoothDevice device) {
        if (ports == null) {
            return false;
        }
        for (blePort port : ports) {
            if (port._device.getAddress().equals(device.getAddress())) {
                return true;
            }
        }

        return false;
    }

    private void checkPackageToSend(byte[] newData) {
        if (receivedBuffer != null) {
            Log.d(tag, "checkPachageToSend buffer length is " + receivedBuffer.length);
            int newLength = receivedBuffer.length + newData.length;
            byte[] tempBuffer = new byte[newLength];
            byteCopy(receivedBuffer, tempBuffer, 0, 0, receivedBuffer.length);
            byteCopy(newData, tempBuffer, 0, receivedBuffer.length, newData.length);
            receivedBuffer = null;
            receivedBuffer = tempBuffer;
        } else {
            Log.d(tag, "checkPachageToSend buffer is null !");
            receivedBuffer = new byte[newData.length];
            byteCopy(newData, receivedBuffer, 0, 0, newData.length);
        }

        Log.d(tag, "buffer lenght now is " + receivedBuffer.length);
        if (receivedBuffer.length >= _lengthOfPackage) {
            byte[] packageToSend = new byte[_lengthOfPackage];
            byte[] tempBuffer = new byte[receivedBuffer.length - _lengthOfPackage];
            byteCopy(receivedBuffer, packageToSend, 0, 0, _lengthOfPackage);
            byteCopy(receivedBuffer, tempBuffer, _lengthOfPackage, 0, tempBuffer.length);
            receivedBuffer = null;
            receivedBuffer = tempBuffer;
            Log.d(tag, "left length is " + receivedBuffer.length);
        }


    }

    private void byteCopy(byte[] from, byte[] to, int fromIndex, int toIndex, int length) {
        int realLength = (from.length < length) ? from.length : length;
        for (int i = 0; i < realLength; i++) {
            to[i + toIndex] = from[i + fromIndex];
        }
    }


    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACSUTILITY_SCAN_TIMEOUT_MSG:
                    Log.d(tag, "scan time out");
                    bScanning = false;
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private class myThread implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep((long) _scanTime * 1000);
                if (bScanning) {
                    Message msg = new Message();
                    msg.what = ACSUTILITY_SCAN_TIMEOUT_MSG;
                    handler.sendMessage(msg);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public interface IACSUtilityCallback {

        void didOpenPort(blePort port, Boolean bSuccess);

        void didClosePort(blePort port);

        void didPackageSended(boolean succeed);

    }

    //Port��
    public class blePort implements Serializable {
        public BluetoothDevice _device;

        public blePort(BluetoothDevice device) {
            _device = device;
        }
    }

    //debug function

    public void printHexString(byte[] b) {
        for (byte aB : b) {
            String hex = Integer.toHexString(aB & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            Log.d(tag, hex);
        }

    }
}

