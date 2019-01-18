package com.freshwind.smarthome;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends Fragment
{
    private static final String TAG = "ScanFragment";

    private View.OnClickListener listener;
    private View root;
    private Button accept;
    private EditText password;
    private Activity activity;
    private WifiScanner scanner;
    private RecyclerView recyclerView;

    public void setOnclickListener(View.OnClickListener listener)
    {
        this.listener = listener;
        if (accept != null)
        {
            accept.setOnClickListener(listener);
        }
    }

    public String getPassword()
    {
        return password.getText().toString();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.scan_fragment, null);
        activity = getActivity();
        password = root.findViewById(R.id.scan_fragment_password);
        accept = root.findViewById(R.id.acceptRouterBtn);
        if (listener != null)
        {
            accept.setOnClickListener(listener);
        }

        recyclerView = root.findViewById(R.id.scanRecycler);
        final DevicesAdapter adapter = new DevicesAdapter();
        recyclerView.setAdapter(adapter);

        final WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        scanner = new WifiScanner(activity);
        scanner.setOnResultListener(new WifiScanner.ScanResultListener() {
            @Override
            public void onScanSuccess()
            {
                List<ScanResult> results = wifiManager.getScanResults();
                adapter.updateDevices(results);
            }

            @Override
            public void onScanFailure()
            {
                Log.w(TAG, "scan FAILED");
            }
        });

        scanner.scan();
        return root;
    }

    // TODO: адаптер можно пихнуть как public класс для обоих сканеров
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
            notifyDataSetChanged();
        }

        void clear()
        {
            scanResults.clear();
        }

        DevicesAdapter()
        {
            scanResults = new ArrayList<>();
            inflater = activity.getLayoutInflater();
        }

        @Override
        public DevicesAdapter.DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = inflater.inflate(R.layout.listitem_device, parent, false);

            return new DevicesAdapter.DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DevicesAdapter.DeviceViewHolder holder, int position)
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
