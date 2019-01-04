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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

public class ConnectingActivity extends AppCompatActivity
{
    public static final String EXTRAS_DEVICE = "KETTLE";
    private static final String TAG = "CONNECT";

    private Kettle kettle;
    private BluetoothLeService BLEService;

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
        final Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRAS_DEVICE, kettle);
        startActivity(intent);
        finish();
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
                    BluetoothGattCharacteristic charTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
                    BluetoothGattCharacteristic charRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

                    if(charTX != null && charRX != null)
                    {
                        // Посылаем букву A
                        charTX.setValue(new byte[] {0x41});
                        BLEService.writeCharacteristic(charTX);
                        BLEService.setCharacteristicNotification(charRX, true);

                        return;
                    }
                }
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                if ('A' == data[0])
                {
                    startMain();
                }
            }
        }
    };

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
//        BLEService.disconnect();

        BLEService = null;
    }
}
