package com.freshwind.smarthome;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import java.util.Arrays;

public class WifiScanner
{
    private static final String TAG = "WifiScanner";

    private boolean isScanning = false;

    private View recyclerView;
    private Activity activity;
    private WifiManager wifiManager;
    private ScanResultListener resultListener;
    private ScanStateListener stateListener;
    private ScanAvailableListener availableListener;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
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
                resultListener.onScanSuccess();
            }
            else
            {
                resultListener.onScanFailure();
            }

            isScanning = false;
            if (stateListener != null)
            {
                stateListener.onScanStateChanged(false);
            }
        }
    };

    public WifiScanner(Activity activity)
    {
        this.activity = activity;
        wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        activity.getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);
    }

    public void setViewForTestFeature(View view)
    {
        recyclerView = view;
    }

    public void setOnStateChangedListener(ScanStateListener listener)
    {
        stateListener = listener;
    }

    public void setOnResultListener(ScanResultListener listener)
    {
        resultListener = listener;
    }

    public void setAvailableListener(ScanAvailableListener listener)
    {
        availableListener = listener;
    }

    public boolean isScanning()
    {
        return isScanning;
    }

    private boolean checkPermissions()
    {
        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED)
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                //Toast.makeText(this, "Не удалось выполнить поиск устройств", Toast.LENGTH_SHORT).show();
                activity.requestPermissions(new String[] {Manifest.permission.CHANGE_WIFI_STATE}, 0);
            }
            return false;
        }

        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                //Toast.makeText(this, "Не удалось выполнить поиск устройств", Toast.LENGTH_SHORT).show();
                activity.requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

            }
            return false;
        }
        return true;
    }

    public void scan()
    {
        Log.d(TAG, Arrays.toString(wifiManager.getScanResults().toArray()));

        if(!checkPermissions())
        {
            return;
        }

        if (!wifiManager.isWifiEnabled() && availableListener != null)
        {
            availableListener.onWifiDisabled();
            return;
        }

        if(!isGeoEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && availableListener != null)
        {
            availableListener.onGpsRequired();
            return;
        }

        wifiManager.startScan();
        isScanning = true;
        if (stateListener != null)
        {
            stateListener.onScanStateChanged(true);
        }
    }

    private boolean isGeoEnabled()
    {
        LocationManager mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null)
        {
            return false;
        }

        boolean mIsGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean mIsNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return mIsGPSEnabled || mIsNetworkEnabled;
    }

    public interface ScanResultListener
    {
        void onScanSuccess();
        void onScanFailure();
    }

    public interface ScanStateListener
    {
        void onScanStateChanged(boolean isScanning);
    }

    public interface ScanAvailableListener
    {
        void onGpsRequired();
        void onWifiDisabled();
    }
}
