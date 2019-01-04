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
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;

import java.util.List;

import static com.freshwind.smarthome.ConnectingActivity.EXTRAS_DEVICE;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Main";

    private Button launchBtn;
    private CircleProgressBar tempProgressBar;
    private CircleProgressBar waterProgressBar;
    private Handler handler;
    private Runnable getTemperature;
    private Runnable getWaterLevel;
    private Kettle kettle;

    private BluetoothLeService BLEService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic charTX;
    private BluetoothGattCharacteristic charRX;
    //private String deviceMAC = "A8:1B:6A:75:9E:17";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            BLEService = ((BluetoothLeService.LocalBinder) service).getService();

            mConnected = true;

            List<BluetoothGattService> gattServices = BLEService.getSupportedGattServices();

            for (BluetoothGattService gattService : gattServices) {
                // get characteristic when UUID matches RX/TX UUID
                charTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
                charRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

                if(charTX != null && charRX != null)
                {
                    return;
                }
            }
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
        }
    };

    private final OnClickListener coldOnClickListener = new OnClickListener() {
        public void onClick(View view)
        {
            // kill
            sendData("K");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Intent intent = getIntent();
        kettle = intent.getParcelableExtra(EXTRAS_DEVICE);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(kettle.name);

        launchBtn = findViewById(R.id.launchBtn);
        launchBtn.setOnClickListener(heatOnClickListener);

        tempProgressBar = findViewById(R.id.temperatureProgressBar);
        tempProgressBar.setOnValueChangeListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                tempProgressBar.mainText = String.valueOf(newVal) + '°';
            }
        });

        waterProgressBar = findViewById(R.id.waterProgressBar);
        waterProgressBar.setOnValueChangeListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                waterProgressBar.bottomText = String.valueOf(newVal / 10.0) + " л";
            }
        });

        waterProgressBar.setProgress(kettle.waterLevel);
        tempProgressBar.setProgress(kettle.temperature);

        handler = new Handler();
        final int delayMillis = 500;
        getTemperature = new Runnable() {
            @Override
            public void run()
            {
                sendData(new byte[] {0x52, 6});
                handler.postDelayed(this, delayMillis);
            }
        };

        getWaterLevel = new Runnable() {
            @Override
            public void run()
            {
                sendData(new byte[] {0x52, 5});
                handler.postDelayed(this, delayMillis);
            }
        };

        handler.postDelayed(getTemperature, delayMillis);
        handler.postDelayed(getWaterLevel, delayMillis / 2);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.device_control_menu, menu);
        menu.findItem(R.id.options).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home)
        {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        else if (item.getItemId() == R.id.options)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        if (BLEService != null)
        {
            BLEService.disconnect();
            BLEService = null;
        }
        stopService(new Intent(this, BluetoothLeService.class));

        handler.removeCallbacks(getTemperature);
        handler.removeCallbacks(getWaterLevel);
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
            Log.d(TAG, "dataSend failed");
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
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                mConnected = true;
                invalidateOptionsMenu();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                mConnected = false;
                Snackbar
                        .make(launchBtn, "Связь потеряна", Snackbar.LENGTH_LONG)
                        .setAction("Подключить", new OnClickListener() {
                            @Override
                            public void onClick(View v)
                            {
                                BLEService.connect(kettle.MAC);
                            }
                        })
                        .show();
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(BLEService.getSupportedGattServices());
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                //displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                processInputData(data);
            }
        }
    };

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, ConnectingActivity.makeGattUpdateIntentFilter());
        if (BLEService != null)
        {
            final boolean result = BLEService.connect(kettle.MAC);
            Log.d(TAG, "Connect request result=" + result);
        }
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
            //Log.d("in displayGattServices", gattService.getUuid().toString());
            charTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            charRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

            if(charTX != null && charRX != null)
            {
                return;
            }
        }

    }

    /**
     * Обрабатывает полученные по bluetooth данные
     * @param data данные
     */
    private void processInputData(byte[] data)
    {
        char command = (char) data[0];

        switch (command)
        {
            case 'T':
                if(data[2] != 0)
                {
                    throw new AssertionError();
                }

                if (6 == data[1])
                {
                    byte temperature = data[3];
                    tempProgressBar.setProgress(temperature);
                }
                else if (5 == data[1])
                {
                    byte waterLevel = data[3];
                    waterProgressBar.setProgress(waterLevel);
                }
                break;

            case 'E':
                String message;

                if (1 == data[1])
                {
                    message = "Мало воды!";
                }
                else if(2 == data[1])
                {
                    message = "Слишком много воды!";
                }
                else
                {
                    break;
                }
                Snackbar
                        .make(launchBtn, message, Snackbar.LENGTH_LONG)
                        .show();
                break;

            case 'H':
                assert launchBtn != null;
                launchBtn.setOnClickListener(coldOnClickListener);
                launchBtn.setText(R.string.turn_off);
                break;

            case 'K':
                assert launchBtn != null;
                launchBtn.setOnClickListener(heatOnClickListener);
                launchBtn.setText(R.string.launch);
                break;
        }
    }
}
