package com.freshwind.smarthome.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.freshwind.smarthome.R;

public class UnableToConnectFragment extends Fragment
{
    private Button backBtn;
    private Button connectBtn;
    private OnClickListener backListener;
    private OnClickListener connectListener;

    public void setBackListener(OnClickListener listener)
    {
        backListener = listener;
        if (backBtn != null)
        {
            backBtn.setOnClickListener(listener);
        }
    }

    public void setConnectListener(OnClickListener listener)
    {
        connectListener = listener;
        if (connectBtn != null)
        {
            connectBtn.setOnClickListener(listener);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.unable_to_connect_fragment, null);

        backBtn = root.findViewById(R.id.unable_back_button);
        backBtn.setOnClickListener(backListener);

        connectBtn = root.findViewById(R.id.unable_connect_button);
        connectBtn.setOnClickListener(connectListener);

        return root;
    }
}
