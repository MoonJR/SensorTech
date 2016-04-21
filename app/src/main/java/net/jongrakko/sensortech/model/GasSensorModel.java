package net.jongrakko.sensortech.model;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import net.jongrakko.sensortech.libs.BluetoothManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by MoonJongRak on 2016. 3. 19..
 */
public class GasSensorModel {
    private int index;

    private byte material;
    private byte status;
    private float temperature;
    private float humid;
    private BluetoothDevice bluetoothDevice;


    public GasSensorModel(int index, BluetoothDevice bluetoothDevice) {
        this.index = index;
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getDeviceName() {
        return bluetoothDevice.getName();
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setmBluetoothDevice(BluetoothDevice mBluetoothDevice) {
        this.bluetoothDevice = mBluetoothDevice;
    }


    public byte getMaterial() {
        return material;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumid() {
        return humid;
    }

    public String getMaterialString() {
        switch (material) {
            case -1:
                return "모름";
            case 0:
                return "없음";
            case 1:
                return "불산";
            case 2:
                return "질산";
            case 3:
                return "황산";
            case 4:
                return "암모니아";
            case 5:
                return "파라티온";
            case 6:
                return "말라티온";
            case 10:
                return "모의작용";
            case 11:
                return "GB";
            case 12:
                return "GD";
            case 13:
                return "VX";
            case 14:
                return "HD";
            case 15:
                return "AC";
            case 16:
                return "CG";
            default:
                return "미등록";

        }
    }


    //-1: 오류0 : 정상1 : 주의2 : 위험
    public byte getStatus() {
        return status;
    }

    public String getStatusString() {
        switch (status) {
            case -1:
                return "오류";
            case 0:
                return "정상";
            case 1:
                return "주의";
            case 2:
                return "위험";
            default:
                return "오류";
        }
    }


    public void setData(byte[] data) {
        this.material = data[4];
        this.status = data[5];
        this.temperature = data[6] + data[7] / 100f;
        this.humid = data[9] + data[10] / 100f;
    }

    public static boolean regDevice(Context context, BluetoothDevice device) {
        SharedPreferences preferences = context.getSharedPreferences("BLUETOOTH_DEVICE", Context.MODE_PRIVATE);
        Set<String> devices = preferences.getStringSet("DEVICES", new TreeSet<String>());
        if (!devices.add(device.getAddress())) {
            return false;
        }
        preferences.edit().clear().putStringSet("DEVICES", devices).apply();
        return true;
    }

    public static Set<String> getRegDevicesAddress(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("BLUETOOTH_DEVICE", Context.MODE_PRIVATE);
        return preferences.getStringSet("DEVICES", new TreeSet<String>());
    }

    public static Set<BluetoothDevice> getRegDevices(Context context) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return null;
        }
        SharedPreferences preferences = context.getSharedPreferences("BLUETOOTH_DEVICE", Context.MODE_PRIVATE);
        Set<String> devicesAddress = preferences.getStringSet("DEVICES", new TreeSet<String>());
        Set<BluetoothDevice> devices = new HashSet<>();
        for (String deviceAddress : devicesAddress) {
            devices.add(mBluetoothAdapter.getRemoteDevice(deviceAddress));
        }
        return devices;
    }

    public static void deleteRegDevice(Context context, String address) {
        SharedPreferences preferences = context.getSharedPreferences("BLUETOOTH_DEVICE", Context.MODE_PRIVATE);
        Set<String> devices = preferences.getStringSet("DEVICES", new TreeSet<String>());
        devices.remove(address);
        preferences.edit().clear().putStringSet("DEVICES", devices).apply();
    }

    public String getDeviceTitle() {
        return bluetoothDevice != null && bluetoothDevice.getName() != null ? (index + 1) + ". " + bluetoothDevice.getName() : (index + 1) + ". " + "장치명 모름";
    }

    public int getIndex() {
        return index;
    }

}
