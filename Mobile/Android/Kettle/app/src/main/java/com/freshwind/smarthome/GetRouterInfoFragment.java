package com.freshwind.smarthome;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class GetRouterInfoFragment extends Fragment
{
    private View.OnClickListener listener;
    private View root;
    private boolean hasPassword;

    public void setOnClickListener(View.OnClickListener listener)
    {
        this.listener = listener;
    }

    public boolean hasPassword()
    {
        return hasPassword;
    }

    public View getRoot()
    {
        return root;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.get_router_info, null);
        final Button done = root.findViewById(R.id.router_done);
        final EditText passView = root.findViewById(R.id.router_password);
        final CheckBox checkHasPassword = root.findViewById(R.id.checkHasPassword);

        done.setOnClickListener(listener);
        checkHasPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                hasPassword = !isChecked;
                passView.setEnabled(!isChecked);
                if (isChecked)
                {
                    passView.getText().clear();
                }
            }
        });

        return root;
    }
}
