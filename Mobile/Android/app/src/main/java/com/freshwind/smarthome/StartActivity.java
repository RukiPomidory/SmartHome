package com.freshwind.smarthome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class StartActivity extends AppCompatActivity
{
    private final static String TAG = "LAUNCH";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);

        try
        {
            getActionBar().hide();
            Log.w(TAG, "getActionBar() OK");
        }
        catch (NullPointerException exc)
        {
            Log.w(TAG, "getActionBar() is null");
        }

        try
        {
            getSupportActionBar().hide();
            Log.w(TAG, "getSupportActionBar() OK");
        }
        catch (NullPointerException exc)
        {
            Log.w(TAG, "getSupportActionBar() is null");
        }
    }
}
