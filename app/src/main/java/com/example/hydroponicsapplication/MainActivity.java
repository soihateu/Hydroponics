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

    private BluetoothAdapter btAdapter;
    private BluetoothGatt btGatt;

    private ProgressDialog progressDialog;

    private TextView currentTemperature;
    private TextView currentHumidity;
    private TextView currentLighting;
    private TextView setTemperatureDisplay;
    private TextView setHumidityDisplay;
    private TextView setLightingDisplay;
    private Button connectButton;
    private Button addTempButton;
    private Button minusTempButton;
    private Button addHumidityButton;
    private Button minusHumidityButton;
    private Button addLightingButton;
    private Button minusLightingButton;

    private int setTemperatureValue = 0;
    private int setHumidityValue = 0;
    private int setLightingValue = 0;
    private boolean sensorsInitialized = false;

    SharedPreferences settings;
    SharedPreferences.Editor editor;

    private String initializeDisplayValues;

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

        // Setup progress dialog; shows as loading dialog while connecting to device
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        // Preferences to hold set temp, set humidity, and set lighting values
        //this.getSharedPreferences("settings", this.MODE_PRIVATE).edit().clear().apply(); // Used to remove settings if preferences get corrupted or there is bug in file
        settings = this.getSharedPreferences("settings", 0);

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
                if (sensorsInitialized) {
                    setTemperatureDisplay.setText(String.valueOf(++setTemperatureValue));
                    savePreferences(0);
                }
            }
        });
        minusTempButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && setTemperatureValue > 0) {
                    setTemperatureDisplay.setText(String.valueOf(--setTemperatureValue));
                    savePreferences(0);
                }
            }
        });
        addHumidityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized) {
                    setHumidityDisplay.setText(String.valueOf(++setHumidityValue));
                    savePreferences(1);
                }
            }
        });
        minusHumidityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && setHumidityValue > 0) {
                    setHumidityDisplay.setText(String.valueOf(--setHumidityValue));
                    savePreferences(1);
                }
            }
        });
        addLightingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized) {
                    setLightingDisplay.setText(String.valueOf(++setLightingValue));
                    savePreferences(2);
                }
            }
        });
        minusLightingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorsInitialized && setLightingValue > 0) {
                    setLightingDisplay.setText(String.valueOf(--setLightingValue));
                    savePreferences(2);
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
            initializeDisplayValues = loadPreferences();
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

                if (characteristic != null) {

                    // TODO: Add handler to call a function to load preferences and concatenate with the initial "currentValues" so that it can be used later for setValue();
                    //handler.sendMessage(Message.obtain(null, MSG_PREFERENCES, "Fetching preferences..."));

                    // Initially set values to 0 by default
                    if (!characteristic.setValue(initializeDisplayValues)) { // TODO: Replace this with full 6 digits
                        handler.sendMessage(Message.obtain(null, MSG_RESET, "Failed to write test value to characteristic."));
                    }

                    // Write to remote characteristic
                    if (!gatt.writeCharacteristic(characteristic)) {
                        handler.sendMessage(Message.obtain(null, MSG_RESET, "Failed to write test value to characteristic."));
                    }

                    handler.sendMessage(Message.obtain(null, MSG_UPDATE_VALUES, characteristic));
                }
                else {
                    handler.sendMessage(Message.obtain(null, MSG_RESET, "Failed to obtain characteristic of service."));
                    return;
                }
            }
            else {
                handler.sendMessage(Message.obtain(null, MSG_RESET, "Failed to obtain service."));
                return;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Will handle all future updates on sensor value changes
            // Pass data read to Activity to update text fields
            if (HM10_CHARACTERISTIC.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_UPDATE_VALUES, characteristic));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            subscribeToSensorNotifications(gatt);
        }
    };

    private static final int MSG_UPDATE_VALUES = 101;
    private static final int MSG_POPUP = 102;
    private static final int MSG_CLOSE_POPUP = 103;
    private static final int MSG_RESET = 104;
    private static final int MSG_PREFERENCES = 105;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_VALUES:
                    BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) msg.obj;

                    if (characteristic.getValue() != null) {
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
//                case MSG_PREFERENCES:
//                    initializeDisplay = loadPreferences();
//                    break;
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
        String values = characteristic.getStringValue(0);
        String[] tokenSettings = values.split(",");

        for (int i = 0; i < tokenSettings.length; i++) {
            String temp = "";
            switch (i) {
                case 0:
                    currentTemperature.setText(tokenSettings[i].concat("°"));
                    break;
                case 1:
                    temp = tokenSettings[i].concat("%");
                    currentHumidity.setText(setSpannableView(temp));
                    break;
                case 2:
                    temp = tokenSettings[i].concat("%");
                    currentLighting.setText(setSpannableView(temp));
                    break;
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

    private Spannable setSpannableView(String temp) {
        Spannable spannable = new SpannableString(temp);
        spannable.setSpan(new RelativeSizeSpan(.5f), (temp.length() - 1), temp.length(), 0);
        return spannable;
    }

    private void savePreferences(int settingType) {
        switch (settingType) {
            case 0:
                settings.edit().putString("temp", "," + setTemperatureValue).apply();
                break;
            case 1:
                settings.edit().putString("humid", "," + setHumidityValue).apply();
                break;
            case 2:
                settings.edit().putString("light", "," + setLightingValue).apply();
                break;
            default:
                Log.w(SENSOR_TAG, "Error saving preferences.");
        }
    }

    private String loadPreferences() {
        // Loads the preferences on startup
        String initialValues = "0,0,0";

        // Retrieves the rest of the values
        String temp = settings.getString("temp", ",1");
        setTemperatureValue = Integer.parseInt(temp.substring(1));
        String humid = settings.getString("humid", ",2");
        setHumidityValue = Integer.parseInt(humid.substring(1));
        String light = settings.getString("light", ",3");
        setLightingValue = Integer.parseInt(light.substring(1));

        return (initialValues + temp + humid + light);
    }
}