package com.freshwind.smarthome;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient
{
    public static final String TAG = TcpClient.class.getSimpleName();
    private String IP;
    private int port;

    private String message;
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    public TcpClient(String IP, int port, OnMessageReceived listener)
    {
        this.IP = IP;
        this.port = port;
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient()
    {
        mRun = false;

        if (mBufferOut != null)
        {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        message = null;
    }

    public void run()
    {
        mRun = true;

        try
        {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(IP);

            Log.d("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, port);

            try
            {
                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                //in this while the client listens for the messages sent by the server
                while (mRun)
                {
                    Log.d(TAG, String.valueOf(mBufferIn.ready()));
                    //message = mBufferIn.readLine();

                    int _byte = mBufferIn.read();

                    if (mMessageListener != null)
                    {
                        //call the method messageReceived from MyActivity class
                        //mMessageListener.messageReceived(message);
                        mMessageListener.byteReceived(_byte);
                    }
                }

                Log.d("RESPONSE FROM SERVER", "S: Received Message: '" + message + "'");

            }
            catch (Exception e)
            {
                Log.e("TCP", "S: Error", e);
            }
            finally
            {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        }
        catch (Exception e)
        {
            Log.e("TCP", "C: Error", e);
        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived
    {
        public void byteReceived(int data);
    }
}

