package net.jongrakko.sensortech;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import net.jongrakko.sensortech.libs.BluetoothManager;
import net.jongrakko.sensortech.libs.GasStatusView;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by MoonJongRak on 2016. 3. 19..
 */
public class MainActivity extends AppCompatActivity implements BluetoothManager.OnBluetoothStateChangeListener, View.OnClickListener {

    private BluetoothManager mBluetoothManager;
    int[] layoutGasStatusIds = {R.id.layoutGasStatus0, R.id.layoutGasStatus1, R.id.layoutGasStatus2, R.id.layoutGasStatus3};
    GasStatusView[] mGasStatusViews = new GasStatusView[4];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothManager = BluetoothManager.getInstance(getContext());

        findViewById(R.id.buttonDeviceEdit).setOnClickListener(this);
        for (int i = 0; i < mGasStatusViews.length; i++) {
            mGasStatusViews[i] = new GasStatusView(getContext());
            ((ViewGroup) findViewById(layoutGasStatusIds[i])).addView(mGasStatusViews[i]);
        }

        if (!mBluetoothManager.isBluetoothEnable()) {
            mBluetoothManager.bluetoothEnable();
            Toast.makeText(getContext(), "블루투스 켜는중...", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        mBluetoothManager.setOnBluetoothStateChangeListener(this);
        setGasSensorModel();
        super.onResume();
    }

    @Override
    protected void onPause() {
        closeGasSensor();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mBluetoothManager.close();
        super.onDestroy();
    }

    private Context getContext() {
        return this;
    }

    private void setGasSensorModel() {
        closeGasSensor();
        Set<BluetoothDevice> devices = GasSensorModel.getRegDevices(getContext());
        int index = 0;

        for (BluetoothDevice device : devices) {
            mGasStatusViews[index].setGasSensorModel(new GasSensorModel(index, device));
            index++;
        }

        for (int i = 3; i >= index; i--) {
            mGasStatusViews[i].setGasSensorModel(null);
        }
    }

    private void closeGasSensor() {
        for (GasStatusView view : mGasStatusViews) {
            view.close();
        }
    }


    @Override
    public void onChangeState(boolean on) {
        if (on) {
            setGasSensorModel();
        } else {
            Toast.makeText(getContext(), "자동으로 블루투스를 다시 시작합니다.", Toast.LENGTH_SHORT).show();
            mBluetoothManager.bluetoothEnable();
        }
    }

    private int selectedDeleteDevice = -1;

    private void showEditDialog() {
        ArrayAdapter<String> deviceAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice);
        for (GasStatusView mGasStatusView : mGasStatusViews) {
            if (mGasStatusView.getGasSensorModel() != null)
                deviceAdapter.add(mGasStatusView.getGasSensorModel().getDeviceTitle());
        }

        if (deviceAdapter.getCount() == 0) {
            Toast.makeText(getContext(), "편집할 장치가 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("장치 삭제").setSingleChoiceItems(deviceAdapter, selectedDeleteDevice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedDeleteDevice = which;
            }
        }).setPositiveButton("삭제", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GasSensorModel.deleteRegDevice(getContext(), mGasStatusViews[selectedDeleteDevice].getGasSensorModel().getBluetoothDevice().getAddress());
                setGasSensorModel();
            }
        }).setNegativeButton("취소", null).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                selectedDeleteDevice = -1;
            }
        }).setCancelable(false).setIcon(R.mipmap.ic_launcher).show();
    }

    @Override
    public void onClick(View v) {
        showEditDialog();
    }
}
