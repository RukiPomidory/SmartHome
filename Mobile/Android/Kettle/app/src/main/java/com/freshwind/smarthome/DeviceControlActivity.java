package com.freshwind.smarthome;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;

import java.util.ArrayList;
import java.util.List;

import static com.freshwind.smarthome.ConnectingActivity.EXTRAS_DEVICE;

public class DeviceControlActivity extends AppCompatActivity
{
    private static final String TAG = "Main";
    private boolean mConnected = false;
    private boolean elephantShown;
    private int reconnectTimeout = 2000;
    private float waterLimit = 2.1f;

    private ArrayList<Byte> receivedData;
    private Button launchBtn;
    private CircleProgressBar tempProgressBar;
    private CircleProgressBar waterProgressBar;
    private Handler handler;
    private Runnable getTemperature;
    private Runnable getWaterLevel;
    private Runnable reconnect;
    private Kettle kettle;
    private Fragment elephantFragment;
    private Fragment connectionErrorFragment;
    private FragmentTransaction transaction;

    private BluetoothLeService BLEService;
    private BluetoothGattCharacteristic character;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            BLEService = ((BluetoothLeService.LocalBinder) service).getService();

            mConnected = true;

            displayGattServices(BLEService.getSupportedGattServices());
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
        setContentView(R.layout.device_control);

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

        reconnect = new Runnable() {
            @Override
            public void run()
            {
                if (BLEService != null)
                {
                    BLEService.connect(kettle.MAC);
                }

                if(!mConnected)
                {
                    handler.postDelayed(this, reconnectTimeout);
                }
            }
        };

        handler.postDelayed(getTemperature, delayMillis);
        handler.postDelayed(getWaterLevel, delayMillis / 2);

        elephantFragment = new ElephantFragment();
        connectionErrorFragment = new ConnectionErrorFragment();

        receivedData = new ArrayList<>();

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
                character.setValue(data);
                boolean result = BLEService.writeCharacteristic(character);
                Log.d(TAG, "char written: " + result);
//                if (!result)
//                {
//                    BLEService.disconnect();
//                }
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
                connectionReturned();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                mConnected = false;
                connectionLost();
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

                for (byte _byte : data)
                {
                    if (';' == _byte && receivedData.size() > 0)
                    {
                        processInputData(receivedData);
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
        if (gattServices == null)
        {
            return;
        }

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            // get characteristic when UUID matches RX/TX UUID
            //Log.d("in displayGattServices", gattService.getUuid().toString());
            character = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

            if(character != null)
            {
                BLEService.setCharacteristicNotification(character, true);
                return;
            }
        }

    }

    /**
     * Обрабатывает полученные по bluetooth данные
     * @param data данные
     */
    private void processInputData(List<Byte> data)
    {
        char command = (char) (byte) data.get(0);

        switch (command)
        {
            case 'T':

                switch (data.get(1))
                {
                    case 5:
                        byte waterLevel = data.get(2);
                        waterProgressBar.setProgress(waterLevel);

                        if (waterLevel / 10.0 > waterLimit)
                        {
                            if (!elephantShown)
                            {
                                transaction = getFragmentManager().beginTransaction();
                                transaction.add(R.id.fragmentLayout, elephantFragment);
                                transaction.commit();
                                elephantShown = true;
                            }
                        }
                        else
                        {
                            if (elephantShown)
                            {
                                transaction = getFragmentManager().beginTransaction();
                                transaction.remove(elephantFragment);
                                transaction.commit();
                                elephantShown = false;
                            }
                        }

                        break;

                    case 6:
                        byte temperature = data.get(2);
                        tempProgressBar.setProgress(temperature);

                        break;
                }
                break;

            case 'E':
                String message = null;

                switch (data.get(1))
                {
                    case 1:
                        message = "Мало воды!";
                        break;

                    case 2:
                        message = "Слишком много воды!";
                        break;
                }

                if (message != null)
                {
                    Snackbar
                            .make(launchBtn, message, Snackbar.LENGTH_LONG)
                            .show();
                }

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

    private void connectionLost()
    {
        transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentLayout, connectionErrorFragment);
        transaction.commit();
        reconnect.run();
    }

    private void connectionReturned()
    {
        transaction = getFragmentManager().beginTransaction();
        transaction.remove(connectionErrorFragment);
        transaction.commit();

        displayGattServices(BLEService.getSupportedGattServices());
    }
}
