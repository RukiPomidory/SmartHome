package com.freshwind.smarthome;

import android.annotation.SuppressLint;
import android.net.wifi.WifiConfiguration;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Kettle implements Parcelable
{
    private static final String TAG = "Kettle";

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

    // Имя чайника
    public String name;

    // Bluetooth MAC-адрес
    public String MAC;

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

    // TODO: наследование
    protected Kettle(Parcel in)
    {
        this();

        name = in.readString();
        MAC = in.readString();
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
    }

    public Kettle(String name, String MAC)
    {
        this.name = name;
        this.MAC = MAC;
    }

    public Kettle(CharSequence name, CharSequence MAC)
    {
        this(name.toString(), MAC.toString());
    }

    public void stop()
    {
        if(tcpClient != null)
        {
            tcpClient.stop();
        }
    }

    public void setSelfIP(String ip)
    {
        selfIP = ip;
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
    public void connectToTcpServer(Connection type)
    {
        String ip;
        if (Connection.router == type)
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
                super.onProgressUpdate(values);
                char _byte = (char) (int) values[0];
                if (';' == _byte && buffer.size() > 0)
                {
                    // Хрен знает, почему я решил так сделать
//                    try
//                    {
//                        ArrayList<Byte> data = (ArrayList<Byte>) buffer.clone();
//                        if (listener != null)
//                        {
//                            listener.dataReceived(data);
//                        }
//                        buffer.clear();
//                    }
//                    catch (ClassCastException exc)
//                    {
//                        Log.e(TAG, "impossible cast exception");
//                    }

                    if (listener != null)
                    {
                        listener.dataReceived(buffer);
                    }
                    buffer.clear();
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
        dest.writeString(MAC);
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
