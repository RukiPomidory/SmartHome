package com.freshwind.smarthome;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
            getSupportActionBar().hide();
            Log.w(TAG, "getSupportActionBar() OK");

        final Intent main = new Intent(this, MainActivity.class);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                startActivity(main);
                finish();
            }
        }, 500);
    }
}
