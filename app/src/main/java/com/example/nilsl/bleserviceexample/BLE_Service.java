package com.example.nilsl.bleserviceexample;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BLE_Service extends Service {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic WriteCharateristic;
    BluetoothGattCharacteristic ReadCharateristic;
    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    private Handler mMainActivityHandler;
    private Handler mMotionActivityHandler;
    private Handler mScanHandler;
    private boolean ConnectionState;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int DEVICELIST = 1;
    private final static int CHANGEDCHARACTERISTIC = 2;
    private final static int BTAdapterState = 0;
    private final static int CONNECTIONSTATECHANGED = 3;
    private final static int SERVICESDISCOVERED =4;
    private final static int SCANNINGSTOPPED =5;

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;

    private final IBinder mBleBinder = new BleServiceBinder();

    public void onCreate(){

        super.onCreate();
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!btAdapter.isEnabled()) {
                btScanner = null;
                btAdapter.enable();
                // Bluetooth is not enable :)
            }

            btScanner = btAdapter.getBluetoothLeScanner();
        }

        ConnectionState = false;
    }
    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String messageText = ("Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
            final Message msg = new Message();
            final Bundle bundle = new Bundle();
            final BleCommands bleCommand = new BleCommands(DEVICELIST);
            devicesDiscovered.add(result.getDevice());
            deviceIndex++;
            bleCommand.setBleDeviceInfo(messageText);
            bundle.putParcelable("COMMAND", bleCommand);
            msg.setData(bundle);
            mMainActivityHandler.sendMessage(msg);
        }

    };

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final Message msg = new Message();
            final Bundle bundle = new Bundle();
            final byte[] dataInput = characteristic.getValue();
            System.out.println("Charcteristic changed ");
            BleCommands bleCommand = new BleCommands(CHANGEDCHARACTERISTIC);
            bleCommand.setReadMessage(dataInput);
            bundle.putParcelable("COMMAND", bleCommand);
            msg.setData(bundle);
            mMainActivityHandler.sendMessage(msg);
        }


        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            final Message msg = new Message();
            final Bundle bundle = new Bundle();
            BleCommands bleCommand = new BleCommands(CONNECTIONSTATECHANGED);

            // this will get called when a device connects or disconnects
            System.out.println(newState);
            switch (newState) {
                case 0:
                    bleCommand.setConnectionState(0);
                    ConnectionState = false;
                    break;
                case 2:
                    bleCommand.setConnectionState(2);
                    ConnectionState = true;
                    // discover services and characteristics for this device
                    bluetoothGatt.discoverServices();
                    break;
                default:
                    bleCommand.setConnectionState(-1);
                    break;
            }
            bundle.putParcelable("COMMAND", bleCommand);
            msg.setData(bundle);
            mMainActivityHandler.sendMessage(msg);
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            displayGattServices(bluetoothGatt.getServices());
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] BleMessage;
                final Message msg = new Message();
                final Bundle bundle = new Bundle();
                System.out.println("Charcteristic read ");
                //bluetoothGatt.readCharacteristic(ReadCharateristic);
                BleMessage = characteristic.getValue();
                System.out.println(BleMessage.toString());
                //BleCommands bleCommand = new BleCommands(CHANGEDCHARACTERISTIC);
                //bundle.putParcelable("COMMAND", bleCommand);
                //msg.setData(bundle);
                //mMainActivityHandler.sendMessage(msg);
            }
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        return mBleBinder;
    }
    public class BleServiceBinder extends Binder {
        public void setMainActivityCallbackHandler(final Handler callback){
            mMainActivityHandler = callback;
        }
        public void setMotionActivityCallbackHandler(final Handler callback){
            mMotionActivityHandler = callback;
        }

        public void startScanning(){
            serviceStartScanning();
        }
        public void stopScanning(){
            serviceStopScanning();
        }
        public void disconnectFromDevice(){
            serviceDisconnectFromDevice();
        }
        public void connectToDevice(int deviceSelected){
            serviceConnectToDevice(deviceSelected);
        }
        public void getState(){
            serviceGetState();
        }
        public boolean getConnectionState(){
            boolean state;
            state = getServiceConnectionState();
            return state;
        }

    }
    private boolean getServiceConnectionState(){
        return this.ConnectionState;
    }

    private void serviceGetState(){
        final Message msg = new Message();
        final Bundle bundle = new Bundle();
        BleCommands bleCommand = new BleCommands(BTAdapterState);
        if (btAdapter != null && !btAdapter.isEnabled()) {
            //Notify Main Activity that BT Adapter is enabled
            bleCommand.setBtAdaperState(true);
        }
        else{
            bleCommand.setBtAdaperState(false);
        }
        bundle.putParcelable("COMMAND",bleCommand);
        msg.setData(bundle);
        mMainActivityHandler.sendMessage(msg);
    }

    private void serviceDisconnectFromDevice(){
        bluetoothGatt.disconnect();
    }
    private void serviceConnectToDevice(int deviceSelected){
        bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
    }

    private void serviceStartScanning() {
        System.out.println("start scanning");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                serviceStopScanning();
            }
        }, SCAN_PERIOD);
    }

    private void serviceStopScanning(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
        btScanning = false;
        final Message msg = new Message();
        final Bundle bundle = new Bundle();
        BleCommands bleCommand = new BleCommands(SCANNINGSTOPPED);
        bundle.putParcelable("COMMAND",bleCommand);
        msg.setData(bundle);
        mMainActivityHandler.sendMessage(msg);

    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        List<String> services = new ArrayList<>();
        List<String> characteristics = new ArrayList<>();
        List<String> properties = new ArrayList<>();
        List<Integer> serviceCount = new ArrayList<>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            services.add("Service discovered: " + uuid+"\n");


            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            int count =0;
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                final int CharacteristicProperties = gattCharacteristic.getProperties();
                BluetoothGattDescriptor CharacteristicDescriptor = gattCharacteristic.getDescriptor(gattCharacteristic.getUuid());
                count++;
                if  (((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                        (gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                    WriteCharateristic = gattCharacteristic;
                    gattCharacteristic.setValue("start".getBytes());
                    gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    bluetoothGatt.writeCharacteristic(gattCharacteristic);
                    boolean retVal =  bluetoothGatt.writeCharacteristic(gattCharacteristic);
                    if(retVal) {
                        System.out.println("Transmission Successful");

                    }
                }
                else if(gattCharacteristic.getProperties() == 16){

                    ReadCharateristic = gattCharacteristic;
                    bluetoothGatt.readCharacteristic(ReadCharateristic);
                    bluetoothGatt.setCharacteristicNotification(ReadCharateristic,true);
                                   }

                System.out.println("Characteristic discovered for service: " + charUuid);
                System.out.println("Properties discovered for Characteristic: " + CharacteristicProperties);
                characteristics.add("Characteristic discovered for service: "+charUuid+"\n");
                properties.add("Properties discovered for characteristic: "+ CharacteristicProperties+"\n");
            }
            serviceCount.add(count);
        }

        final Message msg = new Message();
        final Bundle bundle = new Bundle();
        BleCommands bleCommand = new BleCommands(SERVICESDISCOVERED);
        bleCommand.setServicesInfo(services,characteristics,properties,serviceCount);
        bundle.putParcelable("COMMAND",bleCommand);
        msg.setData(bundle);
        mMainActivityHandler.sendMessage(msg);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        System.out.println(characteristic.getUuid());
    }

}
