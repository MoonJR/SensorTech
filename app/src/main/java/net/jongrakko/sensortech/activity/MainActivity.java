package net.jongrakko.sensortech.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.jongrakko.sensortech.model.GasSensorModel;
import net.jongrakko.sensortech.R;
import net.jongrakko.sensortech.libs.BluetoothManager;
import net.jongrakko.sensortech.libs.GasStatusView;

import java.util.Set;

/**
 * Created by MoonJongRak on 2016. 3. 19..
 */
public class MainActivity extends AppCompatActivity implements BluetoothManager.OnBluetoothStateChangeListener, View.OnClickListener {

    private BluetoothManager mBluetoothManager;
    private int[] layoutGasStatusIds = {R.id.layoutGasStatus0, R.id.layoutGasStatus1, R.id.layoutGasStatus2, R.id.layoutGasStatus3};
    private GasStatusView[] mGasStatusViews = new GasStatusView[4];

    private AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothManager = BluetoothManager.getInstance(getContext());
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

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
        setVolumeMax();
        mBluetoothManager.setOnBluetoothStateChangeListener(this);
        setGasSensorModel();
        super.onResume();
    }

    @Override
    protected void onPause() {
        setVolumeDefault();
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

    private int selectedDeleteDevice = 0;

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
                if (selectedDeleteDevice > -1 && selectedDeleteDevice < mGasStatusViews.length) {
                    GasSensorModel.deleteRegDevice(getContext(), mGasStatusViews[selectedDeleteDevice].getGasSensorModel().getBluetoothDevice().getAddress());
                    setGasSensorModel();
                }
            }
        }).setNegativeButton("취소", null).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                selectedDeleteDevice = 0;
            }
        }).setCancelable(false).setIcon(R.mipmap.ic_launcher).show();
    }

    @Override
    public void onClick(View v) {
        showEditDialog();
    }

    private long backKeyPressedTime;

    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if (now > backKeyPressedTime + 2000) {
            backKeyPressedTime = now;
            Toast.makeText(getContext(), "\'뒤로\'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (now <= backKeyPressedTime + 2000) {
            super.onBackPressed();
        }
    }

    private int defaultVolume;

    private void setVolumeMax() {
        defaultVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);
    }

    private void setVolumeDefault() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, defaultVolume, 0);
    }

}
