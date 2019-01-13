package com.freshwind.smarthome;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class GetRouterInfoFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.get_router_info, null);
        Button done = root.findViewById(R.id.router_done);
        EditText ssid = root.findViewById(R.id.router_ssid);
        EditText password = root.findViewById(R.id.router_password);

        return root;
    }
}
