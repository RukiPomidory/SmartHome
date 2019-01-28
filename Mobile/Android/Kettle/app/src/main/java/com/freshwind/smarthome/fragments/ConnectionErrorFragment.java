package com.freshwind.smarthome.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.freshwind.smarthome.R;

public class ConnectionErrorFragment extends Fragment
{
    private TextView description;
    private String text;

    public void setText(String text)
    {
        this.text = text;
        if (description != null)
        {
            description.setText(text);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.connection_error, null);

        description = root.findViewById(R.id.connectErrDescription);
        if (text != null)
        {
            description.setText(text);
        }

        return root;
    }
}
