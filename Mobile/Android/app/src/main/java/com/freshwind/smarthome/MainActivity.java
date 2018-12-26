package com.freshwind.smarthome;

import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button connectBtn;

    BluetoothAdapter btAdapter;
    BluetoothSocket btSocket;

    // MAC-адрес Bluetooth модуля
    private static String address = "A8:1B:6A:75:9E:17";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        //checkBTState();

        connectBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view)
            {

            }
        });

        sendData("");
    }

    private void sendData(String message)
    {
        byte[] msgBuffer = message.getBytes();

        Log.d("TAG?", "...Посылаем данные: " + message + "...");

        try
        {
            outStream.write(msgBuffer);
        }
        catch (IOException e)
        {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();

            errorExit("Fatal Error", msg);
        }
    }
}
