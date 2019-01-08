package com.freshwind.smarthome;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
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

    @Override
    protected void onPause()
    {
        super.onPause();

        table.removeAllViews();
    }

    private void add(Kettle device)
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
        TextView MAC = deviceView.findViewById(R.id.device_mac);

        name.setText(device.name);
        MAC.setText(device.MAC);

        row.addView(deviceView);
    }

}
