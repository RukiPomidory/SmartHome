package com.freshwind.smarthome;


import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
    private static final int delay = 100;
    private static final int attemptCount = 5;

    private int attempt;

    private Kettle kettle;
    private AsyncTcpClient tcpClient;
    private ArrayList<Byte> receivedData;
    private WifiManager wifiManager;
    private TextView description;
    private GetRouterInfoFragment infoFragment;
    private Runnable preTask = new Runnable() {
        @Override
        public void run()
        {
            // Подключение к точке доступа
            int networkId = wifiManager.addNetwork(kettle.configuration);
            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            wifiManager.reconnect();
            WifiInfo info = wifiManager.getConnectionInfo();

            attempt = 0;
            while(info.getNetworkId() == -1)
            {
                attempt++;
                if (attempt > attemptCount)
                {
                    // TODO уведомление о неудаче и прекращение процесса подключения
                    return;
                }

                description.post(new Runnable() {
                    @Override
                    public void run()
                    {
                        String text = getString(R.string.default_attempt_text) + String.valueOf(attempt);
                        description.setText(text);
                    }
                });

                try { Thread.sleep(1000); }
                catch (InterruptedException ignored) { }

                info = wifiManager.getConnectionInfo();
            }

            description.post(new Runnable() {
                @Override
                public void run()
                {
                    description.setText(R.string.ap_connected);
                }
            });

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    };


    // TODO реализация в классе Kettle!!!
    private boolean[] checkList;
    // P.S. любое упоминание этого объекта не имеет права
    // расцениваться как адекватный, правильный или некривой код.
    // ЭТО ВРЕМЕННОЕ РЕШЕНИЕ


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

        if (Kettle.Connection.selfAp == kettle.connection)
        {
            startTcpClient();
        }
        else
        {
            routerConnection();
        }

        // TODO: Инкапсулировать функционал в Kettle вместо этого дерьма
        // Список того, что нужно запросить у чайника.
        // Данные иногда теряются и нам не нужно запрашивать
        // повторно то, что мы уже получили.
        checkList = new boolean[] {false, false, false};
    }

    @SuppressLint("StaticFieldLeak")
    private void startTcpClient()
    {
        tcpClient = new AsyncTcpClient(kettle.selfIP, kettle.port)
        {
            @Override
            protected void onProgressUpdate(Integer... values)
            {
                super.onProgressUpdate(values);
                //response received from server
                Log.d(TAG, "response " + values[0]);
                char _byte = (char) (int) values[0];
                if (';' == _byte && receivedData.size() > 0)
                {
                    receiveData(receivedData);
                    Log.d(TAG, "received: " + receivedData);
                    receivedData.clear();
                }
                else
                {
                    receivedData.add((byte) _byte);
                }
            }
        };
        tcpClient.setPreTask(preTask);
        tcpClient.setOnStateChangedListener(new AsyncTcpClient.OnStateChanged() {
            @Override
            public void stateChanged(int state)
            {
                switch(state)
                {
                    case AsyncTcpClient.CONNECTED:
                        description.post(new Runnable() {
                            @Override
                            public void run()
                            {
                                description.setText("Успешное соединение с сервером!");
                            }
                        });

                        request();
                        break;
                }
            }
        });
        tcpClient.execute();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(tcpClient != null)
        {
            tcpClient.stopClient();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
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
            tcpClient.sendBytes(data);
        }
        catch (Exception exc)
        {
            Log.d(TAG, "dataSend failed");
        }
    }

    private void request()
    {
        Runnable checking = new Runnable() {
            @Override
            public void run()
            {
                int need = check();
                while(need > 0)
                {
                    final int finalNeed = need;
                    description.post(new Runnable() {
                        @Override
                        public void run()
                        {
                            String text = "Опрашиваю датчики...\nНе хватает: " + String.valueOf(finalNeed);
                            description.setText(text);
                        }
                    });

                    try { Thread.sleep(delay); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                    need = check();
                }

                startMain();
            }
        };

        Thread thread = new Thread(checking);
        thread.start();
    }

    private int check()
    {
        int need = 0;

        if (!checkList[0])
        {
            sendData(new byte[] {'R', 5});
            need++;
        }
        if (!checkList[1])
        {
            sendData(new byte[] {'R', 6});
            need++;
        }
        if (!checkList[2])
        {
            sendData(new byte[] {'R', 0});
            need++;
        }

        return need;
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
                        checkList[0] = true;
                        break;

                    case 6:
                        kettle.temperature = data.get(2);
                        checkList[1] = true;
                        break;

                    case 0:
                        kettle.state = data.get(2);
                        checkList[2] = true;
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
                Log.e(TAG, "ОШИБКА распознавания id датчика");
                break;

            case 11:
                Log.e(TAG, "ОШИБКА распознавания команды");
                break;

            default:
                Log.w(TAG, new IllegalStateException());
                break;
        }
    }

    private void routerConnection()
    {


        description.setText("Запрашиваю у пользователя данные...");
        infoFragment = new GetRouterInfoFragment();
        infoFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // TODO: connection

                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.remove(infoFragment);
                transaction.commit();
                description.setText("Подключаю чайник к точке доступа...");
            }
        });

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.connect_frame_layout, infoFragment);
        transaction.commit();
    }
}
