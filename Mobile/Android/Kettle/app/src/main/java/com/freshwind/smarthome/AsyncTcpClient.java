package com.freshwind.smarthome;

import android.os.AsyncTask;
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
import java.util.Arrays;

public class AsyncTcpClient extends AsyncTask<Void, Integer, Void>
{
    private static final String TAG = AsyncTcpClient.class.getSimpleName();

    private String IP;
    private int port;
    private int tryCount = 5;
    private int state;
    private boolean running = false;

    private Runnable task;
    private PrintWriter bufferOut;
    private OutputStream rawOutputStream;
    private BufferedReader bufferIn;
    private OnStateChanged stateListener = null;

    public static final int DISCONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int UNREACHABLE_NET = 2;

    public void setPreTask(Runnable task)
    {
        this.task = task;
    }

    public void setOnStateChangedListener(OnStateChanged listener)
    {
        stateListener = listener;
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

    public interface OnStateChanged
    {
        void stateChanged(int state);
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
