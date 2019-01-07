package com.freshwind.smarthome;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class ConnectingActivity extends AppCompatActivity
{
    public static final String EXTRAS_DEVICE = "KETTLE";
    private static final String TAG = "CONNECT";

    private Kettle kettle;
    private BluetoothLeService BLEService;
    private BluetoothGattCharacteristic charTX;
    private BluetoothGattCharacteristic charRX;
    private ArrayList<Byte> receivedData;

    private final static int delay = 100;  // Задержка между посылками сообщений

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

            BLEService.connect(kettle.MAC);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            BLEService.disconnect();
            BLEService = null;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect);

        final Intent intent = getIntent();
        kettle = intent.getParcelableExtra(EXTRAS_DEVICE);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(kettle.name);

        receivedData = new ArrayList<>();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home)
        {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private void startMain()
    {
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(EXTRAS_DEVICE, kettle);
        saveDevice(kettle);
        startActivity(intent);
        Log.d(TAG, "LAUNCH DeviceControlActivity");
        finish();
    }

    private void saveDevice(Kettle device)
    {
        try
        {
            String fileName = device.MAC;
//getFilesDir().getName() + '/' + MainActivity.savedDevicesDir + '/' +
            BufferedWriter writer = new BufferedWriter((new OutputStreamWriter(openFileOutput(fileName, MODE_PRIVATE))));

            writer.write(device.name + '\n');
            writer.write(device.MAC + '\n');
            writer.write("in developing\n");

            writer.close();
        }
        catch (IOException | NullPointerException exc)
        {
            exc.printStackTrace();
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                //isConnected = true;
                invalidateOptionsMenu();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                //isConnected = false;
                invalidateOptionsMenu();
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                List<BluetoothGattService> gattServices = BLEService.getSupportedGattServices();

                for (BluetoothGattService gattService : gattServices) {
                    // get characteristic when UUID matches RX/TX UUID
                    charTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
                    charRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

                    if(charTX != null && charRX != null)
                    {
                        BLEService.setCharacteristicNotification(charTX, true);
                        request();

                        return;
                    }
                }
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                for (byte _byte : data)
                {
                    if (';' == _byte && receivedData.size() > 0)
                    {
                        receiveData(receivedData);
                        receivedData.clear();
                    }
                    else
                    {
                        receivedData.add(_byte);
                    }
                }
            }
        }
    };

    private void sendData(byte[] data)
    {
        try
        {
            charTX.setValue(data);
            BLEService.writeCharacteristic(charTX);

        }
        catch (Exception exc)
        {
            Log.d(TAG, "dataSend failed");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (BLEService != null)
        {
            final boolean result = BLEService.connect(kettle.MAC);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public static IntentFilter makeGattUpdateIntentFilter() {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
//        if (BLEService != null)
//        {
//            BLEService.disconnect();
//        }
//
//        BLEService = null;
    }

    private void request()
    {
        Handler handler = new Handler();

        sendData(new byte[] {'R', 5});

        handler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                sendData(new byte[] {'R', 6});
            }
        }, delay);

        handler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                sendData(new byte[] {'R', 0});
            }
        }, delay * 2);
    }

    private void receiveData(List<Byte> data)
    {
        try
        {
            if ('T' == data.get(0))
            {
                switch (data.get(1))
                {
                    case 5:
                        kettle.waterLevel = data.get(2);
                        break;

                    case 6:
                        kettle.temperature = data.get(2);
                        break;

                    case 0:
                        kettle.state = data.get(2);
                        startMain();
                        break;
                }
            }
            else if ('E' == data.get(0))
            {
                Error(data.get(1));
            }
        }
        catch (IndexOutOfBoundsException exc)
        {
            exc.printStackTrace();
        }
    }

    private void Error(byte code)
    {
        switch(code)
        {
            case 10:
                Log.w(TAG, "ОШИБКА распознавания id датчика");
                break;

            case 11:
                Log.w(TAG, "ОШИБКА распознавания команды");
                break;

            default:
                Log.w(TAG, new IllegalStateException());
                break;
        }
    }
}
