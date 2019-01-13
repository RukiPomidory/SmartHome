package com.freshwind.smarthome;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class GetRouterInfoFragment extends Fragment
{
    private View.OnClickListener listener;

    public void setOnClickListener(View.OnClickListener listener)
    {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.get_router_info, null);
        Button done = root.findViewById(R.id.router_done);
        done.setOnClickListener(listener);

//        EditText ssid = root.findViewById(R.id.router_ssid);
//        EditText password = root.findViewById(R.id.router_password);

        return root;
    }
}
