package com.freshwind.smarthome;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class SelectConnectionFragment extends Fragment
{
    private OnClickListener listener;

    public void setOnClickListener(OnClickListener listener)
    {
        this.listener = listener;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.select_connection_fragment, null);
        Button selfApBtn = root.findViewById(R.id.select_self_ap_btn);
        Button routerBtn = root.findViewById(R.id.select_router_btn);

        selfApBtn.setOnClickListener(listener);
        routerBtn.setOnClickListener(listener);

        return root;
    }
}
