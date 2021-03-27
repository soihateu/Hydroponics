package com.example.hydroponicsapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String HM10_MAC_ADDRESS = "64:69:4E:8A:07:67";

    private static final UUID HM10_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String ERROR_BLUETOOTH_LE_NOT_SUPPORTED = "ERROR: Bluetooth LE is not supported on this device.";
    private static final String ERROR_BLUETOOTH_CONNECTION_FAILED = "ERROR: Failed to connect to HM10 device.";

    private static final String SENSOR_TAG = "SensorTag";

    private static final int TEMPERATURE = 1000;
    private static final int HUMIDITY = 1001;
    private static final int LIGHTING = 1002;

    private BluetoothAdapter btAdapter;
    private BluetoothGatt btGatt;

    private ProgressDialog progressDialog;

    private TextView currentTemperature;
    private TextView currentHumidity;
    private TextView currentLighting;
    private TextView setTemperatureDisplay;
    private TextView setHumidityDisplay;
    private TextView setLightingDisplay;
    private TextView currIR;
    private TextView currUV;

    private Button connectButton;
    private Button addTempButton;
    private Button minusTempButton;
    private Button addHumidityButton;
    private Button minusHumidityButton;
    private Button addLightingButton;
    private Button minusLightingButton;

    private boolean sensorsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get all text views
        connectButton = findViewById(R.id.connect);
        currentTemperature = findViewById(R.id.currTemp);
        currentHumidity = findViewById(R.id.currHumidity);
        currentLighting = findViewById(R.id.currLighting);
        addTempButton = findViewById(R.id.addTemp);
        minusTempButton = findViewById(R.id.decreaseTemp);
        addHumidityButton = findViewById(R.id.addHumidity);
        minusHumidityButton = findViewById(R.id.decreaseHumidity);
        addLightingButton = findViewById(R.id.addLighting);
        minusLightingButton = findViewById(R.id.decreaseLighting);
        setTemperatureDisplay = findViewById(R.id.setTemp);
        setHumidityDisplay = findViewById(R.id.setHumidity);
        setLightingDisplay = findViewById(R.id.setLighting);
        //currIR = findViewById(R.id.currIR);
        //currUV = findViewById(R.id.currUV);

        // Setup progress dialog; shows as loading dialog while connecting to device
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        // Setup buttons
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        addTempButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && getSetValue(TEMPERATURE) < 30) {
                    updateSetValues(TEMPERATURE, true);
                }
            }
        });
        minusTempButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && getSetValue(TEMPERATURE) > 15) {
                    updateSetValues(TEMPERATURE, false);
                }
            }
        });
        addHumidityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && getSetValue(HUMIDITY) < 100) {
                    updateSetValues(HUMIDITY, true);
                }
            }
        });
        minusHumidityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && getSetValue(HUMIDITY) > 0) {
                    updateSetValues(HUMIDITY, false);
                }
            }
        });
        addLightingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && getSetValue(LIGHTING) < 100) {
                    updateSetValues(LIGHTING, true);
                }
            }
        });
        minusLightingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && getSetValue(LIGHTING) > 0) {
                    updateSetValues(LIGHTING, false);
                }
            }
        });

        // Setup Bluetooth
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Validate Bluetooth is enabled; if not enabled, ask user to enable it
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            finish();
            return;
        }

        // Validate Bluetooth capabilities
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, ERROR_BLUETOOTH_LE_NOT_SUPPORTED, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressDialog.dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect from any active Gatt connection
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt = null;
        }
    }

    // Connect to bluetooth device
    private void connect() {
        BluetoothDevice btDevice = btAdapter.getRemoteDevice(HM10_MAC_ADDRESS);

        if (btDevice != null) {
            btGatt = btDevice.connectGatt(this, false, btGattCallback, BluetoothDevice.TRANSPORT_LE);
            handler.sendMessage(Message.obtain(null, MSG_POPUP, "Connecting to " + btDevice.getName() + "..."));
            disableConnectButton();
        }
        else {
            Toast.makeText(this, ERROR_BLUETOOTH_CONNECTION_FAILED, Toast.LENGTH_SHORT).show();
        }
    }

    // Allow the button to be clickable once the application has connected to the bluetooth module
    private void enableConnectButton() {
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
    }

    private void disableConnectButton() {
        connectButton.setText("Connected");
        connectButton.setEnabled(false);
    }

    private void resetApplication() {
        // Reset text
        currentTemperature.setText("-------");
        currentHumidity.setText("-------");
        currentLighting.setText("-------");
        setTemperatureDisplay.setText("-------");
        setHumidityDisplay.setText("-------");
        setLightingDisplay.setText("-------");

        // Reset connection details
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt = null;
        }

        enableConnectButton();
    }

    public final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        // Notify us for any updates/changes on the sensors
        private void subscribeToSensorNotifications(BluetoothGatt gatt) {
            BluetoothGattService btService = gatt.getService(HM10_SERVICE);

            if (btService != null) {
                BluetoothGattCharacteristic characteristic = btService.getCharacteristic(HM10_CHARACTERISTIC);

                if (characteristic != null) {
                    // Enable local notifications
                    if (!gatt.setCharacteristicNotification(characteristic, true)) {
                        handler.sendMessage(Message.obtain(null, MSG_RESET, "Error setting characteristic notifications."));
                    }

                    // Enable remote notifications
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(HM10_CHARACTERISTIC_CONFIG);

                    if (descriptor != null) {
                        if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                            if (!gatt.writeDescriptor(descriptor)) {
                                handler.sendMessage(Message.obtain(null, MSG_RESET, "Error writing to descriptor."));
                            }
                        }
                        else {
                            handler.sendMessage(Message.obtain(null, MSG_RESET, "Error setting descriptor value."));
                        }
                    }
                    else {
                        handler.sendMessage(Message.obtain(null, MSG_RESET, "Error obtaining descriptor."));
                    }
                }
                else {
                    handler.sendMessage(Message.obtain(null, MSG_RESET, "Error obtaining characteristic of service."));
                }
            }
            else {
                handler.sendMessage(Message.obtain(null, MSG_RESET, "Error obtaining service."));
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // Connection was successful
                handler.sendMessage(Message.obtain(null, MSG_POPUP, "Discovering services..."));
                gatt.discoverServices();
            }
            else if (status != BluetoothGatt.GATT_SUCCESS) {
                // Failed to connect
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            handler.sendMessage(Message.obtain(null, MSG_POPUP, "Reading sensors..."));

            // Read and update current values on display
            BluetoothGattService btService = gatt.getService(HM10_SERVICE);

            if (btService != null) {
                BluetoothGattCharacteristic characteristic = btService.getCharacteristic(HM10_CHARACTERISTIC);
            }
            else {
                handler.sendMessage(Message.obtain(null, MSG_RESET, "Failed to obtain service."));
                return;
            }

            subscribeToSensorNotifications(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Will handle all future updates on sensor value changes
            // Pass data read to Activity to update text fields
            if (HM10_CHARACTERISTIC.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_UPDATE_VALUES, characteristic));
            }
        }
    };

    private static final int MSG_UPDATE_VALUES = 101;
    private static final int MSG_POPUP = 102;
    private static final int MSG_CLOSE_POPUP = 103;
    private static final int MSG_RESET = 104;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_VALUES:
                    BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) msg.obj;

                    if (characteristic.getValue() != null) {
                        System.out.println("Get Value is: " + characteristic.getValue());
                        updateValues(characteristic);
                    }
                    else {
                        handler.sendMessage(Message.obtain(null, MSG_RESET, "Characteristic's value is null."));
                    }
                    break;
                case MSG_RESET:
                    progressDialog.hide();
                    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    resetApplication();
                    break;
                case MSG_POPUP:
                    progressDialog.setMessage((String) msg.obj);

                    if (!progressDialog.isShowing()) {
                        progressDialog.show();
                    }
                    break;
                case MSG_CLOSE_POPUP:
                    progressDialog.hide();
                    break;
                default:
                    break;
            }
        }
    };

    private void updateValues(BluetoothGattCharacteristic characteristic) {
        // Parse values from characteristic then update each value's TextView
        // Format of characteristic: ("temperature, humidity, lighting (visible light), IR, UV, setTemp, setHumid, setLight")
        String values = characteristic.getStringValue(0);
        String[] tokenSettings = values.split(",");

        for (int i = 0; i < tokenSettings.length; i++) {
            String temp = "";
            System.out.println("Setting is: " + tokenSettings[i]);
            switch (i) {
                case 0:
                    currentTemperature.setText(tokenSettings[i].concat("°"));
                    break;
                case 1:
                    temp = tokenSettings[i].concat("%");
                    currentHumidity.setText(setSpannableView(temp, true));
                    break;
                case 2:
                    temp = tokenSettings[i].concat("%");
                    currentLighting.setText(setSpannableView(temp, true));
                    break;
//                case 3:
//                    currIR.setText(tokenSettings[i]);
//                    break;
//                case 4:
//                    currUV.setText(tokenSettings[i]);
//                    break;
                case 3:
                    setTemperatureDisplay.setText(tokenSettings[i].concat(""));
                    break;
                case 4:
                    setHumidityDisplay.setText(tokenSettings[i].concat(""));
                    break;
                case 5:
                    setLightingDisplay.setText(tokenSettings[i].concat(""));
                    break;
                default:
                    Log.w(SENSOR_TAG, "Error obtaining rest of the values.");
                    break;
            }
        }

        // Disables popup once the initial values have been set.
        if (!sensorsInitialized) {
            sensorsInitialized = true;
            handler.sendEmptyMessage(MSG_CLOSE_POPUP);
        }
    }

    // Used to create a textView with different textSizes
    private Spannable setSpannableView(String temp, boolean isSpannable) {
        Spannable spannable = new SpannableString(temp);
        if (isSpannable) {
            spannable.setSpan(new RelativeSizeSpan(.5f), (temp.length() - 1), temp.length(), 0);
        }
        return spannable;
    }

    // Updates the setValues when any of the + or - buttons are pressed
    private void updateSetValues(int valueType, boolean isAddition) {
        String tempTemp = currentTemperature.getText().toString();
        String tempHumid = currentHumidity.getText().toString();
        String tempLight = currentLighting.getText().toString();
        //String tempIR = currIR.getText().toString();
        //String tempUV = currUV.getText().toString();
        String tempSetTemp = setTemperatureDisplay.getText().toString();
        String tempSetHumid = setHumidityDisplay.getText().toString();
        String tempSetLight = setLightingDisplay.getText().toString();

        int currTemperature = Integer.parseInt(tempTemp.substring(0, (tempTemp.length() - 1)));
        int currHumid = Integer.parseInt(tempHumid.substring(0, (tempHumid.length() - 1)));
        int currLight = Integer.parseInt(tempLight.substring(0, (tempLight.length() - 1)));
        //int currentIR = Integer.parseInt(tempIR);
        //int currentUV = Integer.parseInt(tempUV);
        int setTemp = Integer.parseInt(tempSetTemp);
        int setHumidity = Integer.parseInt(tempSetHumid);
        int setLighting = Integer.parseInt(tempSetLight);

        switch (valueType) {
            case TEMPERATURE:
                setTemp = isAddition ? setTemp + 1: setTemp - 1;
                setTemperatureDisplay.setText(String.valueOf(setTemp));
                break;
            case HUMIDITY:
                setHumidity = isAddition ? setHumidity + 10: setHumidity - 10;
                setHumidityDisplay.setText(String.valueOf(setHumidity));
                break;
            case LIGHTING:
                setLighting = isAddition ? setLighting + 10: setLighting - 10;
                setLightingDisplay.setText(String.valueOf(setLighting));
                break;
            default:
                break;
        }

        String output = currTemperature + "," + currHumid + "," + currLight + "," + setTemp + "," + setHumidity + "," + setLighting; // If need IR or UV add back here
        BluetoothGattService btService = btGatt.getService(HM10_SERVICE);

        if (btService != null) {
            BluetoothGattCharacteristic characteristic = btService.getCharacteristic(HM10_CHARACTERISTIC);
            if (characteristic != null) {
                characteristic.setValue(output);
                if (!btGatt.writeCharacteristic(characteristic)) {
                    Log.w(SENSOR_TAG, "Error writing characteristic.");
                }
            }
            else {
                Log.w(SENSOR_TAG, "Error fetching characteristic.");
            }
        }
        else {
            Log.w(SENSOR_TAG, "Cannot get bt service");
        }
    }

    private int getSetValue(int valueType) {
        int value = 0;

        switch (valueType) {
            case TEMPERATURE:
                String tempSetTemp = setTemperatureDisplay.getText().toString(); // TODO: FIX THIS SHIT
                value = Integer.parseInt(tempSetTemp);
                break;
            case HUMIDITY:
                String tempSetHumid = setHumidityDisplay.getText().toString();
                value = Integer.parseInt(tempSetHumid);
                break;
            case LIGHTING:
                String tempSetLight = setLightingDisplay.getText().toString();
                value = Integer.parseInt(tempSetLight);
                break;
            default:
                Log.w(SENSOR_TAG, "Error initializing set values");
        }

        return value;
    }
}