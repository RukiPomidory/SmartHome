package com.freshwind.smarthome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "CHOOSING";
    private final static String savedDevicesDir = "devices";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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
        catch (IOException exc)
        {
            exc.printStackTrace();
        }

    }

    private void add(Kettle device)
    {

    }
}
