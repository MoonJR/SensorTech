package net.jongrakko.sensortech.libs;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

public class ACSUtilityService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final static String TAG = "ACSUtilityService";
    private String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static int EVENT_GATT_CONNECTED = 1;
    public final static int EVENT_GATT_DISCONNECTED = 2;
    public final static int EVENT_GATT_SERVICES_DISCOVERED = 3;
    public final static int EVENT_OPEN_PORT_SUCCEED = 4;
    public final static int EVENT_DATA_AVAILABLE = 5;
    public final static int EVENT_DATA_RECEIVED = 6;
    public final static int EVENT_HEART_BEAT_DEBUG = 7;
    public final static int EVENT_DATA_SEND_SUCEED = 8;
    public final static int EVENT_DATA_SEND_FAILED = 9;
    public final static int EVENT_OPEN_PORT_FAILED = 10;

    public final static String EXTRA_DATA =
            "EXTRA_DATA";

    public final static UUID ACS_SERVICE_UUID = UUID.fromString("0000FFB0-0000-1000-8000-00805f9b34fb");
    private final UUID CMD_LINE_UUID = UUID.fromString("0000FFB1-0000-1000-8000-00805f9b34fb");
    private final UUID DATA_LINE_UUID = UUID.fromString("0000FFB2-0000-1000-8000-00805f9b34fb");
    public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Stack<Handler> mEventHandlers = new Stack<Handler>();
    private Handler mCurrentEventHandler;
    private boolean isInitializing = true;

    private int packagesCount = 0;
    private ArrayList<byte[]> packages = new ArrayList<byte[]>();
    private WorkerThread workerThread;
    private int wakeTimes = 0;

    public void addEventHandler(Handler eventHandler) {
        mEventHandlers.push(eventHandler);
        mCurrentEventHandler = eventHandler;
    }

    public void removeEventHandler() {
        mEventHandlers.pop();
        if (!mEventHandlers.empty())
            mCurrentEventHandler = mEventHandlers.peek();
    }

    public class ACSBinder extends Binder {
        public ACSUtilityService getService() {
            return ACSUtilityService.this;
        }
    }

    private IBinder binder = new ACSBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return binder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        LogCat.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        isInitializing = true;
        LogCat.logToFileInit();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Message msg = null;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                msg = mCurrentEventHandler.obtainMessage(EVENT_GATT_CONNECTED);
                LogCat.i(TAG, "Connected to GATT server.");
                LogCat.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                LogCat.i(TAG, "Disconnected from GATT server.");
                close();
                msg = mCurrentEventHandler.obtainMessage(EVENT_GATT_DISCONNECTED);
                mBluetoothDeviceAddress = null;
            }
            msg.sendToTarget();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogCat.i(TAG, "Starting enable DATA Line Notificaiton");
            if (status == BluetoothGatt.GATT_SUCCESS)
                setACSNotification(CMD_LINE_UUID);
            else {
                Message msg = mCurrentEventHandler.obtainMessage(EVENT_OPEN_PORT_FAILED);
                msg.sendToTarget();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            byte[] value = characteristic.getValue();

            Message msg = null;

            LogCat.d(TAG, "onCharacteristicChanged");
            if (characteristic.getUuid().equals(CMD_LINE_UUID)) {
                if (value[0] == 0x05) {
                    LogCat.d(TAG, "heart beat package write back ");
                    msg = mCurrentEventHandler.obtainMessage(EVENT_HEART_BEAT_DEBUG);
                    byte[] handshake = {0x05};
                    characteristic.setValue(handshake);
                    mBluetoothGatt.writeCharacteristic(characteristic);
                }
            } else if (characteristic.getUuid().equals(DATA_LINE_UUID)) {
                LogCat.d(TAG, "data line : length = " + value.length);
                msg = mCurrentEventHandler.obtainMessage(EVENT_DATA_AVAILABLE);
                Bundle bundle = new Bundle();
                bundle.putByteArray(EXTRA_DATA, characteristic.getValue());
                msg.setData(bundle);
            }

            msg.sendToTarget();
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            // TODO Auto-generated method stub
            LogCat.d(TAG, "onCharacteristicWrite");
            if (isInitializing && characteristic.getUuid().equals(CMD_LINE_UUID)) {
                LogCat.i(TAG, "data has written!");
                isInitializing = false;
                Message msg = mCurrentEventHandler.obtainMessage(status == BluetoothGatt.GATT_SUCCESS ? EVENT_OPEN_PORT_SUCCEED : EVENT_OPEN_PORT_FAILED);
                msg.sendToTarget();
            } else if (!isInitializing && characteristic.getUuid().equals(DATA_LINE_UUID)) {
                if (workerThread == null) {
                    LogCat.e(TAG, "workerThread is null");
                    return;
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    workerThread.setOperationResult(1);
                } else {
                    workerThread.setOperationResult(0);
                }
                synchronized (workerThread) {
                    LogCat.d(TAG, "notify workerThread");
                    wakeTimes++;
                    workerThread.notify();
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            super.onDescriptorWrite(gatt, descriptor, status);
            LogCat.i(TAG, "onDescriptorWrite");
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Message msg = mCurrentEventHandler.obtainMessage(EVENT_OPEN_PORT_FAILED);
                msg.sendToTarget();
                return;
            }
            LogCat.i(TAG, descriptor.getCharacteristic().getUuid() + " Notification Enabled");
            if (descriptor.getCharacteristic().getUuid().equals(DATA_LINE_UUID)) {

                isInitializing = false;
                Message msg = mCurrentEventHandler.obtainMessage(EVENT_OPEN_PORT_SUCCEED);
                msg.sendToTarget();
            } else if (descriptor.getCharacteristic().getUuid().equals(CMD_LINE_UUID)) {
                LogCat.i(TAG, "Starting enable DATA Line Notificaiton");
                mCurrentEventHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        setACSNotification(DATA_LINE_UUID);
                    }

                }, 2000);

            }
        }
    };


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        LogCat.i(TAG, "Initializing " + TAG);
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                LogCat.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            LogCat.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            LogCat.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            LogCat.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            LogCat.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        LogCat.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            LogCat.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    private boolean setACSNotification(UUID charaUUID) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            LogCat.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattCharacteristic chara = getACSCharacteristic(charaUUID);
        if (chara == null) {
            LogCat.e(TAG, "Characteristic is null!");
            LogCat.e(TAG, "Enableing Notification failed!");
            return false;
        }
        return setCharacteristicNotification(chara, true);

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    private boolean setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            LogCat.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCC);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        return mBluetoothGatt.writeDescriptor(descriptor);

    }


    private BluetoothGattCharacteristic getACSCharacteristic(UUID charaUUID) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            LogCat.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(ACS_SERVICE_UUID);
        if (service == null) {
            LogCat.e(TAG, "Service is not found!");
            return null;
        }
        BluetoothGattCharacteristic chara = service.getCharacteristic(charaUUID);
        return chara;
    }

    public boolean writePort(byte[] value) {
        int rest = value.length;
        int count = 0;
        packages.clear();
        while (rest > 0) {
            byte[] blePackage = null;
            rest -= 20;

            if (rest >= 0) {
                blePackage = new byte[20];
                System.arraycopy(value, count * 20, blePackage, 0, 20);
            } else {
                rest += 20;
                blePackage = new byte[rest];
                System.arraycopy(value, count * 20, blePackage, 0, rest);
                rest -= 20;
            }
            packages.add(blePackage);
            count++;
        }
        // �ִη���
        packagesCount = (value.length / 20) + (value.length % 20 > 0 ? 1 : 0);
        workerThread = new WorkerThread();
        workerThread.start();
        return true;
    }

    private boolean writePackage(byte[] value) {
        LogCat.d(TAG, "writePort 1");
        boolean result = false;
        if (mBluetoothGatt == null) {
            return result;
        }
        LogCat.d(TAG, "writePort 2");
        BluetoothGattService ACSService = mBluetoothGatt.getService(ACS_SERVICE_UUID);
        if (ACSService == null) {
            LogCat.e(TAG, "ACSService == null");
            return result;
        }
        LogCat.d(TAG, "writePort 3");
        BluetoothGattCharacteristic characteristic = ACSService.getCharacteristic(DATA_LINE_UUID);
        if (characteristic == null) {
            LogCat.e(TAG, "characteristic == null");
            return result;
        }
        characteristic.setValue(value);
        LogCat.d(TAG, "writePort 4");
        LogCat.d(TAG, "data: " + value.toString());
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //util.closeACSUtility();
        LogCat.logToFileFini();
    }

    class WorkerThread extends Thread {

        private int operationResult = 0;
        private int repeatSendTimes = 0;

        public int getOperationResult() {
            return operationResult;
        }

        public void setOperationResult(int operationResult) {
            this.operationResult = operationResult;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            int count = 0;
            wakeTimes = 0;
            LogCat.d(TAG, "There are " + packagesCount + " datas to be sended...");
            while (packagesCount > 0) {
                LogCat.d(TAG, "sending data...count" + count);
                byte[] blePackage = packages.get(count);
                writePackage(blePackage);
                synchronized (this) {
                    LogCat.d(TAG, "synchronized...");
                    try {
                        if (wakeTimes == 0) {
                            LogCat.d(TAG, "waiting...");
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                wakeTimes--;
                if (operationResult == 1) {
                    LogCat.d(TAG, "send succeed");
                    operationResult = 0;
                    packagesCount--;
                    count++;

                } else {
                    LogCat.d(TAG, "send failed");
                    while (repeatSendTimes < 3) {
                        LogCat.d(TAG, "repeat send data...count" + count);
                        writePackage(blePackage);
                        repeatSendTimes++;
                        synchronized (this) {
                            try {
                                if (wakeTimes == 0) {
                                    LogCat.d(TAG, "waiting...");
                                    this.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        wakeTimes--;
                        if (operationResult == 1) {
                            LogCat.d(TAG, "repeat succeed");
                            break;
                        }
                    }
                    if (repeatSendTimes == 3 && operationResult == 0) {
                        Message msg = mCurrentEventHandler.obtainMessage(EVENT_DATA_SEND_FAILED);
                        msg.sendToTarget();
                        return;
                    } else {
                        repeatSendTimes = 0;
                        operationResult = 0;
                        packagesCount--;
                        count++;
                    }
                }
            }
            Message msg = mCurrentEventHandler.obtainMessage(EVENT_DATA_SEND_SUCEED);
            msg.sendToTarget();
        }
    }

    class TimerThread extends Thread {
        private int mPackagesCount;

        public TimerThread(int packagesCount) {
            mPackagesCount = packagesCount;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            try {
                Thread.sleep(5000);
                if (mPackagesCount == packagesCount) {
                    workerThread.setOperationResult(0);
                    synchronized (workerThread) {
                        LogCat.d(TAG, "notify workerThread from timerThread!!!");
                        wakeTimes++;
                        workerThread.notify();
                    }
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private static class LogCat {
        static BufferedOutputStream bos;

        public static void logToFileInit() {
            File sdCard = Environment.getExternalStorageDirectory();
            File logFile = new File(sdCard, "Log.txt");
            try {
                bos = new BufferedOutputStream(new FileOutputStream(logFile));
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                Log.e(TAG, "FileNotFoundException");
                e.printStackTrace();
            }
        }

        public static void logToFileFini() {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(TAG, "flush failed or close failed");
                e.printStackTrace();
            }

        }

        private static void logToFile(String tag, String message) {
            if (bos == null) {
                return;
            }
            StringBuilder sb = new StringBuilder(tag);
            sb.append("   ").append(message).append("\n");
            try {
                bos.write(sb.substring(0).getBytes());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(TAG, "write failed");
                e.printStackTrace();
            }
        }

        public static int v(String tag, String msg) {
            logToFile(tag, msg);
            return Log.v(tag, msg);
        }

        public static int i(String tag, String msg) {
            logToFile(tag, msg);
            return Log.i(tag, msg);
        }

        public static int e(String tag, String msg) {
            logToFile(tag, msg);
            return Log.e(tag, msg);
        }

        public static int d(String tag, String msg) {
            logToFile(tag, msg);
            return Log.d(tag, msg);
        }

        public static int w(String tag, String msg) {
            logToFile(tag, msg);
            return Log.w(tag, msg);
        }
    }
}
