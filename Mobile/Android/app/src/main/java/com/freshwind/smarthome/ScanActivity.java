package com.freshwind.smarthome;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity
{
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private BluetoothLeScanner bleScanner;
    private RecyclerView recyclerView;
    private BleDevicesAdapter bleDevicesAdapter;
    private LinearLayoutManager layoutManager;

    private boolean isScanning = false;
    private final int SCAN_PERIOD = 10000;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            final BluetoothDevice device = result.getDevice();
            runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    bleDevicesAdapter.addDevice(device);
                    bleDevicesAdapter.notifyDataSetChanged();
                }
            });
            super.onScanResult(callbackType, result);
        }
    };

    private Runnable cancelSearching = new Runnable() {
        @Override
        public void run()
        {
            isScanning = false;
            bleScanner.stopScan(scanCallback);
            invalidateOptionsMenu();
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);

        handler = new Handler();
        recyclerView = findViewById(R.id.list);

        final BluetoothManager manager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        assert manager != null;
        bluetoothAdapter = manager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<BluetoothDevice> list = new ArrayList<>();
        bleDevicesAdapter = new BleDevicesAdapter(list);
        recyclerView.setAdapter(bleDevicesAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerClickListener(this) {
            @Override
            public void onItemClick(RecyclerView recyclerView, View itemView, int position)
            {
                final Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                TextView textName = itemView.findViewById(R.id.device_name);
                TextView textAddress = itemView.findViewById(R.id.device_address);
                intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, textName.getText());
                intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, textAddress.getText());
                if (isScanning) {
                    bleScanner.stopScan(scanCallback);
                    isScanning = false;
                }
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        if (!isScanning)
        {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        else
        {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_scan:
                bleDevicesAdapter.clear();
                bleDevicesAdapter.notifyDataSetChanged();
                scan(true);
                break;
            case R.id.menu_stop:
                scan(false);
                break;
        }

        return true;
    }

    private boolean checkPermissions()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                //Toast.makeText(this, "Не удалось выполнить поиск устройств", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

            }
            return false;
        }
        return true;
    }

    private void scan(final boolean enable)
    {
        checkPermissions();

        if(enable)
        {
            handler.postDelayed(cancelSearching, SCAN_PERIOD);

            isScanning = true;
            bleScanner.startScan(scanCallback);
        }
        else
        {
            handler.removeCallbacks(cancelSearching);

            isScanning = false;
            bleScanner.stopScan(scanCallback);
        }

        invalidateOptionsMenu();
    }

    private class BleDevicesAdapter extends RecyclerView.Adapter<BleDevicesAdapter.BleDeviceViewHolder>{
        private ArrayList<BluetoothDevice> bleDevices;
        private LayoutInflater inflater;

        public class BleDeviceViewHolder extends RecyclerView.ViewHolder
        {
            View device;

            public BleDeviceViewHolder(View device) {
                super(device);
                this.device = device;
            }
        }

        public void addDevice(BluetoothDevice device)
        {
            if(!bleDevices.contains(device))
            {
                bleDevices.add(device);
            }
        }

        public void clear()
        {
            bleDevices.clear();
        }

        public BleDevicesAdapter(ArrayList<BluetoothDevice> devices)
        {
            bleDevices = devices;
            inflater = ScanActivity.this.getLayoutInflater();
        }



        @Override
        public BleDeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_device, parent, false);
            BleDeviceViewHolder holder = new BleDeviceViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(BleDeviceViewHolder holder, int position)
        {
            View view = holder.device;

            TextView nameTextView = view.findViewById(R.id.device_name);
            TextView addressTextView = view.findViewById(R.id.device_address);

            String name = bleDevices.get(position).getName();
            if (null != name && name.length() > 0)
            {
                nameTextView.setText(name);
            }
            else
            {
                nameTextView.setText(R.string.unknown_device);
            }

            addressTextView.setText(bleDevices.get(position).getAddress());
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getItemCount() {
            return bleDevices.size();
        }
    }
}
