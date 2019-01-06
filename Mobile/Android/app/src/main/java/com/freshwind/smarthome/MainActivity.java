package com.freshwind.smarthome;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
                Intent scan = new Intent(MainActivity.this, ScanActivity.class);

                startActivity(scan);
            }
        });

        try
        {
            String dirName = getFilesDir().getName() + '/' + savedDevicesDir;
            File dir = new File(dirName);

            File[] files = dir.listFiles();
            for (File file : files)
            {
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

    private void add(Kettle device)
    {
        TableRow row = (TableRow) inflater.inflate(R.layout.devices_table_row, null);
        View deviceView = inflater.inflate(R.layout.devices_table_row, null);

        row.addView(deviceView);

        table.addView(row);
    }
}
