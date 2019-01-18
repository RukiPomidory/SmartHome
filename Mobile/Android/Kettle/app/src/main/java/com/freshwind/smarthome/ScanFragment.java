package com.freshwind.smarthome;

import android.app.Fragment;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class ScanFragment extends Fragment
{
    private View.OnClickListener listener;
    private View root;
    private Button accept;
    private EditText password;

    public void setOnclickListener(View.OnClickListener listener)
    {
        this.listener = listener;
        if (accept != null)
        {
            accept.setOnClickListener(listener);
        }
    }

    public String getPassword()
    {
        return password.getText().toString();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.scan_fragment, null);
        password = root.findViewById(R.id.scan_fragment_password);
        accept = root.findViewById(R.id.acceptRouterBtn);
        if (listener != null)
        {
            accept.setOnClickListener(listener);
        }

        return root;
    }
}
