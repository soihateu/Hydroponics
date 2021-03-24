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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

    private TextView currentTemperture;
    private TextView currentHumidity;
    private TextView currentLighting;
    private TextView setTemperature;
    private TextView setHumidity;
    private TextView setLighting;
    private Button connectButton;

    private boolean sensorsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get all text views
        currentTemperture = findViewById(R.id.currTemp);
        currentHumidity = findViewById(R.id.currHumidity);
        currentLighting = findViewById(R.id.currLighting);
        setTemperature = findViewById(R.id.setTemp);
        setHumidity = findViewById(R.id.setHumidity);
        setLighting = findViewById(R.id.setLighting);
        connectButton = findViewById(R.id.connect);

        // Setup progress dialog; shows as loading dialog while connecting to device
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        // Setup connect button
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
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
        currentTemperture.setText("-------");
        currentHumidity.setText("-------");
        currentLighting.setText("-------");
        setTemperature.setText("-------");
        setHumidity.setText("-------");
        setLighting.setText("-------");

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
                    // TODO: test write, remove later
                    if (!characteristic.setValue("50,40,70")) {
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
        String values = characteristic.getStringValue(0);

        int delimiterIndex = values.indexOf(",");

        // Get temperature value
        if (delimiterIndex != -1) {
            String temperatureValue = values.substring(0, delimiterIndex);
            values = values.substring(delimiterIndex+1);
            currentTemperture.setText(temperatureValue.concat("°"));
        }
        else {
            Log.w(SENSOR_TAG, "Error obtaining rest of the values.");
            return;
        }

        delimiterIndex = values.indexOf(",");

        // Get humidity value
        if (delimiterIndex != 1) {
            String humidityValue = values.substring(0, delimiterIndex);
            values = values.substring(delimiterIndex+1);
            currentHumidity.setText(humidityValue.concat("%"));
        }
        else {
            Log.w(SENSOR_TAG, "Error obtaining rest of the values.");
            return;
        }

        // Get lighting value
        if (values.length() > 0) {
            currentLighting.setText(values.concat("%"));
        }
        else {
            Log.w(SENSOR_TAG, "Error obtaining lighting value.");
            return;
        }

        if (!sensorsInitialized) {
            sensorsInitialized = true;
            handler.sendEmptyMessage(MSG_CLOSE_POPUP);
        }
    }
}