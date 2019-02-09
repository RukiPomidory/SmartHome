package com.freshwind.smarthome;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;

import com.freshwind.smarthome.fragments.ConnectionErrorFragment;
import com.freshwind.smarthome.fragments.ElephantFragment;
import com.freshwind.smarthome.fragments.UnableToConnectFragment;

import java.util.ArrayList;
import java.util.List;

import static com.freshwind.smarthome.ConnectingActivity.EXTRAS_DEVICE;

public class DeviceControlActivity extends AppCompatActivity
{
    private static final String TAG = "Main";
    private boolean elephantShown;
    private boolean lowWaterShown;
    private boolean needToHeat; //нужно ли посылать повторные сигналы о включении
    private boolean needToCold; //то же самое с выключением
    private int reconnectTimeout = 2000;
    private float waterLimit = 2.1f;

    private Button launchBtn;
    private CircleProgressBar tempProgressBar;
    private CircleProgressBar waterProgressBar;
    private Handler handler;
    private Runnable getTemperature;
    private Runnable getWaterLevel;
    private Runnable reconnect;
    private Runnable letsHeat;
    private Runnable letsKill;
    private Kettle kettle;
    private Fragment elephantFragment;
    private Fragment connectionErrorFragment;
    private FragmentTransaction transaction;
    private Kettle.OnDataReceived onDataReceivedListener;
    private Kettle.OnStateChanged onStateChangedListener;
    private ImageView temperatureImage;


    private final OnClickListener heatOnClickListener = new OnClickListener() {
        public void onClick(View view)
        {
            needToHeat = true;
            letsHeat.run();
        }
    };

    private final OnClickListener coldOnClickListener = new OnClickListener() {
        public void onClick(View view)
        {
            needToCold = true;
            letsKill.run();
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

        needToHeat = false;
        needToCold = false;

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
                kettle.sendData(new byte[] {0x52, 6});
                handler.postDelayed(this, delayMillis);
            }
        };

        getWaterLevel = new Runnable() {
            @Override
            public void run()
            {
                kettle.sendData(new byte[] {0x52, 5});
                handler.postDelayed(this, delayMillis);
            }
        };

        letsHeat = new Runnable() {
            @Override
            public void run()
            {
                kettle.sendData(new byte[] {'H'});
                if (needToHeat) handler.postDelayed(this, 100);
            }
        };

        letsKill = new Runnable() {
            @Override
            public void run()
            {
                kettle.sendData(new byte[] {'K'});
                if (needToCold) handler.postDelayed(this, 100);
            }
        };

        reconnect = new Runnable() {
            @Override
            public void run()
            {
                // TODO
            }
        };

        handler.postDelayed(getTemperature, delayMillis);
        handler.postDelayed(getWaterLevel, delayMillis / 2);

        elephantFragment = new ElephantFragment();
        connectionErrorFragment = new ConnectionErrorFragment();

        initOnStateChangedListener();
        initOnDataReceivedListener();

        kettle.setOnDataReceivedListener(onDataReceivedListener);
        kettle.setOnStateChangedListener(onStateChangedListener);
        kettle.connectToTcpServer();

        temperatureImage = findViewById(R.id.heatingState);
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
        // Кнопка "назад"
        if (item.getItemId() == android.R.id.home)
        {
            finish();
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
        if (kettle != null)
        {
            kettle.stop();
        }
    }

    private void initOnDataReceivedListener()
    {
        onDataReceivedListener = new Kettle.OnDataReceived() {
            @Override
            public void dataReceived(List<Byte> data)
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
                        needToHeat = false;

                        switch (data.get(1))
                        {
                            case 1:
                                message = "Мало воды!";
                                break;

                            case 2:
                                message = "Слишком много воды!";
                                break;
                        }

                        if (message != null && !lowWaterShown)
                        {
                            Snackbar
                                    .make(launchBtn, message, Snackbar.LENGTH_LONG)
                                    .addCallback(new Snackbar.Callback()
                                    {
                                        @Override
                                        public void onDismissed(Snackbar transientBottomBar, @DismissEvent int event)
                                        {
                                            if (DISMISS_EVENT_TIMEOUT == event)
                                            {
                                                lowWaterShown = false;
                                            }
                                        }
                                    })
                                    .show();
                            lowWaterShown = true;
                        }
                        break;

                    case 'H':
                        assert launchBtn != null;
                        launchBtn.setOnClickListener(coldOnClickListener);
                        launchBtn.setText(R.string.turn_off);
                        temperatureImage.setImageResource(R.drawable.ic_temperature);
                        needToHeat = false;
                        break;

                    case 'K':
                        assert launchBtn != null;
                        launchBtn.setOnClickListener(heatOnClickListener);
                        launchBtn.setText(R.string.launch);
                        temperatureImage.setImageResource(R.drawable.ic_temperature_off);
                        needToCold = false;
                        break;

                    case 'D':
                        if (getLifecycle().getCurrentState() == Lifecycle.State.RESUMED)
                        {
                            Snackbar
                                    .make(launchBtn, "Вода вскипела!", Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        else
                        {

                            Intent resIntent = new Intent(getApplicationContext(), DeviceControlActivity.class);

                            NotificationCompat.Builder builder =
                                new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.ic_temperature)
                                .setContentTitle(kettle.name)
                                .setContentText("Вода вскипела, пора заваривать чай!");

                            NotificationManager manager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            manager.notify(0, builder.build());
                        }

                        launchBtn.setOnClickListener(heatOnClickListener);
                        launchBtn.setText(R.string.launch);
                        temperatureImage.setImageResource(R.drawable.ic_temperature_off);
                        break;
                }
            }
        };
    }

    private void initOnStateChangedListener()
    {
        onStateChangedListener = new Kettle.OnStateChanged() {
            @Override
            public void stateChanged(int state)
            {
                if (Kettle.DISCONNECTED == state)
                {
                    connectionLost();
                }
            }
        };
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
        //reconnect.run();

        waterProgressBar.setProgress(0);
        tempProgressBar.setProgress(0);
    }

    private void connectionReturned()
    {
        transaction = getFragmentManager().beginTransaction();
        transaction.remove(connectionErrorFragment);
        transaction.commit();
    }
}
