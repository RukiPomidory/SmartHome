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
    private static final String TAG = TcpClient.class.getSimpleName();

    private String message;
    private String IP;
    private int port;
    private boolean running = false;

    private Runnable task;
    private PrintWriter bufferOut;
    private BufferedReader bufferIn;

    public void setPreTask(Runnable task)
    {
        this.task = task;
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
                //sends the message to the server
                bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                //in this while the client listens for the messages sent by the server
                while (running)
                {
                    //message = mBufferIn.readLine();

                    //int _byte = mBufferIn.read();
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
}
