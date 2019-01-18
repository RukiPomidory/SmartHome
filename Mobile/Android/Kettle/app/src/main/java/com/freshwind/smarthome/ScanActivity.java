package com.freshwind.smarthome;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static com.freshwind.smarthome.ConnectingActivity.EXTRAS_DEVICE;

public class ScanActivity extends AppCompatActivity
{
    private static final String TAG = "ScanActivity";
    private static final String ssidRegex = "SMART_.+";

    private DevicesAdapter devicesAdapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private WifiManager wifiManager;
    private WifiScanner scanner;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);

        recyclerView = findViewById(R.id.list);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        scanner = new WifiScanner(this);
        scanner.setViewForTestFeature(recyclerView);
        scanner.setOnResultListener(new WifiScanner.ScanResultListener() {
            @Override
            public void onScanSuccess()
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

            @Override
            public void onScanFailure()
            {
                Log.w(TAG, "scan FAILED");
            }
        });
        scanner.setOnStateChangedListener(new WifiScanner.ScanStateListener() {
            @Override
            public void onScanStateChanged(boolean isScanning)
            {
                invalidateOptionsMenu();
            }
        });

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

        scanner.scan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        if (!scanner.isScanning())
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
                scanner.scan();
                break;

            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    private class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>{
        public ArrayList<ScanResult> scanResults;
        private LayoutInflater inflater;

        class DeviceViewHolder extends RecyclerView.ViewHolder
        {
            View device;

            DeviceViewHolder(View device) {
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

        void clear()
        {
            scanResults.clear();
        }

        DevicesAdapter(ArrayList<ScanResult> devices)
        {
            scanResults = devices;
            inflater = ScanActivity.this.getLayoutInflater();
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = inflater.inflate(R.layout.listitem_device, parent, false);

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
