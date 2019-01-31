package com.freshwind.smarthome;


import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.freshwind.smarthome.fragments.ConnectionErrorFragment;
import com.freshwind.smarthome.fragments.ScanFragment;
import com.freshwind.smarthome.fragments.SelectConnectionFragment;
import com.freshwind.smarthome.fragments.UnableToConnectFragment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class ConnectingActivity extends AppCompatActivity implements OnClickListener
{
    public static final String EXTRAS_DEVICE = "KETTLE";

    private static final String TAG = "CONNECT";
    private static final int delay = 100;
    private static final int attemptCount = 5;

    private int attempt;

    private Kettle kettle;
    private WifiManager wifiManager;
    private TextView description;
    private ScanFragment scanFragment;
    private SelectConnectionFragment selectConnectionFragment;
    private UnableToConnectFragment unableToConnectFragment;
    private FragmentTransaction transaction;
    private Kettle.OnDataReceived dataReceivedListener;
    private AsyncTcpClient.OnStateChanged onStateChangedListener;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (action != null &&
                    action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
            {
                boolean state = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
                if (!state)
                {
                    // Wi-Fi отключился
                    showFailFragment();
                }
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

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager != null;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);

        start();

        // TODO: Пока что хрен знает, что делать с этим дерьмом
        // Список того, что нужно запросить у чайника.
        // Данные иногда теряются и нам не нужно запрашивать
        // повторно то, что мы уже получили.
        checkList = new boolean[] {false, false, false};
    }

    private void start()
    {
        initOnDataReceivedListener();
        initOnStateChangedListener();

        if (Kettle.Connection.selfAp == kettle.connection)
        {
            startTcpClient(createPreTask(kettle.selfApConfiguration));
        }
        else
        {
            startTcpClient(createPreTask(kettle.routerConfiguration));
        }
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        switch(id)
        {
            case R.id.select_self_ap_btn:
                removeSelectionFragment();
                startMain();
                break;

            case R.id.select_router_btn:
//                kettle.connection = Kettle.Connection.router;
                showInputFragment();
                removeSelectionFragment();
                break;

            case R.id.acceptRouterBtn:
                kettle.routerConfiguration = scanFragment.getConfig();
                kettle.setRouterKey(scanFragment.getPassword());
                removeScanFragment();

                description.setText("Подключаю чайник к точке доступа...");
                kettle.connectToRouter(kettle.routerConfiguration);
                break;

            case R.id.cancelRouterBtn:
                removeScanFragment();
                startMain();
                break;

            case R.id.fragment_refresh:
                // TODO refresh
                throw new IllegalArgumentException();

            case R.id.unable_back_button:
                finish();
                break;

            case R.id.unable_connect_button:
                removeUnableFragment();
                start();
                break;
        }
    }

    private void removeScanFragment()
    {
        transaction = getFragmentManager().beginTransaction();
        transaction.remove(scanFragment);
        transaction.commit();
    }

    private void removeSelectionFragment()
    {
        try
        {
            transaction = getFragmentManager().beginTransaction();
            transaction.remove(selectConnectionFragment);
            transaction.commit();
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
        }
    }

    private void removeUnableFragment()
    {
        transaction = getFragmentManager().beginTransaction();
        transaction.remove(unableToConnectFragment);
        transaction.commit();
    }

    private void showConnectionDialog()
    {
        selectConnectionFragment = new SelectConnectionFragment();
        selectConnectionFragment.setOnClickListener(this);

        transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.router_frame_layout, selectConnectionFragment);
        transaction.commit();
    }

    private void connectSelfToRouter()
    {
        kettle.stop();
        kettle.connection = Kettle.Connection.router;

        int id = wifiManager.addNetwork(kettle.routerConfiguration);

        wifiManager.disconnect();
        wifiManager.enableNetwork(id, true);
        wifiManager.reconnect();

        Runnable preTask = createPreTask(kettle.routerConfiguration);

        kettle.setOnDataReceivedListener(dataReceivedListener);
        kettle.setOnStateChangedListener(onStateChangedListener);
        kettle.setPreTask(preTask);
        kettle.connectToTcpServer();
    }

    @SuppressLint("StaticFieldLeak")
    private void startTcpClient(Runnable preTask)
    {
        kettle.setPreTask(preTask);
        kettle.setOnStateChangedListener(onStateChangedListener);
        kettle.setOnDataReceivedListener(dataReceivedListener);
        kettle.connectToTcpServer();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(kettle != null)
        {
            kettle.stop();
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
        saveDevice(kettle);
        startActivity(intent);
        Log.d(TAG, "LAUNCH DeviceControlActivity");
        finish();
    }

    private void saveDevice(Kettle device)
    {
        String fileName = device.selfApConfiguration.BSSID;
        try (BufferedWriter writer = new BufferedWriter((new OutputStreamWriter(openFileOutput(fileName, MODE_PRIVATE)))))
        {

            writer.write(device.name + '\n');
            writer.write(device.connection.name() + '\n');
            writer.write(device.selfApConfiguration.SSID + '\n');
            writer.write(device.selfApConfiguration.BSSID + '\n');
            writer.write(device.selfIP + '\n');
            writer.write(device.localNetIP + '\n');
            writer.write("in developing\n");
            writer.write(device.selfKey + '\n');
            writer.write(device.routerConfiguration.SSID + '\n');
            writer.write(device.routerConfiguration.BSSID + '\n');
            writer.write(device.getRouterKey() + '\n');
        }
        catch (IOException | NullPointerException exc)
        {
            exc.printStackTrace();
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
                    String text = "Опрашиваю датчики...\nНе хватает: " + String.valueOf(finalNeed);
                    setAsyncDescription(text);

                    try { Thread.sleep(delay); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                    need = check();
                }
                String text = "Чайник готов к работе!";
                setAsyncDescription(text);

                if (Kettle.Connection.selfAp == kettle.connection)
                {
                    showConnectionDialog();
                }
                else
                {
                    startMain();
                }
            }
        };

        Thread thread = new Thread(checking);
        thread.start();
    }

    private void setAsyncDescription(int resource)
    {
        setAsyncDescription(getResources().getString(resource));
    }

    private void setAsyncDescription(final String message)
    {
        description.post(new Runnable() {
            @Override
            public void run()
            {
                description.setText(message);
            }
        });
    }

    private int check()
    {
        int need = 0;

        if (!checkList[0])
        {
            kettle.sendData(new byte[] {'R', 5});
            need++;
        }
        if (!checkList[1])
        {
            kettle.sendData(new byte[] {'R', 6});
            need++;
        }
        if (!checkList[2])
        {
            kettle.sendData(new byte[] {'R', 0});
            need++;
        }

        return need;
    }

    private void initOnDataReceivedListener()
    {
        dataReceivedListener = new Kettle.OnDataReceived() {
            @Override
            public void dataReceived(List<Byte> data)
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
                    else if ('O' == data.get(0))
                    {
                        if ('K' == data.get(1))
                        {
                            String text = "Чайник подключился к точке доступа, ждем адрес...";
                            description.setText(text);
                        }
                    }
                    else if('I' == data.get(0))
                    {
                        if ('P' == data.get(1))
                        {
                            StringBuilder builder = new StringBuilder();

                            for (int i = 2; i < data.size(); i++)
                            {
                                builder.append((char)data.get(i).byteValue());
                            }

                            kettle.connection = Kettle.Connection.router;
                            kettle.localNetIP = builder.toString();
                            kettle.sendData(new byte[] {'O'});

                            String text = "IP чайника получен, подключаемся к роутеру...";
                            description.setText(text);

                            connectSelfToRouter();
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
        };
    }

    private void initOnStateChangedListener()
    {
        onStateChangedListener = new AsyncTcpClient.OnStateChanged() {
            @Override
            public void stateChanged(int state)
            {
                switch(state)
                {
                    case AsyncTcpClient.CONNECTED:
                        setAsyncDescription("Успешное соединение с сервером!");
                        request();
                        break;

                    case AsyncTcpClient.UNREACHABLE_NET:
                        setAsyncDescription("Не удалось подключиться к серверу");
                        kettle.stop();
                        showFailFragment();
                        break;
                }
            }
        };
    }

    private Runnable createPreTask(final WifiConfiguration config)
    {
        return new Runnable() {
            @Override
            public void run()
            {
                // Подключение к точке доступа
                int networkId = wifiManager.addNetwork(config);
                wifiManager.disconnect();
                wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
                wifiManager.enableNetwork(networkId, true);

                final boolean[] using = {false};

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    final ConnectivityManager manager = (ConnectivityManager)
                            getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    NetworkRequest.Builder builder = new NetworkRequest.Builder();
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

                    if (manager != null)
                    {
                        manager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onAvailable(Network network)
                            {
                                super.onAvailable(network);
                                manager.bindProcessToNetwork(null);
                                manager.bindProcessToNetwork(network);
                                manager.unregisterNetworkCallback(this);
                                wifiManager.reconnect();
                                using[0] = true;
                            }

                            @Override
                            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities)
                            {
                                super.onCapabilitiesChanged(network, networkCapabilities);
                            }

                            @Override
                            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties)
                            {
                                super.onLinkPropertiesChanged(network, linkProperties);
                            }
                        });
                    }
                    else
                    {
                        Log.e(TAG, "ConnectivityManager IS NULL");
                        finish();
                        return;
                    }
                }

                WifiInfo info = wifiManager.getConnectionInfo();

                attempt = 0;
                while(info.getNetworkId() == -1)
                {
                    attempt++;
                    if (attempt > attemptCount)
                    {
                        setAsyncDescription("Превышен лимит попыток подключения");
                        showFailFragment();
                        kettle.stop();
                        return;
                    }

                    String text = getString(R.string.default_attempt_text) + String.valueOf(attempt);
                    setAsyncDescription(text);

                    try { Thread.sleep(2000); }
                    catch (InterruptedException ignored) { }

                    info = wifiManager.getConnectionInfo();
                    Log.d(TAG, "[" + String.valueOf(attempt) + "]" + info.getSSID());
                }

                Log.d(TAG, "WiFi SSID: " + info.getSSID());

                setAsyncDescription(R.string.ap_connected);

                while (!using[0])
                {
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
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

            case 14:
                showBadPasswordFragment();
                break;

            default:
                Log.w(TAG, new IllegalStateException());
                break;
        }
    }

    private void showBadPasswordFragment()
    {
        ConnectionErrorFragment fragment = new ConnectionErrorFragment();
        fragment.setText("Не удалось подключиться к роутеру. Проверьте верность пароля и доступность точки доступа.");
        transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.error_frame_layout, fragment);
        transaction.commit();
    }

    private void showInputFragment()
    {
        description.setText("Запрашиваю у пользователя данные...");
        scanFragment = new ScanFragment();

        scanFragment.setAcceptListener(this);
        scanFragment.setCancelListener(this);
        scanFragment.setRefreshListener(this);

        transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.router_frame_layout, scanFragment);
        transaction.commit();
    }

    private void showFailFragment()
    {
        unableToConnectFragment = new UnableToConnectFragment();

        unableToConnectFragment.setBackListener(this);
        unableToConnectFragment.setConnectListener(this);

        transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.error_frame_layout, unableToConnectFragment);
        transaction.commit();
    }
}
