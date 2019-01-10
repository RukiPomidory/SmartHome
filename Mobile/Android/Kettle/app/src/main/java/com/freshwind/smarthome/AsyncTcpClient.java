package com.freshwind.smarthome;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class AsyncTcpClient extends AsyncTask<Void, String, Void>
{
    private static final String TAG = AsyncTcpClient.class.getSimpleName();

    private String message;
    private String IP;
    private int port;
    private int tryCount = 5;
    private int state;
    private boolean running = false;

    private Runnable task;
    private PrintWriter bufferOut;
    private BufferedReader bufferIn;
    private OnStateChanged stateListener = null;

    public static final int DISCONNECTED = 0;
    public static final int CONNECTED = 1;

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
    }

    public void stopClient()
    {
        running = false;

        if (bufferOut != null)
        {
            bufferOut.flush();
            bufferOut.close();
            bufferOut = null;
        }

        bufferIn = null;
        message = null;
        stateListener = null;
    }

    public void sendMessage(final String message)
    {
        if (bufferOut != null)
        {
            Log.d(TAG, "Sending: \"" + message + "\"");
            bufferOut.println(message);
            bufferOut.flush();
        }
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


            try (Socket socket = new Socket(serverIP, port))
            {
                Log.d(TAG, "connected!");
                if (stateListener != null)
                {
                    stateListener.stateChanged(CONNECTED);
                }
                state = CONNECTED;

                // Используется для отправки данных на сервер
                bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                // С помощью него получаем данные от сервера
                bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Пока клиент работает, прослушиваем сервер
                while (running)
                {
                    // Читаем строку и публикуем ее, чтобы она появилась в onProgressUpdate(...);
                    message = bufferIn.readLine();
                    if (message != null)
                    {
                        publishProgress(message);
                    }
                }

                Log.d(TAG, "Received Message: \"" + message + "\"");

            }
            catch (Exception exc)
            {
                Log.e(TAG, exc.getMessage());
            }

        }
        catch(Exception exc)
        {
            Log.e(TAG, exc.getMessage());
        }

        return null;
    }

    public interface OnStateChanged
    {
        public void stateChanged(int state);
    }
}
