package com.freshwind.smarthome;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
    private AsyncTcpClient tcpClient;


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


    @SuppressLint("StaticFieldLeak")
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
                // TODO сделать реконнект
            }
        };

        handler.postDelayed(getTemperature, delayMillis);
        handler.postDelayed(getWaterLevel, delayMillis / 2);

        elephantFragment = new ElephantFragment();
        connectionErrorFragment = new ConnectionErrorFragment();

        receivedData = new ArrayList<>();

        tcpClient = new AsyncTcpClient(kettle.selfIP, kettle.port) {
            @Override
            protected void onProgressUpdate(Integer... values)
            {
                super.onProgressUpdate(values);
                //response received from server
                Log.d(TAG, "response " + values[0]);
                char _byte = (char) (int) values[0];
                if (';' == _byte && receivedData.size() > 0)
                {
                    // Код символа ';' = 59, поэтому показание с датчика, равное 59
                    // может быть ошибочно принято за разделитель. Отсекаем этот случай.
                    if ('T' == receivedData.get(0) && 2 == receivedData.size())
                    {
                        receivedData.add((byte) _byte);
                        return;
                    }

                    processInputData(receivedData);
                    Log.d(TAG, "received: " + receivedData);
                    receivedData.clear();
                }
                else
                {
                    receivedData.add((byte) _byte);
                }
            }
        };
        tcpClient.execute();
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
    protected void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacks(getTemperature);
        handler.removeCallbacks(getWaterLevel);
        if (tcpClient != null)
        {
            tcpClient.stopClient();
        }
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
            tcpClient.sendBytes(data);
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
    }

    @Override
    protected void onPause()
    {
        super.onPause();
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
                        checkElephant(waterLevel);
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

    private void checkElephant(byte waterLevel)
    {
        if (waterLevel > waterLimit * 10)
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
    }
}
