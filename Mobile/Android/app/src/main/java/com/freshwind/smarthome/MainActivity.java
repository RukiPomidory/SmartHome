package com.freshwind.smarthome;

import android.content.Intent;
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

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

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
                add(new Kettle("name", "MAC"));
                add(new Kettle("another name", "MAC"));
                add(new Kettle("user name", "MAC"));
                add(new Kettle("Letov alive", "MAC"));
                add(new Kettle("what it is?", "MAC"));
                add(new Kettle("extra", "MAC"));
                add(new Kettle("seventh", "MAC"));
//                Intent scan = new Intent(MainActivity.this, ScanActivity.class);
//
//                table.removeAllViews();
//                startActivity(scan);
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
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    continue;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(file.getName())));

                Kettle device = new Kettle();

                device.name = reader.readLine();
                device.MAC = reader.readLine();
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

    private void display(List<Kettle> devices)
    {
        for (int i = 0; i + 1 < devices.size(); i += 2)
        {
            TableRow row = (TableRow) inflater.inflate(R.layout.devices_table_row, null);
            View leftDevice = inflater.inflate(R.layout.device, null);
            View rightDevice = inflater.inflate(R.layout.device, null);

            TextView leftName = leftDevice.findViewById(R.id.device_title);
            leftName.setText(devices.get(i).name);

            TextView rightName = rightDevice.findViewById(R.id.device_title);
            rightName.setText(devices.get(i + 1).name);

            row.addView(leftDevice);
            row.addView(rightDevice);

            table.addView(row);
        }

        if (devices.size() % 2 != 0)
        {
            TableRow row = (TableRow) inflater.inflate(R.layout.devices_table_row, null);
            View device = inflater.inflate(R.layout.device, row);

            TextView name = device.findViewById(R.id.device_title);
            name.setText(devices.get(devices.size() - 1).name);

            table.addView(row);
        }
    }

    private void add(Kettle device)
    {
        int count = table.getChildCount();
        TableRow row = (TableRow) table.getChildAt(count - 1);
        if (row != null && row.getChildCount() == 1)
        {
            View deviceView = inflater.inflate(R.layout.device, null);

            TextView name = deviceView.findViewById(R.id.device_title);
            name.setText(device.name);

            row.addView(deviceView);
        }
        else
        {
            TableRow newRow = (TableRow) inflater.inflate(R.layout.devices_table_row, null);
            View deviceView = inflater.inflate(R.layout.device, null);

            TextView name = deviceView.findViewById(R.id.device_title);
            name.setText(device.name);

            newRow.addView(deviceView);

            table.addView(newRow);
        }

    }

}
