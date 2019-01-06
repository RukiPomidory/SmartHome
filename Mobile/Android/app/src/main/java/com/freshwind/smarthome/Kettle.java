package com.freshwind.smarthome;

import android.os.Parcel;
import android.os.Parcelable;

public class Kettle implements Parcelable
{
    public String MAC;
    public String name;
    public String model;
    public int temperature;
    public int waterLevel;
    public byte state;

    protected Kettle(Parcel in)
    {
        MAC = in.readString();
        name = in.readString();
        model = in.readString();
        temperature = in.readInt();
        waterLevel = in.readInt();
        state = in.readByte();
    }

    public Kettle()
    {

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
        dest.writeString(model);
        dest.writeInt(temperature);
        dest.writeInt(waterLevel);
        dest.writeByte(state);
    }
}
