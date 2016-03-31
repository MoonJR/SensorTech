package net.jongrakko.sensortech.adapter;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.jongrakko.sensortech.R;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by MoonJongRak on 2016. 3. 23..
 */
public class RecyclerRegAdapter extends RecyclerView.Adapter<RecyclerRegAdapter.RegViewHolder> {

    private BluetoothDevice selectedDevice;

    private ArrayList<BluetoothDevice> devices;

    public RecyclerRegAdapter(ArrayList<BluetoothDevice> devices) {
        this.devices = devices;
    }


    @Override
    public RegViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_recycler_reg, parent, false);
        return new RegViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RegViewHolder holder, final int position) {
        final BluetoothDevice device = devices.get(position);

        if (device.equals(selectedDevice)) {
            holder.mLayoutMain.setBackgroundResource(R.drawable.bg_register_table03);
        } else {
            if (position % 2 == 0) {
                holder.mLayoutMain.setBackgroundResource(R.drawable.bg_register_table01);
            } else {
                holder.mLayoutMain.setBackgroundResource(R.drawable.bg_register_table02);
            }
        }

        holder.mTextViewDeviceName.setText(device.getName());
        holder.mTextViewDeviceAddress.setText(device.getAddress());
        holder.mLayoutMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDevice = device;
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        if (devices != null)
            return devices.size();
        else
            return 0;

    }

    public ArrayList<BluetoothDevice> getData() {
        return devices;
    }

    public BluetoothDevice getSelectedDevice() {
        return selectedDevice;
    }

    public void notifyUpdate(Set<BluetoothDevice> devices) {
        this.devices = new ArrayList<>(devices);
        notifyDataSetChanged();
    }

    public void cancelSelect() {
        selectedDevice = null;
        notifyDataSetChanged();
    }


    public class RegViewHolder extends RecyclerView.ViewHolder {

        private ViewGroup mLayoutMain;
        private TextView mTextViewDeviceAddress;
        private TextView mTextViewDeviceName;

        public RegViewHolder(View itemView) {
            super(itemView);
            mTextViewDeviceAddress = (TextView) itemView.findViewById(R.id.textViewDeviceAddress);
            mTextViewDeviceName = (TextView) itemView.findViewById(R.id.textViewDeviceName);
            mLayoutMain = (ViewGroup) itemView.findViewById(R.id.layoutMain);
        }
    }


}
