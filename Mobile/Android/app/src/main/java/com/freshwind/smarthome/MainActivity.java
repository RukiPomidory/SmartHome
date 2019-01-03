package com.freshwind.smarthome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bluetooth1";

    private Button launchBtn;
    private CircleProgressBar progressBar;
    private Handler handler;

    private BluetoothLeService BLEService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic charTX;
    private BluetoothGattCharacteristic charRX;
    //private String deviceName = "kettle";
    //private String deviceMAC = "64:CC:2E:B7:08:B3";
    private String deviceMAC = "A8:1B:6A:75:9E:17";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


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

    private final OnClickListener heatOnClickListener = new OnClickListener() {
        public void onClick(View view)
        {
            // heating
            sendData("H");

            assert launchBtn != null;
            launchBtn.setOnClickListener(coldOnClickListener);
            launchBtn.setText(R.string.turn_off);
        }
    };

    private final OnClickListener coldOnClickListener = new OnClickListener() {
        public void onClick(View view)
        {
            // kill
            sendData("K");

            assert launchBtn != null;
            launchBtn.setOnClickListener(heatOnClickListener);
            launchBtn.setText(R.string.launch);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        launchBtn = findViewById(R.id.launchBtn);
        launchBtn.setOnClickListener(heatOnClickListener);

        progressBar = findViewById(R.id.custom_progressBar);
        progressBar.setOnValueChangeListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                progressBar.mainText = String.valueOf(newVal) + '°';
            }
        });

        handler = new Handler();
        final int delayMillis = 1000;
        Runnable getTemperature = new Runnable() {
            @Override
            public void run()
            {
                sendData(new byte[] {0x52 , 4});
                handler.postDelayed(this, delayMillis);
            }
        };

        handler.postDelayed(getTemperature, delayMillis);

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
        try
        {
            if(mConnected)
            {
                charTX.setValue(data);
                BLEService.writeCharacteristic(charTX);
                BLEService.setCharacteristicNotification(charRX, true);
            }
        }
        catch (Exception exc)
        {
            Log.d(TAG, exc.getMessage());
        }
    }


    // Обрабатывает различные события bluetooth сервиса
    // ACTION_GATT_CONNECTED: подключение к GATT серверу.
    // ACTION_GATT_DISCONNECTED: отключение от GATT сервера.
    // ACTION_GATT_SERVICES_DISCOVERED: найдены GATT службы.
    // ACTION_DATA_AVAILABLE: Получены данные с устройства.
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
                displayGattServices(BLEService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                processInputData(data);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (BLEService != null) {
            final boolean result = BLEService.connect(deviceMAC);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            // get characteristic when UUID matches RX/TX UUID
            Log.d(TAG, "in displayGattServices");
            charTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            charRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }

    }

    /**
     * Обрабатывает полученные по bluetooth данные
     * @param data данные
     */
    private void processInputData(byte[] data)
    {
        char command = (char) data[0];
        StringBuilder sb = new StringBuilder();
        sb.append(command);
        for (int i = 1; i < data.length; i++)
        {
            sb.append(data[i]);
        }

        switch (command)
        {
            case 'T':
                if(data[2] != 0)
                {
                    throw new AssertionError();
                }

                if (4 == data[1])
                {
                    byte temperature = data[3];
                    progressBar.setProgress(temperature);
                }
                break;
        }
    }
}
