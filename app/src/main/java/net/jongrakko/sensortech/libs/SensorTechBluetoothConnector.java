package net.jongrakko.sensortech.libs;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by MoonJongRak on 2016. 3. 25..
 */
public class SensorTechBluetoothConnector {

    private final UUID ACS_SERVICE_UUID = UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb");
    private final UUID CMD_LINE_UUID = UUID.fromString("0000ffb1-0000-1000-8000-00805f9b34fb");
    private final UUID DATA_LINE_UUID = UUID.fromString("0000ffb2-0000-1000-8000-00805f9b34fb");
    private final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private OnBluetoothConnectListener listener;

    private Context mContext;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private DataWriteThread mDataWriteThread;

    public SensorTechBluetoothConnector(Context mContext, BluetoothDevice mBluetoothDevice) {
        this.mContext = mContext;
        this.mBluetoothDevice = mBluetoothDevice;
    }

    public void connect() {
        if (listener != null) {
            listener.onStartConnect();
        }

        if (mBluetoothDevice == null) {
            if (listener != null) {
                listener.onError();
            }
        } else {
            mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mBluetoothGattCallback);
            if (mBluetoothGatt == null) {
                if (listener != null) {
                    listener.onError();
                }
            }
        }
    }

    public void close() {
        try {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        } catch (Exception ignored) {

        }

    }

    public void setOnBluetoothConnectListener(OnBluetoothConnectListener listener) {
        this.listener = listener;
    }


    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        Handler mHandler = new Handler();

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d("yeonBlueTooth", "<--onConnectionStateChange-->");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("yeonBlueTooth", "onConnect");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("yeonBlueTooth", "onDisconnect");
                if (listener != null) {
                    listener.onDisconnected();
                }
            } else {
                Log.d("yeonBlueTooth", "unknowed status");
            }
            Log.d("yeonBlueTooth", "<--onConnectionStateChange-->");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("yeonBlueTooth", "<--onServicesDiscovered-->");
            boolean isError = false;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("yeonBlueTooth", "success service discover");
                BluetoothGattService mBluetoothGattService = gatt.getService(ACS_SERVICE_UUID);
                BluetoothGattCharacteristic mBluetoothGattCharacteristicCmdLine = mBluetoothGattService.getCharacteristic(CMD_LINE_UUID);
                if (mBluetoothGattCharacteristicCmdLine != null) {
                    gatt.setCharacteristicNotification(mBluetoothGattCharacteristicCmdLine, true);
                    BluetoothGattDescriptor descriptor = mBluetoothGattCharacteristicCmdLine.getDescriptor(CCC);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (gatt.writeDescriptor(descriptor)) {
                        Log.d("yeonBlueTooth", "success writeDescriptor");
                    } else {
                        Log.d("yeonBlueTooth", "fail writeDescriptor");
                        isError = true;
                    }
                } else {
                    Log.d("yeonBlueTooth", "cmdLine null");
                    isError = true;
                }
            } else {
                Log.d("yeonBlueTooth", "not discovery");
                isError = true;
            }
            if (isError) {
                if (listener != null) {
                    listener.onError();
                }
            }
            Log.d("yeonBlueTooth", "<--onServicesDiscovered-->");


        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("yeonBlueTooth", "<--onDescriptorWrite-->");

            boolean isError = false;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.getCharacteristic().getUuid().equals(DATA_LINE_UUID)) {
                    Log.d("yeonBlueTooth", "success Data Line Connect");
                    if (listener != null) {
                        listener.onConnected();
                    }
                } else if (descriptor.getCharacteristic().getUuid().equals(CMD_LINE_UUID)) {
                    Log.d("yeonBlueTooth", "start connect Data Line");
                    BluetoothGattCharacteristic mBluetoothGattCharacteristicDataLine = gatt.getService(ACS_SERVICE_UUID).getCharacteristic(DATA_LINE_UUID);
                    gatt.setCharacteristicNotification(mBluetoothGattCharacteristicDataLine, true);
                    descriptor = mBluetoothGattCharacteristicDataLine.getDescriptor(CCC);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (gatt.writeDescriptor(descriptor)) {
                        Log.d("yeonBlueTooth", "success writeDescriptor");
                    } else {
                        Log.d("yeonBlueTooth", "fail writeDescriptor");
                        isError = true;
                    }
                }
            } else {
                Log.d("yeonBlueTooth", "fail onDescriptorWrite");
                isError = false;
            }

            if (isError) {
                if (listener != null) {
                    listener.onError();
                }
            }

            Log.d("yeonBlueTooth", "<--onDescriptorWrite-->");

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("yeonBlueTooth", "<--onServicesDiscovered-->");
            if (listener != null) {
                listener.onReadData(characteristic.getValue());
            }
            Log.d("yeonBlueTooth", "<--onServicesDiscovered-->");

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, final int status) {
            Log.d("yeonBlueTooth", "<--onCharacteristicWrite-->");

            if (mDataWriteThread == null) {
                return;
            }

            if (characteristic.getUuid().equals(DATA_LINE_UUID)) {
                mDataWriteThread.setSuccess(status == BluetoothGatt.GATT_SUCCESS);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    synchronized (mDataWriteThread) {
                        mDataWriteThread.setWait(false);
                        mDataWriteThread.notify();
                    }
                } else {
                    mDataWriteThread.interrupt();
                }
            }
            Log.d("yeonBlueTooth", "<--onCharacteristicWrite-->");

        }
    };


    public interface OnBluetoothConnectListener {
        void onStartConnect();

        void onConnected();

        void onDisconnected();

        void onReadData(byte[] data);

        void onError();

        void onWrote(boolean isSuccess);


    }


    public void changeDeviceName(String name) {
        String command = "SPP:setName =" + name + "\r\n\0";
        mDataWriteThread = new DataWriteThread(command.getBytes());
        mDataWriteThread.start();

    }

    private class DataWriteThread extends Thread {

        private int index;
        private boolean isSuccess;
        private boolean wait;
        private ArrayList<byte[]> dataArray;

        private DataWriteThread(byte[] data) {
            dataArray = new ArrayList<>();
            wait = true;
            int rest = data.length;
            for (int i = 0; rest > 0; i++) {
                byte[] blePackage;
                if (rest >= 20) {
                    blePackage = new byte[20];
                    System.arraycopy(data, i * 20, blePackage, 0, 20);
                } else {
                    blePackage = new byte[rest];
                    System.arraycopy(data, i * 20, blePackage, 0, rest);
                }
                rest -= 20;
                dataArray.add(blePackage);
            }
        }

        @Override
        public void run() {
            try {
                for (index = 0; index < dataArray.size(); index++) {
                    writeData(dataArray.get(index));

                    synchronized (this) {
                        if (wait) {
                            wait();
                        }
                    }
                    wait = true;
                }

                sleep(1000);
            } catch (InterruptedException ignored) {

            }

            if (listener != null) {
                listener.onWrote(isSuccess);
            }
        }

        private void setSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }

        private void setWait(boolean wait) {
            this.wait = wait;
        }
    }


    private void writeData(byte[] value) {
        BluetoothGattService ACSService = mBluetoothGatt.getService(ACS_SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = ACSService.getCharacteristic(DATA_LINE_UUID);
        characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }


}
