package net.jongrakko.sensortech;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import net.jongrakko.sensortech.libs.BluetoothManager;

import java.util.Set;

/**
 * Created by MoonJongRak on 2016. 3. 19..
 */
public class TestActivity extends MainActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_test);
//
//
//        ((Switch) findViewById(R.id.switch1)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//                    BluetoothManager.getInstance(getContext());
//                } else {
//                    BluetoothManager.getInstance(getContext()).close();
//                }
//            }
//        });
//
//        findViewById(R.id.buttonStart).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                BluetoothManager.getInstance(getContext()).startDeviceSearch();
//            }
//        });
//    }
//
//    private Context getContext() {
//        return this;
//    }
}
