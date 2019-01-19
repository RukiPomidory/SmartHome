package com.freshwind.smarthome;

import android.annotation.SuppressLint;
import android.net.wifi.WifiConfiguration;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Kettle implements Parcelable
{
    private static final String TAG = "Kettle";
    private static final String defaultSelfIP = "192.168.42.1";
    private static final int defaultPort = 3333;

    public interface OnDataReceived
    {
        void dataReceived(List<Byte> data);
    }

    public enum Connection
    {
        /**
         *  В этом виде смартфон подключен к точке доступа
         *  самого чайника и, следовательно, не может
         *  подключаться к роутеру или использовать мобильный
         *  интернет. Другими словами, смартфон в таком подключении
         *  не способен пользоваться интернетом и общаться с
         *  чайником одновременно.
         */
        selfAp,

        /**
         *  Чайник связан с роутером и общается через него
         *  с смартфоном. Это позволяет смартвону выходить
         *  в интернет и управлять чайником.
         */
        router,

        /**
         *  Подключения к чайнику нет.
         */
        disconnected
    }

    // Пользовательское имя чайника
    public String name;

    // Модель чайника
    public String model;

    // IP-адрес в режиме точки доступа
    public String selfIP;

    // IP-адрес в локальной сети при подключении через роутер
    public String localNetIP;

    // порт сервера
    public int port;

    // Значения с датчиков
    public int temperature;
    public int waterLevel;

    // Состояние - вкл/выкл
    public byte state;

    // Конфигурация Wi-Fi
    public WifiConfiguration configuration;

    // Метод соединения с телефоном
    public Connection connection = Connection.disconnected;


    // Клиент TCP сервера
    private AsyncTcpClient tcpClient;

    // Буфер для хранения полученных от сервера данных
    private ArrayList<Byte> buffer;

    // То, что выполнится асинхронно перед подключением к серверу
    private Runnable preTask;

    // Обработчик изменения состояния клиента
    private AsyncTcpClient.OnStateChanged onStateChangedListener;

    // Обработчик данных с сервера
    private OnDataReceived listener;


    private Kettle(Parcel in)
    {
        this();

        name = in.readString();
        model = in.readString();

        selfIP = in.readString();
        localNetIP = in.readString();
        port = in.readInt();

        temperature = in.readInt();
        waterLevel = in.readInt();
        state = in.readByte();

        configuration = in.readParcelable(WifiConfiguration.class.getClassLoader());
        connection = Connection.valueOf(in.readString());
    }

    public Kettle()
    {
        buffer = new ArrayList<>();
        selfIP = defaultSelfIP;
        port = defaultPort;
    }

    public void stop()
    {
        if(tcpClient != null)
        {
            tcpClient.stop();
        }

        onStateChangedListener = null;
        listener = null;
        if (buffer != null)
        {
            buffer.clear();
            buffer = null;
        }
    }

    public void setPreTask(Runnable task)
    {
        preTask = task;
    }

    public void setOnDataReceivedListener(OnDataReceived listener)
    {
        this.listener = listener;
    }

    public void setOnStateChangedListener(AsyncTcpClient.OnStateChanged listener)
    {
        onStateChangedListener = listener;
    }

    @SuppressLint("StaticFieldLeak")
    public void connectToTcpServer()
    {
        String ip;
        if (Connection.router == connection)
        {
            ip = localNetIP;
        }
        else
        {
            ip = selfIP;
        }

        tcpClient = new AsyncTcpClient(ip, port)
        {
            @Override
            protected void onProgressUpdate(Integer... values)
            {
                if (buffer == null)
                {
                    return;
                }

                super.onProgressUpdate(values);
                char _byte = (char) (int) values[0];
                if (';' == _byte && buffer.size() > 0)
                {
                    // Код символа ';' = 59, поэтому показание с датчика, равное 59
                    // может быть ошибочно принято за разделитель. Отсекаем этот случай.
                    if ('T' == buffer.get(0) && 2 == buffer.size())
                    {
                        buffer.add((byte) _byte);
                        return;
                    }

                    if (listener != null)
                    {
                        listener.dataReceived(buffer);
                    }

                    if (buffer != null)
                    {
                        buffer.clear();
                    }
                }
                else
                {
                    buffer.add((byte) _byte);
                }
            }
        };
        tcpClient.setPreTask(preTask);
        tcpClient.setOnStateChangedListener(onStateChangedListener);
        tcpClient.execute();
    }

    public void connectToRouter(String ssid, String password)
    {
        // TODO: остановть процесс цикличной проверки коннекта

        // TODO: сделать процесс цикличной проверки коннекта, чтобы здесь его останавливать

        int ssidLength = ssid.getBytes().length;
        int passLength = password.getBytes().length;
        String data = "A" + (char)ssidLength + ssid + (char)0 + "" + (char) passLength + password + (char)0;
        sendData(data);

        // TODO: проверка получения данных спустя определенное время
    }

    public void connectToRouter(WifiConfiguration config)
    {
        String ssid = config.SSID;
        ssid = ssid.substring(1, ssid.length() - 1);

        String password = config.preSharedKey;
        password = password.substring(1, password.length() - 1);

        connectToRouter(ssid, password);
    }

    public void sendData(String data)
    {
        try
        {
            tcpClient.sendString(data);
        }
        catch (Exception exc)
        {
            Log.d(TAG, "dataSend failed");
        }
    }

    public void sendData(byte[] data)
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


    // implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name);
        dest.writeString(model);

        dest.writeString(selfIP);
        dest.writeString(localNetIP);
        dest.writeInt(port);

        dest.writeInt(temperature);
        dest.writeInt(waterLevel);
        dest.writeByte(state);

        dest.writeParcelable(configuration, flags);
        dest.writeString(connection.name());
    }

    public static final Creator<Kettle> CREATOR = new Creator<Kettle>() {
        @Override
        public Kettle createFromParcel(Parcel in) {
            return new Kettle(in);
        }

        @Override
        public Kettle[] newArray(int size) {
            return new Kettle[size];
        }
    };
}
