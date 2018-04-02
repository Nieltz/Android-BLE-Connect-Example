package com.example.nilsl.bleserviceexample;

/**
 * Created by Nils on 02.04.2018.
 */

public class SensorData {
    private int light;
    private int temperature;
    private int pressure;
    private int humidity;

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }



    public SensorData(){
        this.light =0;
        this.temperature=0;
        this.pressure =0;
        this.humidity =0;

    }

    public void setSensorValues(byte[] readData){
        if (readData[0] == 0){
            this.light = getLightVal(readData);
        }
        else if(readData[0] == 1){
            int[] envData = getEnvData(readData);
            this.temperature = envData[0];
            this.pressure = envData[1];
            this.humidity = envData[2];
        }
    }



    public int getLightVal(byte[] readData){
        int lightVal;
        byte [] bytes = new byte[4];
        bytes[0] = readData[7];
        bytes[1] = readData[6];
        bytes[2] = readData[5];
        bytes[3] = readData[4];
        lightVal = byteArrayToInt(bytes);
        return lightVal;
        //output = "Light: " + lightVal +"\r\n";
    }

    public int[] getEnvData(byte[] readData){
        int temp, pressure, humidity;
        int [] outVals = new int[3];
        byte [] bytes = new byte[4];
        bytes[0] = readData[7];
        bytes[1] = readData[6];
        bytes[2] = readData[5];
        bytes[3] = readData[4];
        temp = byteArrayToInt(bytes);
        bytes[0] = readData[11];
        bytes[1] = readData[10];
        bytes[2] = readData[9];
        bytes[3] = readData[8];
        pressure = byteArrayToInt(bytes);
        bytes[0] = readData[15];
        bytes[1] = readData[14];
        bytes[2] = readData[13];
        bytes[3] = readData[12];
        humidity = byteArrayToInt(bytes);
        outVals[0] = temp;
        outVals[1] = pressure;
        outVals[2] = humidity;

        return outVals;
    }

    public static int byteArrayToInt(byte[] b)
    {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

}
