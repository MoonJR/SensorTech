package net.jongrakko.sensortech.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.jongrakko.sensortech.model.GasSensorModel;
import net.jongrakko.sensortech.R;
import net.jongrakko.sensortech.adapter.RecyclerRegAdapter;
import net.jongrakko.sensortech.libs.BluetoothManager;
import net.jongrakko.sensortech.libs.BluetoothManager.OnBluetoothListener;
import net.jongrakko.sensortech.libs.SensorTechBluetoothConnector;

import java.util.Set;

/**
 * Created by MoonJongRak on 2016. 3. 23..
 */
public class RegActivity extends AppCompatActivity implements OnBluetoothListener, View.OnClickListener {

    private BluetoothManager mBluetoothManager;

    private RecyclerRegAdapter mRecyclerRegAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        mBluetoothManager = BluetoothManager.getInstance(getContext());
        mBluetoothManager.setOnBluetoothListener(this);

        RecyclerView mRecyclerViewSensor = (RecyclerView) findViewById(R.id.recyclerViewSensor);
        mRecyclerViewSensor.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerRegAdapter = new RecyclerRegAdapter(null);
        mRecyclerViewSensor.setAdapter(mRecyclerRegAdapter);

        findViewById(R.id.buttonCancelSelect).setOnClickListener(this);
        findViewById(R.id.buttonDeviceNameModify).setOnClickListener(this);
        findViewById(R.id.buttonSelectDeviceReg).setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mBluetoothManager.startDeviceSearch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothManager.stopDeviceSearch();
    }

    private Context getContext() {
        return this;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonCancelSelect:
                mRecyclerRegAdapter.cancelSelect();
                break;
            case R.id.buttonDeviceNameModify:
                if (mRecyclerRegAdapter.getSelectedDevice() == null) {
                    Toast.makeText(getContext(), "장치를 선택해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                showModifyDialog();
                break;
            case R.id.buttonSelectDeviceReg:
                if (mRecyclerRegAdapter.getSelectedDevice() == null) {
                    Toast.makeText(getContext(), "장치를 선택해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                GasSensorModel.regDevice(getContext(), mRecyclerRegAdapter.getSelectedDevice());
                finish();
                break;
        }
    }

    @Override
    public void onFoundDevice(Set<BluetoothDevice> mBluetoothDevice, int rssi) {
        mRecyclerRegAdapter.notifyUpdate(mBluetoothDevice);
        mRecyclerRegAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onDestroy() {
        mBluetoothManager.setOnBluetoothListener(null);
        super.onDestroy();
    }

    ProgressDialog changeDeviceNameDialog;

    private void showModifyDialog() {
        if (changeDeviceNameDialog == null) {
            changeDeviceNameDialog = new ProgressDialog(getContext());
            changeDeviceNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mBluetoothManager.startDeviceSearch();
                }
            });
            changeDeviceNameDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    mBluetoothManager.stopDeviceSearch();
                }
            });
            changeDeviceNameDialog.setTitle("알림");
            changeDeviceNameDialog.setMessage("연결하는 중...");
            changeDeviceNameDialog.setCancelable(false);
        }
        final View inputView = getLayoutInflater().inflate(R.layout.dialog_change_device_name, null, false);

        ((EditText) inputView.findViewById(R.id.editTextDeviceName)).addTextChangedListener(new TextWatcher() {
            CharSequence beforeText = null;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeText = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.charAt(s.length() - 1) > Byte.MAX_VALUE) {
                    s.clear();
                    ((EditText) inputView.findViewById(R.id.editTextDeviceName)).setError("영문과 숫자 일부 특수문자만 입력 가능합니다.");
                }
            }
        });
        final AlertDialog inputDialog = new AlertDialog.Builder(getContext())
                .setIcon(R.mipmap.ic_launcher)
                .setView(inputView).create();
        inputView.findViewById(R.id.buttonDeviceNameModify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String changeDeviceName = ((EditText) inputView.findViewById(R.id.editTextDeviceName)).getText().toString();
                if (changeDeviceName.equals("")) {
                    ((EditText) inputView.findViewById(R.id.editTextDeviceName)).setError("변경할 이름을 입력해 주세요.");
                    return;
                } else if (changeDeviceName.length() > 20) {
                    ((EditText) inputView.findViewById(R.id.editTextDeviceName)).setError("장치명은 최대 20글자를 초과할 수 없습니다.");
                    return;
                }
                inputDialog.dismiss();
                final SensorTechBluetoothConnector connector = BluetoothManager.getBluetoothConnectThread(getContext(), mRecyclerRegAdapter.getSelectedDevice());
                connector.setOnBluetoothConnectListener(new SensorTechBluetoothConnector.OnBluetoothConnectListener() {
                    @Override
                    public void onStartConnect() {
                        changeDeviceNameDialog.show();
                    }

                    @Override
                    public void onConnected() {


                    }

                    @Override
                    public void onDisconnected() {
                        connector.close();
                        changeDeviceNameDialog.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeDeviceNameDialog.dismiss();
                                Toast.makeText(getContext(), "연결 해제.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }


                    //최소 한번 리드 된뒤에 데이터를 기록할 수 있음.
                    @Override
                    public void onReadData(byte[] data) {
                        connector.changeDeviceName(changeDeviceName);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeDeviceNameDialog.setMessage("이름 변경 중...");
                            }
                        });

                    }

                    @Override
                    public void onError() {
                        connector.close();
                        changeDeviceNameDialog.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeDeviceNameDialog.dismiss();
                                Toast.makeText(getContext(), "이름 변경 실패.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onWrote(final boolean isSuccess) {
                        connector.close();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeDeviceNameDialog.dismiss();
                                if (isSuccess) {
                                    Toast.makeText(getContext(), "이름 변경 성공.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getContext(), "이름 변경 실패.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                });
                connector.connect();
            }
        });
        inputDialog.show();
    }


}
