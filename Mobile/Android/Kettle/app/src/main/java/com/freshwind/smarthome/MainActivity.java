package com.freshwind.smarthome;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.freshwind.smarthome.ConnectingActivity.EXTRAS_DEVICE;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "CHOOSING";

    private TableLayout table;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        inflater = getLayoutInflater();
        table = findViewById(R.id.devices_table);
        FloatingActionButton addButton = findViewById(R.id.add_button);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent scan = new Intent(MainActivity.this, ScanActivity.class);
                startActivity(scan);
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        try
        {
            String dirName = getFilesDir().getAbsolutePath();
            File dir = new File(dirName);

            File[] files = dir.listFiles();
            Log.d(TAG, "item list in app directory:");
            for (File file : files)
            {
                Log.d(TAG, file.getName());

                if (file.isDirectory())
                {
                    continue;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(file.getName())));

                Kettle device = new Kettle();
                device.selfApConfiguration = new WifiConfiguration();
                device.routerConfiguration = new WifiConfiguration();

                device.name = reader.readLine();
                device.connection = Kettle.Connection.valueOf(reader.readLine());
                device.selfApConfiguration.SSID = reader.readLine();
                device.selfApConfiguration.BSSID = reader.readLine();
                device.selfIP = reader.readLine();
                device.localNetIP = reader.readLine();
                device.model = reader.readLine();
                device.selfKey = reader.readLine();
                if (null != device.selfKey && !device.selfKey.equals("null"))
                {
                    device.selfApConfiguration.preSharedKey = device.selfKey;
                }
                else
                {
                    device.selfApConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                device.routerConfiguration.SSID = reader.readLine();
                device.routerConfiguration.BSSID = reader.readLine();
                device.setRouterKey(reader.readLine());
                if (null != device.getRouterKey())
                {
                    device.routerConfiguration.preSharedKey = "\"" + device.getRouterKey() + "\"";
                }

                reader.close();

                add(device);
            }
        }
        catch (IOException | NullPointerException exc)
        {
            exc.printStackTrace();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        table.removeAllViews();
    }

    @SuppressLint("InflateParams")
    private void add(final Kettle device)
    {
        int count = table.getChildCount();
        TableRow row = (TableRow) table.getChildAt(count - 1);
        if (row == null || row.getChildCount() != 1)
        {
            row = (TableRow) inflater.inflate(R.layout.devices_table_row, null);
            table.addView(row);
        }

        View deviceView = inflater.inflate(R.layout.device, null);

        TextView name = deviceView.findViewById(R.id.device_title);
        TextView ip = deviceView.findViewById(R.id.device_ip);

        name.setText(device.selfApConfiguration.SSID);
        if (Kettle.Connection.router == device.connection)
        {
            ip.setText(device.localNetIP);
        }
        else
        {
            ip.setText(device.selfIP);
        }

        deviceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // Проверяем состояние Wi-Fi
                final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                assert manager != null;

                CoordinatorLayout coordinator = findViewById(R.id.coordinator);
                if (WifiManager.WIFI_STATE_DISABLED == manager.getWifiState())
                {
                    Snackbar
                            .make(coordinator, "Wi-Fi выключен", Snackbar.LENGTH_LONG)
                            .setAction("включить", new View.OnClickListener() {
                                @Override
                                public void onClick(View v)
                                {
                                    manager.setWifiEnabled(true);
                                }
                            })
                            .show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, ConnectingActivity.class);
                intent.putExtra(EXTRAS_DEVICE, device);
                startActivity(intent);
            }
        });

        row.addView(deviceView);
    }
}
