package com.freshwind.smarthome;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.freshwind.smarthome.ConnectingActivity.EXTRAS_DEVICE;

public class ScanActivity extends AppCompatActivity
{
    private static final String TAG = "ScanActivity";
    private static final String ssidRegex = "SMART_.+";

    private DevicesAdapter devicesAdapter;
    private Handler handler;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private WifiManager wifiManager;

    private boolean isScanning = false;

    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            {
                success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
            }

            if (success)
            {
                scanSuccess();
            }
            else
            {
                scanFailure();
            }

            isScanning = false;
            invalidateOptionsMenu();
        }
    };

    private void scanSuccess()
    {
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult result : results)
        {
            if (result.SSID.matches(ssidRegex))
            {
                devicesAdapter.addDevice(result);
            }
        }

        devicesAdapter.notifyDataSetChanged();
    }

    private void scanFailure()
    {
        Log.w(TAG, "scan FAILED");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);

        handler = new Handler();
        recyclerView = findViewById(R.id.list);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<ScanResult> list = new ArrayList<>();
        devicesAdapter = new DevicesAdapter(list);
        recyclerView.setAdapter(devicesAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerClickListener(this) {
            @Override
            public void onItemClick(RecyclerView recyclerView, View itemView, int position)
            {
                final Intent intent = new Intent(ScanActivity.this, ConnectingActivity.class);
                TextView textName = itemView.findViewById(R.id.device_name);
                TextView textAddress = itemView.findViewById(R.id.device_address);

                WifiConfiguration config = new WifiConfiguration();
                ScanResult result = devicesAdapter.scanResults.get(position);

                config.SSID = "\"" + result.SSID + "\"";
                config.BSSID = "\"" + result.BSSID + "\"";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                Kettle kettle = new Kettle(textName.getText(), textAddress.getText());
                kettle.configuration = config;
                // TODO переместить куда-нибудь в настройки или константы
                kettle.selfIP = "192.168.42.1";
                kettle.port = 3333;
                kettle.connection = Kettle.Connection.selfAp;
                intent.putExtra(EXTRAS_DEVICE, kettle);

                startActivity(intent);
                finish();
            }
        });

        scan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        if (!isScanning)
        {
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        else
        {
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
                devicesAdapter.clear();
                devicesAdapter.notifyDataSetChanged();
                scan();
                break;

            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    private boolean checkPermissions()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED)
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                //Toast.makeText(this, "Не удалось выполнить поиск устройств", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[] {Manifest.permission.CHANGE_WIFI_STATE}, 0);

            }
            return false;
        }

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

    private void scan()
    {
        Log.d(TAG, Arrays.toString(wifiManager.getScanResults().toArray()));

        if(!checkPermissions())
        {
            return;
        }


        if (!wifiManager.isWifiEnabled())
        {
            // TODO create warning "enable WIFI" view
            Snackbar
                    .make(recyclerView, "Wi-Fi выключен", Snackbar.LENGTH_LONG)
                    .setAction("включить", new OnClickListener() {
                        @Override
                        public void onClick(View v)
                        {
                            wifiManager.setWifiEnabled(true);
                        }
                    })
                    .show();

            return;
        }

        if(!isGeoEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            // TODO create warning "enable Location" view
            Snackbar
                    .make(recyclerView, "Не удалось выполнить поиск сетей: GPS выключен", Snackbar.LENGTH_LONG)
                    .setAction("включить", new OnClickListener() {
                        @Override
                        public void onClick(View v)
                        {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .show();
        }

        isScanning = true;
        wifiManager.startScan();
        invalidateOptionsMenu();
    }

    public boolean isGeoEnabled()
    {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean mIsGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean mIsNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return mIsGPSEnabled || mIsNetworkEnabled;
    }

    private class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>{
        public ArrayList<ScanResult> scanResults;
        private LayoutInflater inflater;

        public class DeviceViewHolder extends RecyclerView.ViewHolder
        {
            View device;

            public DeviceViewHolder(View device) {
                super(device);
                this.device = device;
            }
        }

        public void addDevice(ScanResult result)
        {
            if (!scanResults.contains(result))
            {
                scanResults.add(result);
            }
        }

        public void updateDevices(List<ScanResult> results)
        {
            scanResults.clear();
            scanResults.addAll(results);
        }

        public void clear()
        {
            scanResults.clear();
        }

        public DevicesAdapter(ArrayList<ScanResult> devices)
        {
            scanResults = devices;
            inflater = ScanActivity.this.getLayoutInflater();
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.listitem_device, parent, false);

            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position)
        {
            View view = holder.device;

            TextView nameTextView = view.findViewById(R.id.device_name);
            TextView addressTextView = view.findViewById(R.id.device_address);

            String name = scanResults.get(position).SSID;
            if (null != name && name.length() > 0)
            {
                nameTextView.setText(name);
            }
            else
            {
                nameTextView.setText(R.string.unknown_device);
            }

            addressTextView.setText(scanResults.get(position).BSSID);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getItemCount() {
            return scanResults.size();
        }
    }
}
