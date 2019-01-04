package com.freshwind.smarthome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Parcel;
import android.os.Parcelable;

public class Kettle implements Parcelable
{
    public String MAC;
    public String name;

    protected Kettle(Parcel in)
    {
        MAC = in.readString();
        name = in.readString();
    }

    public Kettle(String name, String MAC)
    {
        this.name = name;
        this.MAC = MAC;
    }

    public Kettle(CharSequence name, CharSequence MAC)
    {
        this(name.toString(), MAC.toString());
    }

    public Kettle(String MAC)
    {
        this("Unnamed", MAC);
    }

    public static final Creator<Kettle> CREATOR = new Creator<Kettle>() {
        @Override
        public Kettle createFromParcel(Parcel in) {
            return new Kettle(in);
        }

        @Override
        public Kettle[] newArray(int size) {
            return new Kettle[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(MAC);
        dest.writeString(name);
    }
}
