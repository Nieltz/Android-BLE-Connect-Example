package com.example.nilsl.bleserviceexample;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button startScanningButton;
    Button stopScanningButton;
    Button startCursorControlButton;
    Button startOrientationControlButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private final static int BTAdapterState = 0;
    private final static int DEVICELIST = 1;
    private final static int CHANGEDCHARACTERISTIC = 2;
    private final static int CONNECTIONSTATECHANGED = 3;
    private final static int SERVICESDISCOVERED =4;
    private final static int SCANNINGSTOPPED =5;
    private BLE_Service.BleServiceBinder mBinder;
    private boolean connectionState;
    private String filename;
    private boolean fileCountValid;


    private final Handler mMainActivityHandler = new Handler(){
        public void handleMessage(Message msg){
            final Bundle bundle =msg.getData();
            BleCommands bleCommand = bundle.getParcelable("COMMAND");
            int command = bleCommand.getCommand();

            switch(command){
                case BTAdapterState:
                    break;
                case DEVICELIST:
                    String deviceList = bleCommand.getBleDeviceInfo();
                    handleDevieList(deviceList);
                    break;
                case CHANGEDCHARACTERISTIC:
                    // this will get called anytime a characteristic was updated and you registered for it
                    final byte[] readData = bleCommand.getReadMessage();
                    final int count =0;


                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            File path = getPublicDocumentsStorageDir("XdkLogs");
                            if (!fileCountValid){
                                File[] files = path.listFiles();
                                final int count = files.length+1;
                                filename  = "Log"+count+".txt";
                                fileCountValid=true;
                            }
                            File file = new File(path, filename);
                            if(isExternalStorageWritable()){
                                FileOutputStream outputStream;
                                String dataToWrite = getStringFromBytes(readData);
                                try {
                                    outputStream =  new FileOutputStream(file, true);
                                    outputStream.write(dataToWrite.getBytes());
                                    outputStream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();

                                }
                            }
                            peripheralTextView.append("device read or wrote to\n");
                        }
                    });
                    break;
                case CONNECTIONSTATECHANGED:
                    int connectionState = bleCommand.getConnectionState();
                    onConnectionStateChange(connectionState);
                    break;
                case SCANNINGSTOPPED:
                    stopScanningView();
                    break;
                case SERVICESDISCOVERED:
                    List<String> characteristics;
                    characteristics = bleCommand.getCharacteristics();
                    displayGattServices(characteristics);
                    break;
            }


        }
    };

    private ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ((BLE_Service.BleServiceBinder) binder).setMainActivityCallbackHandler(mMainActivityHandler);
            mBinder =  (BLE_Service.BleServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    EditText deviceIndexInput;
    Button connectToDevice;
    Button disconnectDevice;


    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String, String> uuids = new HashMap<String, String>();

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Intent bleIntent = new Intent(this, BLE_Service.class);
        bindService(bleIntent,mBleServiceConnection,Context.BIND_AUTO_CREATE);

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = (EditText) findViewById(R.id.InputIndex);
        deviceIndexInput.setText("0");

        connectToDevice = (Button) findViewById(R.id.ConnectButton);
        connectToDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectToDeviceSelected();
            }
        });

        disconnectDevice = (Button) findViewById(R.id.DisconnectButton);
        disconnectDevice.setVisibility(View.INVISIBLE);
        disconnectDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnectDeviceSelected();
            }
        });

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        this.verifyStoragePermissions(this);

        this.fileCountValid =false;

              // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();

        }

    }

    private void handleDevieList(String deviceInfo){
        peripheralTextView.append(deviceInfo);
        final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0) {
            peripheralTextView.scrollTo(0, scrollAmount);
        }

    }

    private void onConnectionStateChange(final int newState) {
        // this will get called when a device connects or disconnects
        System.out.println(newState);
        switch (newState) {
            case 0:
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.setText("");
                        peripheralTextView.append("device disconnected\n");
                        disconnectDevice.setVisibility(View.INVISIBLE);
                        connectToDevice.setVisibility(View.VISIBLE);

                    }
                });
                break;
            case 2:
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.setText("");
                        peripheralTextView.append("device connected\n");
                        connectToDevice.setVisibility(View.INVISIBLE);
                        disconnectDevice.setVisibility(View.VISIBLE);
                    }
                });

                break;
            default:
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append("we encounterned an unknown state, uh oh\n");
                    }
                });
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {

        System.out.println("start scanning");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        peripheralTextView.setText("");
        peripheralTextView.append("Started Scanning\n");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        mBinder.startScanning();

    }

    public void stopScanningView() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning\n");
        btScanning = false;
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
    }

    public void stopScanning() {
        mBinder.stopScanning();
    }

    public void connectToDeviceSelected() {
        peripheralTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        mBinder.connectToDevice(deviceSelected);
    }

    public void disconnectDeviceSelected() {
        peripheralTextView.append("Disconnecting from device\n");
        mBinder.disconnectFromDevice();
    }
    private void displayGattServices(final List<String> characteristics) {
        int i;
        int j;
        int offset;
        int count;
        // Loops through available GATT Services.
        offset = 0;
        while (offset<characteristics.size()){
            final String service = characteristics.get(offset);
            count=Integer.parseInt(characteristics.get(offset+1));
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append(service);
                }
            });
            offset+=2;
            for (j = 0; j < count; j++) {
                final String characteristic = characteristics.get(offset);
                offset++;
                final String property = characteristics.get(offset);
                offset++;
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append(characteristic);
                        peripheralTextView.append(property);
                    }
                });
            }


        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }



    public File getPublicDocumentsStorageDir(String fileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName);
        if (!file.mkdirs()) {
            //Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }

    public String getStringFromBytes(byte[] readData){
        String output;
        int lightVal;
        int temp, pressure, humidity;
        int type = readData[0];
        byte [] bytes = new byte[4];

        if (type ==0){
            bytes[0] = readData[7];
            bytes[1] = readData[6];
            bytes[2] = readData[5];
            bytes[3] = readData[4];
            lightVal = byteArrayToInt(bytes);
            output = "Light: " + lightVal +"\r\n";
        }
        else{
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


            output = "Environment: Temp:" + temp +", Pressure: " + pressure +", Humidity: " + humidity+"\r\n";
        }

        return output;
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

       @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();


    }

    @Override
    public void onRestart(){
        super.onRestart();
        this.connectionState = mBinder.getConnectionState();
        if (this.connectionState){
            peripheralTextView.append("device connected\n");
            connectToDevice.setVisibility(View.INVISIBLE);
            disconnectDevice.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
