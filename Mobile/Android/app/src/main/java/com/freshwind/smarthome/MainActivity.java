package com.freshwind.smarthome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bluetooth1";

    Button connectBtn;
    Switch onOffSwitch;
    TextView inData;

    private BluetoothLeService BLEService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic charTX;
    private BluetoothGattCharacteristic charRX;
    //private String deviceName = "kettle";
    private String deviceMAC = "A8:1B:6A:75:9E:17";


    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            BLEService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!BLEService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            BLEService.connect(deviceMAC);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            BLEService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        connectBtn = findViewById(R.id.connectBtn);
        onOffSwitch = findViewById(R.id.onOff);
        inData = findViewById(R.id.incomingData);


        connectBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view)
            {
                BLEService.connect(deviceMAC);
            }
        });

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(onOffSwitch.isChecked())
                {
                    sendData("K\n");
                    Log.i(TAG ,"turned off");
                }
                else
                {
                    sendData("O\n");
                    Log.i(TAG ,"turned on");
                }
            }
        });


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        BLEService = null;
    }

    private void sendData(String message)
    {
        final byte[] data = message.getBytes();
        sendData(data);
    }

    private void sendData(byte[] data)
    {

        if(mConnected)
        {
            charTX.setValue(data);
            BLEService.writeCharacteristic(charTX);
            BLEService.setCharacteristicNotification(charRX, true);
        }
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                inData.setText(":" + data);

            }
        }
    };
}
