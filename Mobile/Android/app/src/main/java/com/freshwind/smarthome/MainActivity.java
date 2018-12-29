package com.freshwind.smarthome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.freshwind.smarthome.example_code.BluetoothLeService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bluetooth1";

    Button connectBtn;
    Switch onOffSwitch;

    private BluetoothLeService BLEService;
    private boolean connected = false;
    private BluetoothGattCharacteristic charTX;
    private BluetoothGattCharacteristic charRX;
    private String deviceName;
    private String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        connectBtn = findViewById(R.id.connectBtn);
        onOffSwitch = findViewById(R.id.onOff);


        connectBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view)
            {

            }
        });

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(onOffSwitch.isChecked())
                {
                    sendData("O");
                }
                else
                {
                    sendData("B");
                }
            }
        });

    }

    private void sendData(String message)
    {
        final byte[] data = message.getBytes();
        if(connected)
        {
            charTX.setValue(data);
            BLEService.writeCharacteristic(charTX);
            BLEService.setCharacteristicNotification(charRX, true);
        }
    }

    
}
