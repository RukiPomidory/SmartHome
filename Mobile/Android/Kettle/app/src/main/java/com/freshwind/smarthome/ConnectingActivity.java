package com.freshwind.smarthome;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

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
    private TcpClient tcpClient;
    private ArrayList<Byte> receivedData;
    private WifiManager wifiManager;
    private Handler handler;
    private TextView description;

    private final static int delay = 100;  // Задержка между посылками сообщений


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect);

        description = findViewById(R.id.processDescription);

        final Intent intent = getIntent();
        kettle = intent.getParcelableExtra(EXTRAS_DEVICE);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(kettle.name);

        receivedData = new ArrayList<>();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager != null;

        new ConnectTask().execute();

//        handler = new Handler();
//        handler.post(new Runnable() {
//            @Override
//            public void run()
//            {
//                connect();
//            }
//        });

    }

    private void connect()
    {
        int networkId = wifiManager.addNetwork(kettle.configuration);

        wifiManager.disconnect();
        wifiManager.enableNetwork(networkId, true);
        wifiManager.reconnect();

        WifiInfo info = wifiManager.getConnectionInfo();

        description.setText(R.string.ap_connected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            if(tcpClient != null)
            {
                tcpClient.stopClient();
            }
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void startMain()
    {
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(EXTRAS_DEVICE, kettle);
        // сохранять будем потом
        //saveDevice(kettle);
        startActivity(intent);
        Log.d(TAG, "LAUNCH DeviceControlActivity");
        finish();
    }

    private void saveDevice(Kettle device)
    {
        try
        {
            String fileName = device.MAC;
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

    private void sendData(byte[] data)
    {
        try
        {
            //charTX.setValue(data);
            //BLEService.writeCharacteristic(charTX);

        }
        catch (Exception exc)
        {
            Log.d(TAG, "dataSend failed");
        }
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


    public class ConnectTask extends AsyncTask<Void, String, Void>
    {
        @Override
        protected Void doInBackground(Void... empty)
        {
            // Подключение к точке доступа
            int networkId = wifiManager.addNetwork(kettle.configuration);
            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            wifiManager.reconnect();
            WifiInfo info = wifiManager.getConnectionInfo();

            int i = 0;
            while(info.getNetworkId() == -1)
            {
                if (i < 20)
                {
                    Log.d(TAG, String.valueOf(i));
                }
                i++;
                info = wifiManager.getConnectionInfo();
            }
            publishProgress(getResources().getString(R.string.ap_connected));

            // Соединение с сервером и получение данных
            tcpClient = new TcpClient(
                kettle.selfIP,
                kettle.port,
                new TcpClient.OnMessageReceived() {
                    @Override
                    public void messageReceived(String message)
                    {
                        publishProgress("", message);
                    }
            });
            tcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            super.onProgressUpdate(values);
            //response received from server
            Log.d(TAG, "response " + values[0]);

            if(values.length > 1)
            {
                char[] data = values[1].toCharArray();
                for (char _byte : data)
                {
                    if (';' == _byte && receivedData.size() > 0)
                    {
                        receiveData(receivedData);
                        receivedData.clear();
                    }
                    else
                    {
                        receivedData.add((byte) _byte);
                    }
                }
            }

            if (values[0] != null && !values[0].equals(""))
            {
                description.setText(values[0]);
            }
        }
    }
}
