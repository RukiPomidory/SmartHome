package com.freshwind.smarthome;

import android.os.Parcel;
import android.os.Parcelable;

public class Kettle implements Parcelable
{
    // Имя чайника
    public String name;

    // Bluetooth MAC-адрес
    public String MAC;

    // Модель чайника
    public String model;

    // IP-адрес в режиме точки доступа
    public String selfIP;

    // IP-адрес в локальной сети при подключении через роутер
    public String localNetIP;

    // порт сервера
    public int port;

    // Значения с датчиков
    public int temperature;
    public int waterLevel;

    // Состояние - вкл/выкл
    public byte state;

    protected Kettle(Parcel in)
    {
        name = in.readString();
        MAC = in.readString();
        model = in.readString();

        selfIP = in.readString();
        localNetIP = in.readString();
        port = in.readInt();

        temperature = in.readInt();
        waterLevel = in.readInt();
        state = in.readByte();
    }

    public Kettle() { }

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
