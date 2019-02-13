package com.freshwind.smarthome;

import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Kettle implements Parcelable
{
    private static final String TAG = "Kettle";
    private static final String defaultSelfIP = "192.168.42.1";
    private static final int defaultPort = 3333;

    public static final int DISCONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int UNREACHABLE_NET = 2;

    public String getRouterKey()
    {
        return routerKey;
    }

    public void setRouterKey(String routerKey)
    {
        this.routerKey = routerKey;
    }

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

    // Собственная конфигурация Wi-Fi чайника
    public WifiConfiguration selfApConfiguration;

    // Wi-Fi конфигурация роутера, к которому автоматически подключается чайник
    public WifiConfiguration routerConfiguration;

    // Пароль своей точки доступа
    public String selfKey;

    // Пароль роутера
    private String routerKey;

    // Метод соединения с телефоном
    public Connection connection = Connection.disconnected;


    // Клиент TCP сервера
    private AsyncTcpClient tcpClient;

    // Буфер для хранения полученных от сервера данных
    private ArrayList<Byte> buffer;

    // То, что выполнится асинхронно перед подключением к серверу
    private Runnable preTask;

    // Обработчик изменения состояния клиента
    private OnStateChanged onStateChangedListener;

    // Обработчик данных с сервера
    private OnDataReceived onDataReceivedListener;


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

        selfApConfiguration = in.readParcelable(WifiConfiguration.class.getClassLoader());
        routerConfiguration = in.readParcelable(WifiConfiguration.class.getClassLoader());
        connection = Connection.valueOf(in.readString());

        selfKey = in.readString();
        routerKey = in.readString();
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
        onDataReceivedListener = null;
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
        onDataReceivedListener = listener;
        if (tcpClient != null)
        {
            tcpClient.setDataReceivedListener(listener);
        }
    }

    public void setOnStateChangedListener(OnStateChanged listener)
    {
        onStateChangedListener = listener;
    }


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

        tcpClient = new AsyncTcpClient(ip, port);
        tcpClient.setPreTask(preTask);
        tcpClient.setOnStateChangedListener(onStateChangedListener);
        tcpClient.setDataReceivedListener(onDataReceivedListener);
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

        dest.writeParcelable(selfApConfiguration, flags);
        dest.writeParcelable(routerConfiguration, flags);
        dest.writeString(connection.name());

        dest.writeString(selfKey);
        dest.writeString(routerKey);
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

    public interface OnStateChanged
    {
        void stateChanged(int state);
    }

    private static class AsyncTcpClient extends AsyncTask<Void, Integer, Void>
    {
        private static final String TAG = AsyncTcpClient.class.getSimpleName();

        private String IP;
        private int port;
        private int tryCount = 5;
        private int state;
        private boolean running = false;

        // Буфер для хранения полученных от сервера данных
        private ArrayList<Byte> buffer;

        private Runnable task;
        private PrintWriter bufferOut;
        private OutputStream rawOutputStream;
        private BufferedReader bufferIn;
        private OnStateChanged stateListener = null;
        private OnDataReceived dataReceivedListener = null;

        public void setPreTask(Runnable task)
        {
            this.task = task;
        }

        public void setOnStateChangedListener(OnStateChanged listener)
        {
            stateListener = listener;
        }

        public void setDataReceivedListener(OnDataReceived listener)
        {
            dataReceivedListener = listener;
        }

        public AsyncTcpClient(String IP, int port)
        {
            this.IP = IP;
            this.port = port;
            state = DISCONNECTED;
        }

        public void stop()
        {
            running = false;

            if (bufferOut != null)
            {
                bufferOut.flush();
                bufferOut.close();
                bufferOut = null;
            }

            bufferIn = null;
            stateListener = null;
        }

        public void sendString(final String message)
        {
            Runnable sending = new Runnable() {
                @Override
                public void run() {
                    if (bufferOut != null)
                    {
                        Log.d(TAG, "Sending: \"" + message + "\"");
                        bufferOut.println(message);
                        bufferOut.flush();
                    }
                }
            };
            Thread thread = new Thread(sending);
            thread.start();
        }

        public void sendBytes(final byte[] data)
        {
            Runnable sending = new Runnable() {
                @Override
                public void run() {
                    if (rawOutputStream != null)
                    {
                        Log.d(TAG, "Sending: \"" + Arrays.toString(data) + "\"");
                        try
                        {
                            rawOutputStream.write(data);
                            rawOutputStream.flush();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread thread = new Thread(sending);
            thread.start();
        }

        @Override
        protected Void doInBackground(Void... none)
        {
            running = true;

            try
            {
                if (task != null)
                {
                    task.run();
                }

                if (!running)
                {
                    return null;
                }

                InetAddress serverIP = InetAddress.getByName(IP);

                Socket socket = null;
                int i = 0;
                while(running)
                {
                    i++;
                    Log.d(TAG, "Попытка " + String.valueOf(i));
                    socket = createSocket(serverIP, port);
                    if (socket != null)
                    {
                        break;
                    }

                    Thread.sleep(100);
                    if (i > 5)
                    {
                        stateListener.stateChanged(UNREACHABLE_NET);
                        return null;
                    }
                }

                Log.d(TAG, "Сеть найдена, сокет готов!");

                try
                {
                    // TODO buffer здесь излишен. Упростить.
                    // Используется для отправки данных на сервер
                    bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    rawOutputStream = socket.getOutputStream();

                    // С помощью него получаем данные от сервера
                    bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    Log.d(TAG, "connected!");
                    state = CONNECTED;
                    if (stateListener != null)
                    {
                        stateListener.stateChanged(CONNECTED);
                    }

                    // Пока клиент работает, прослушиваем сервер
                    while (running)
                    {
                        // Читаем байт и публикуем его, чтобы он появился в onProgressUpdate(...);
                        int data = bufferIn.read();
                        publishProgress(data);
                    }
                }
                catch (Exception exc)
                {
                    exc.printStackTrace();
                }
                finally
                {
                    if(stateListener != null)
                    {
                        stateListener.stateChanged(DISCONNECTED);
                    }
                }
            }
            catch(Exception exc)
            {
                Log.e(TAG + " (out)", exc.getMessage());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            if (buffer == null)
            {
                buffer = new ArrayList<>();
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

                if (dataReceivedListener != null)
                {
                    dataReceivedListener.dataReceived(buffer);
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

        private Socket createSocket(InetAddress ip, int port) throws IOException
        {
            Socket socket;

            try
            {
                socket = new Socket(ip, port);
            }
            catch(SocketException exc)
            {
                Log.e(TAG, "Неудачная попытка создать сокет");
                socket = null;
            }

            return socket;
        }
    }
}
