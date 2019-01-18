package com.freshwind.smarthome;

import android.app.Fragment;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class ScanFragment extends Fragment
{
    private View.OnClickListener listener;
    private View root;
    private Button accept;

    public void setOnclickListener(View.OnClickListener listener)
    {
        this.listener = listener;
        if (accept != null)
        {
            accept.setOnClickListener(listener);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.scan_fragment, null);
        accept = root.findViewById(R.id.acceptRouterBtn);
        if (listener != null)
        {
            accept.setOnClickListener(listener);
        }

        return root;
    }
}
