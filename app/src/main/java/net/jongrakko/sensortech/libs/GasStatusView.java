package net.jongrakko.sensortech.libs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.jongrakko.sensortech.R;
import net.jongrakko.sensortech.activity.RegActivity;
import net.jongrakko.sensortech.model.GasSensorModel;

/**
 * Created by MoonJongRak on 2016. 3. 19..
 */
public class GasStatusView extends LinearLayout implements View.OnClickListener, SensorTechBluetoothConnector.OnBluetoothConnectListener, GasSensorModel.OnStatusChangeListener {

    private GasSensorModel mGasSensorModel;
    private SensorTechBluetoothConnector mSensorTechBluetoothConnector;

    private TextView mTextViewDeviceName;
    private TextView mTextViewDetectObject;
    private TextView mTextViewGasStatus;
    private ImageView mImageViewDeviceStatus;
    private ImageButton mImageButtonAdd;

    private SoundPool mSoundPool;
    private int soundWaring;
    private int soundDanger;

    public GasStatusView(Context context) {
        super(context);
        setLayout();
        init();
    }

    public void setGasSensorModel(GasSensorModel model) {
        mGasSensorModel = model;
        init();
    }

    public void setLayout() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.view_gas_status, this, false);
        addView(v);
        mTextViewDeviceName = (TextView) findViewById(R.id.textViewDeviceName);
        mTextViewDetectObject = (TextView) findViewById(R.id.textViewDetectObject);
        mTextViewGasStatus = (TextView) findViewById(R.id.textViewGasStatus);
        mImageViewDeviceStatus = (ImageView) findViewById(R.id.imageViewDeviceStatus);
        mImageButtonAdd = (ImageButton) findViewById(R.id.imageButtonAdd);
        mImageButtonAdd.setOnClickListener(this);
        mSoundPool = new SoundPool(2, AudioManager.STREAM_NOTIFICATION, 0);
        soundWaring = mSoundPool.load(getContext(), R.raw.sound_waring, 1);
        soundDanger = mSoundPool.load(getContext(), R.raw.sound_danger, 2);

    }

    private void init() {
        close();
        if (mGasSensorModel == null || mGasSensorModel.getBluetoothDevice() == null) {
            modeAddDevice();
            return;
        }
        mGasSensorModel.setOnStatusChangeListener(this);
        modeConnectDevice();
        mTextViewDeviceName.setText(mGasSensorModel.getDeviceTitle());
        mSensorTechBluetoothConnector = BluetoothManager.getBluetoothConnectThread(getContext(), mGasSensorModel.getBluetoothDevice());
        mSensorTechBluetoothConnector.setOnBluetoothConnectListener(this);
        mSensorTechBluetoothConnector.connect();
    }

    public GasSensorModel getGasSensorModel() {
        return mGasSensorModel;
    }

    private void modeAddDevice() {
        findViewById(R.id.layoutMain).setVisibility(GONE);
        mImageButtonAdd.setVisibility(VISIBLE);
    }

    private void modeConnectDevice() {
        findViewById(R.id.layoutMain).setVisibility(VISIBLE);
        mImageButtonAdd.setVisibility(GONE);
    }

    private void modeConnectState01() {
        modeConnectDevice();
        findViewById(R.id.layoutGasStatus).setVisibility(GONE);
        mImageViewDeviceStatus.setVisibility(VISIBLE);
        mImageViewDeviceStatus.setImageResource(R.drawable.ic_sense_linkstate01);
        mImageViewDeviceStatus.setOnClickListener(null);
    }

    private void modeConnectState02() {
        modeConnectDevice();
        findViewById(R.id.layoutGasStatus).setVisibility(GONE);
        mImageViewDeviceStatus.setVisibility(VISIBLE);
        mImageViewDeviceStatus.setImageResource(R.drawable.ic_sense_linkstate02);
        mImageViewDeviceStatus.setOnClickListener(null);
        if (mImageViewDeviceStatus.getDrawable() instanceof AnimationDrawable) {
            ((AnimationDrawable) mImageViewDeviceStatus.getDrawable()).start();
        }
    }

    private void modeConnectState03() {
        modeConnectDevice();
        findViewById(R.id.layoutGasStatus).setVisibility(GONE);
        mImageViewDeviceStatus.setVisibility(VISIBLE);
        mImageViewDeviceStatus.setImageResource(R.drawable.ic_sense_linkstate03);
        mImageViewDeviceStatus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle("오류")
                        .setMessage("다시 연결하시겠습니까?")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("재연결", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                init();
                            }
                        }).show();
            }
        });
    }

    private void modeConnected() {
        if (mImageViewDeviceStatus.getDrawable() instanceof AnimationDrawable) {
            ((AnimationDrawable) mImageViewDeviceStatus.getDrawable()).stop();
        }
        findViewById(R.id.layoutGasStatus).setVisibility(VISIBLE);
        mImageViewDeviceStatus.setVisibility(GONE);
    }

    private void modeDeviceStatus0() {
        mTextViewGasStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        ((ImageView) findViewById(R.id.imageViewDeviceStatus0)).setImageResource(R.drawable.ic_sense_state03);
        ((ImageView) findViewById(R.id.imageViewDeviceStatus1)).setImageResource(R.drawable.ic_sense_state02);
        ((ImageView) findViewById(R.id.imageViewDeviceStatus2)).setImageResource(R.drawable.ic_sense_state02);
    }

    private void modeDeviceStatus1() {
        mTextViewGasStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        ((ImageView) findViewById(R.id.imageViewDeviceStatus0)).setImageResource(R.drawable.ic_sense_state05);
        ((ImageView) findViewById(R.id.imageViewDeviceStatus1)).setImageResource(R.drawable.ic_sense_state05);
        ((ImageView) findViewById(R.id.imageViewDeviceStatus2)).setImageResource(R.drawable.ic_sense_state04);
    }

    private void modeDeviceStatus2() {
        mTextViewGasStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        ((ImageView) findViewById(R.id.imageViewDeviceStatus0)).setImageResource(R.drawable.ic_sense_state06);
        ((ImageView) findViewById(R.id.imageViewDeviceStatus1)).setImageResource(R.drawable.ic_sense_state06);
        ((ImageView) findViewById(R.id.imageViewDeviceStatus2)).setImageResource(R.drawable.ic_sense_state06);
    }

    public void close() {
        if (mSensorTechBluetoothConnector != null)
            mSensorTechBluetoothConnector.close();
        if (mCheckTimeoutThread != null) {
            mCheckTimeoutThread.interrupt();
        }
        post(new Runnable() {
            @Override
            public void run() {
                if (mGasSensorModel == null || mGasSensorModel.getBluetoothDevice() == null) {
                    modeAddDevice();
                } else {
                    modeConnectState03();
                }
            }
        });
    }

    private CheckTimeoutThread mCheckTimeoutThread;

    @Override
    public void onStartConnect() {
        mCheckTimeoutThread = new CheckTimeoutThread(8000, 3000);
        mCheckTimeoutThread.start();
        post(new Runnable() {
            @Override
            public void run() {
                modeConnectState01();
            }
        });
    }

    @Override
    public void onConnected() {
        mCheckTimeoutThread.setConnected(true);
        post(new Runnable() {
            @Override
            public void run() {
                modeConnectState02();
            }
        });
        postDelayed(new Runnable() {
            @Override
            public void run() {
                modeConnected();
            }
        }, 1000);
    }

    @Override
    public void onDisconnected() {
        close();
    }

    @Override
    public void onReadData(final byte[] data) {
        mCheckTimeoutThread.setRead(true);
        if (data.length == 14) {
            mGasSensorModel.setData(data);
            notifyData();
        }
    }

    private void notifyData() {
        post(new Runnable() {
            @Override
            public void run() {
                mTextViewDeviceName.setText(mGasSensorModel.getDeviceTitle());
                mTextViewGasStatus.setText(mGasSensorModel.getStatusString());
                mTextViewDetectObject.setText(mGasSensorModel.getMaterialString());

                switch (mGasSensorModel.getStatus()) {
                    case 0:
                        modeDeviceStatus0();
                        break;
                    case 1:
                        modeDeviceStatus1();
                        break;
                    case 2:
                        modeDeviceStatus2();
                        break;
                    default:
                        modeDeviceStatus2();
                        break;
                }
            }
        });
    }

    @Override
    public void onError() {
        close();
    }

    @Override
    public void onWrote(boolean isSuccess) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonAdd:
                getContext().startActivity(new Intent(getContext(), RegActivity.class));
                break;
        }
    }

    @Override
    public void onStateChange(final int status) {
        post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case GasSensorModel.OnStatusChangeListener.FLAG_STATUS_CHANGE_NORMAL:
                        break;
                    case GasSensorModel.OnStatusChangeListener.FLAG_STATUS_CHANGE_WARING:
                        mSoundPool.play(soundWaring, 1.0F, 1.0F, 1, 0, 1.0F);
                        break;
                    case GasSensorModel.OnStatusChangeListener.FLAG_STATUS_CHANGE_DANGER:
                        mSoundPool.play(soundDanger, 1.0F, 1.0F, 1, 0, 1.0F);
                        break;
                }
            }
        });

    }


    private class CheckTimeoutThread extends Thread {
        boolean read;
        boolean isConnected;

        long connectTimeout;
        long readTimeout;


        private CheckTimeoutThread(long connectTimeout, long readTimeout) {
            this.read = true;
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;

        }

        @Override
        public void run() {
            try {
                if (!isConnected) {
                    sleep(connectTimeout);
                }
                if (!isConnected) {
                    close();
                    return;
                }
                while (read) {
                    read = false;
                    sleep(readTimeout);
                }
            } catch (InterruptedException ignored) {

            }
            close();

        }

        private void setConnected(boolean isConnected) {
            this.isConnected = isConnected;
        }

        private void setRead(boolean read) {
            this.read = read;
        }

    }

}
