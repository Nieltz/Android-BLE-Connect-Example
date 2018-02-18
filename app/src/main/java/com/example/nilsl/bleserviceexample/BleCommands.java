package com.example.nilsl.bleserviceexample;

import android.os.Parcelable;

/**
 * Created by Nils on 18.02.2018.
 */

import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nils on 04.02.2018.
 */

public class BleCommands implements Parcelable {

    private int Command;
    private String bleDeviceInfo;
    private boolean[] btAdaperState = new boolean[1];

    public List<String> getCharacteristics() {
        return characteristics;
    }

    private List<String> characteristics = new ArrayList<>();

    public int getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(int connectionState) {
        this.connectionState = connectionState;
    }

    private int connectionState;

    public BleCommands(int command) {
        this.Command = command;
    }

    public int getCommand() {
        return Command;
    }

    public static final Parcelable.Creator<BleCommands> CREATOR = new Parcelable.Creator<BleCommands>(){
        @Override
        public BleCommands createFromParcel(final Parcel in) {
            return new BleCommands(in);
        }

        @Override
        public BleCommands[] newArray(int size) {
            return new BleCommands[size];
        }
    } ;

    private BleCommands(Parcel in) {
        readFromParcel(in);
    }
    public String getBleDeviceInfo() {
        return bleDeviceInfo;
    }

    public void setBleDeviceInfo(String bleDeviceInfo) {
        this.bleDeviceInfo = bleDeviceInfo;
    }

    public boolean[] isBtAdaperState() {
        return btAdaperState;
    }

    public void setBtAdaperState(boolean btAdaperState) {
        this.btAdaperState[0] = btAdaperState;
    }

    public void setServicesInfo(  List<String> services,  List<String> characteristics,  List<String> properties,  List<Integer> counts ){
        int i;
        int j;
        int offset =0;
        for (i=0;i<counts.size();i++) {
            this.characteristics.add(services.get(i));
            this.characteristics.add(Integer.toString(counts.get(i)));
            for(j=0; j<counts.get(i);j++) {
                this.characteristics.add(characteristics.get(j + offset));
                this.characteristics.add(properties.get(j + offset));
            }
            offset+=counts.get(i);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.Command);
        dest.writeBooleanArray(this.btAdaperState);
        dest.writeStringList(this.characteristics);
    }
    private void readFromParcel(Parcel in) {
        Command = in.readInt();
        bleDeviceInfo = in.readString();
        in.readBooleanArray(this.btAdaperState);
        in.readStringList(this.characteristics);

    }

}
