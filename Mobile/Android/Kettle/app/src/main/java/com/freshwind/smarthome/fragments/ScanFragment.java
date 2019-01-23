package com.freshwind.smarthome.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.freshwind.smarthome.R;
import com.freshwind.smarthome.RecyclerClickListener;
import com.freshwind.smarthome.WifiScanner;

import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends Fragment
{
    private static final String TAG = "ScanFragment";

    private int defaultColor;
    private int selectedColor;
    private View.OnClickListener acceptListener;
    private View.OnClickListener refreshListener;
    private View root;
    private View prevSelectedItem;
    private Button accept;
    private Button refresh;
    private EditText passwordView;
    private Activity activity;
    private WifiScanner scanner;
    private DevicesAdapter adapter;
    private RecyclerView recyclerView;
    private WifiConfiguration config;
    private View refreshProgress;

    public void setAcceptListener(View.OnClickListener listener)
    {
        this.acceptListener = listener;
        if (accept != null)
        {
            accept.setOnClickListener(listener);
        }
    }

    public void setRefreshListener(View.OnClickListener listener)
    {
        this.refreshListener = listener;
        if (refresh != null)
        {
            refresh.setOnClickListener(listener);
        }
    }

    public WifiConfiguration getConfig()
    {
        String password = passwordView.getText().toString();
        config.preSharedKey = '\"' + password + '\"';
        return config;
    }

    public String getPassword()
    {
        return passwordView.getText().toString();
    }

    public View getRoot()
    {
        return root;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.scan_fragment, null);
        activity = getActivity();
        passwordView = root.findViewById(R.id.scan_fragment_password);

        accept = root.findViewById(R.id.acceptRouterBtn);
        if (acceptListener != null)
        {
            accept.setOnClickListener(acceptListener);
        }

        recyclerView = root.findViewById(R.id.scanRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new DevicesAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerClickListener(activity) {
            @Override
            public void onItemClick(RecyclerView recyclerView, View itemView, int position)
            {
                if (prevSelectedItem != null)
                {
                    prevSelectedItem.setBackgroundColor(defaultColor);
                }
                itemView.setBackgroundColor(selectedColor);
                prevSelectedItem = itemView;

                config = new WifiConfiguration();
                ScanResult result = adapter.scanResults.get(position);

                config.SSID = "\"" + result.SSID + "\"";
                config.BSSID = "\"" + result.BSSID + "\"";

                adapter.setSelectedItem(position);
            }
        });

        final WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        scanner = new WifiScanner(activity);
        scanner.setOnResultListener(new WifiScanner.ScanResultListener() {
            @Override
            public void onScanSuccess()
            {
                List<ScanResult> results = wifiManager.getScanResults();
                refreshProgress.setVisibility(View.INVISIBLE);
                adapter.updateDevices(results);
            }

            @Override
            public void onScanFailure()
            {
                refreshProgress.setVisibility(View.INVISIBLE);
                Log.w(TAG, "scan FAILED");
            }
        });

        refreshProgress = root.findViewById(R.id.refresh_progressbar);

//        scanner.scan();

        refresh = root.findViewById(R.id.fragment_refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                scanner.scan();
                refreshProgress.setVisibility(View.VISIBLE);
            }
        });

        defaultColor = getResources().getColor(R.color.neutral_500);
        selectedColor = getResources().getColor(R.color.redAccent_300);

        return root;
    }

    // TODO: адаптер можно пихнуть как public класс для обоих сканеров
    private class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>{
        public ArrayList<ScanResult> scanResults;

        private LayoutInflater inflater;
        private int selectedPosition = -1;

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

        void setSelectedItem(int position)
        {
            selectedPosition = position;
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
        public void onBindViewHolder(DeviceViewHolder holder, int position)
        {
            View view = holder.device;

            TextView nameTextView = view.findViewById(R.id.device_name);
            TextView addressTextView = view.findViewById(R.id.device_address);

            ScanResult result = scanResults.get(position);

            String name = result.SSID;
            if (null != name && name.length() > 0)
            {
                nameTextView.setText(name);
            }
            else
            {
                nameTextView.setText(R.string.unknown_device);
            }

            if (position == selectedPosition)
            {
                view.setBackgroundColor(selectedColor);
            }
            else
            {
                view.setBackgroundColor(defaultColor);
            }

            addressTextView.setText(result.BSSID);
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
