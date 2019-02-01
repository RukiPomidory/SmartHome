package com.freshwind.smarthome.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.freshwind.smarthome.R;

public class EnterNameFragment extends Fragment
{
    private View root;
    private EditText nameView;
    private Button acceptBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.enter_name, null);

        nameView = root.findViewById(R.id.nameView);
        acceptBtn = root.findViewById(R.id.acceptName);

        return root;
    }
}
