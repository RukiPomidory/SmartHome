package com.freshwind.smarthome;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
    public final static String savedDevicesDir = "devices";

    private TableLayout table;
    private LayoutInflater inflater;
    private FloatingActionButton addButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        inflater = getLayoutInflater();
        table = findViewById(R.id.devices_table);
        addButton = findViewById(R.id.add_button);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // for DEBUG
//                add(new Kettle("name", "MAC"));
//                add(new Kettle("another name", "MAC"));
//                add(new Kettle("user name", "MAC"));
//                add(new Kettle("Letov alive", "MAC"));
//                add(new Kettle("what it is?", "MAC"));
//                add(new Kettle("extra", "MAC"));
//                add(new Kettle("seventh", "MAC"));

                Intent scan = new Intent(MainActivity.this, ScanActivity.class);
                startActivity(scan);
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

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
                device.configuration = new WifiConfiguration();

                device.name = reader.readLine();
                device.connection = Kettle.Connection.valueOf(reader.readLine());
                device.configuration.SSID = reader.readLine();
                device.configuration.BSSID = reader.readLine();
                device.selfIP = reader.readLine();
                device.localNetIP = reader.readLine();
                device.model = reader.readLine();

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
    protected void onPause()
    {
        super.onPause();

        table.removeAllViews();
    }

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

        name.setText(device.configuration.SSID);
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
                Intent intent = new Intent(MainActivity.this, ConnectingActivity.class);
                intent.putExtra(EXTRAS_DEVICE, device);
                startActivity(intent);
            }
        });

        row.addView(deviceView);
    }

}
