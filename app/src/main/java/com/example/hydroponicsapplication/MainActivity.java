package com.example.hydroponicsapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String HM10_MAC_ADDRESS = "64:69:4E:8A:07:67";

    private static final UUID TEMPERATURE_SERVICE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID LIGHTING_SERVICE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID TEMPERATURE_SENSOR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID HUMIDITY_SENSOR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    private static final UUID LIGHTING_SENSOR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID TEMPERATURE_DATA = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID HUMIDITY_DATA = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID LIGHTING_DATA = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

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
            handler.sendMessage(Message.obtain(null, MSG_LOADING, "Connecting to " + btDevice.getName() + "..."));
            disableConnectButton();
        }
        else {
            Toast.makeText(this, ERROR_BLUETOOTH_CONNECTION_FAILED, Toast.LENGTH_SHORT).show();
        }
    }

    // Allow the button to be clickable once the application has connected to the bluetooth module
    private void enableConnectButton() {
        // Reset connection details
        btAdapter = null;
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt = null;
        }
        // Enable button
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
    }

    private void disableConnectButton() {
        connectButton.setText("Connected");
        connectButton.setEnabled(false);
    }

    private void resetApplication() {
        currentTemperture.setText("-------");
        currentHumidity.setText("-------");
        currentLighting.setText("-------");
        setTemperature.setText("-------");
        setHumidity.setText("-------");
        setLighting.setText("-------");
        enableConnectButton();
    }

    public final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        private int state = 0;

        private void reset() {
            state = 0;
        }

        private void next() {
            state++;
        }

        // Send an enable command to each sensor by writing a configuration characteristic
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;

            switch (state) {
                case 0:
                    // Enable temperature sensor
                    characteristic = gatt.getService(TEMPERATURE_SERVICE).getCharacteristic(TEMPERATURE_SENSOR);
                    characteristic.setValue(new byte[] {0x02}); // specific to hm10 device
                    break;
                case 1:
                    // Enable humidity sensor
                    characteristic = gatt.getService(HUMIDITY_SERVICE).getCharacteristic(HUMIDITY_SENSOR);
                    characteristic.setValue(new byte[] {0x01}); // specific to hm10 device
                    break;
                case 2:
                    // Enable lighting sensor
                    characteristic = gatt.getService(LIGHTING_SERVICE).getCharacteristic(LIGHTING_SENSOR);
                    characteristic.setValue(new byte[] {0x01}); // specific to hm10 device
                    break;
                default:
                    // All sensors are enabled
                    handler.sendEmptyMessage(MSG_LOAD_COMPLETE);
                    break;
            }

            if (characteristic != null) {
                gatt.writeCharacteristic(characteristic);
            }
        }

        // Read sensor data
        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;

            switch (state) {
                case 0:
                    // Reading temperature sensor data
                    characteristic = gatt.getService(TEMPERATURE_SERVICE).getCharacteristic(TEMPERATURE_DATA);
                    break;
                case 1:
                    // Reading humidity sensor data
                    characteristic = gatt.getService(HUMIDITY_SERVICE).getCharacteristic(HUMIDITY_DATA);
                    break;
                case 2:
                    // Reading lighting sensor data
                    characteristic = gatt.getService(LIGHTING_SERVICE).getCharacteristic(LIGHTING_DATA);
                    break;
                default:
                    handler.sendEmptyMessage(MSG_LOAD_COMPLETE);
                    break;
            }

            if (characteristic != null) {
                gatt.readCharacteristic(characteristic);
            }
        }

        // Notify us for any updates/changes on the sensors
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;

            switch (state) {
                case 0:
                    // Reading temperature sensor data
                    characteristic = gatt.getService(TEMPERATURE_SERVICE).getCharacteristic(TEMPERATURE_DATA);
                    break;
                case 1:
                    // Reading humidity sensor data
                    characteristic = gatt.getService(HUMIDITY_SERVICE).getCharacteristic(HUMIDITY_DATA);
                    break;
                case 2:
                    // Reading lighting sensor data
                    characteristic = gatt.getService(LIGHTING_SERVICE).getCharacteristic(LIGHTING_DATA);
                    break;
                default:
                    handler.sendEmptyMessage(MSG_LOAD_COMPLETE);
                    break;
            }

            if (characteristic != null) {
                // Enable local notifications
                gatt.setCharacteristicNotification(characteristic, true);
                // Enable remote notifications
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // Connection was successful
                gatt.discoverServices();
                handler.sendMessage(Message.obtain(null, MSG_LOADING, "Discovering services..."));
            }
            else if (status != BluetoothGatt.GATT_SUCCESS) {
                // Failed to connect
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            handler.sendMessage(Message.obtain(null, MSG_LOADING, "Enabling sensors..."));
            reset();
            enableNextSensor(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // Pass data read to Activity to update text fields
            if (TEMPERATURE_DATA.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_TEMPERATURE, characteristic));
            }
            if (HUMIDITY_DATA.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (LIGHTING_DATA.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_LIGHTING, characteristic));
            }

            setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Will handle all future updates on sensor value changes
            // Pass data read to Activity to update text fields
            if (TEMPERATURE_DATA.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_TEMPERATURE, characteristic));
            }
            if (HUMIDITY_DATA.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (LIGHTING_DATA.equals(characteristic.getUuid())) {
                handler.sendMessage(Message.obtain(null, MSG_LIGHTING, characteristic));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            next();
            enableNextSensor(gatt);
        }
    };

    private static final int MSG_TEMPERATURE = 101;
    private static final int MSG_HUMIDITY = 102;
    private static final int MSG_LIGHTING = 103;
    private static final int MSG_RESET = 201;
    private static final int MSG_LOADING = 202;
    private static final int MSG_LOAD_COMPLETE = 203;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_TEMPERATURE:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() != null) {
                        updateTemperatureValues(characteristic);
                    }
                    else {
                        Log.w(SENSOR_TAG, "Error obtaining temperature value.");
                    }
                    break;
                case MSG_HUMIDITY:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() != null) {
                        updateHumidityValues(characteristic);
                    }
                    else {
                        Log.w(SENSOR_TAG, "Error obtaining humidity value.");
                    }
                    break;
                case MSG_LIGHTING:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() != null) {
                        updateLightingValues(characteristic);
                    }
                    else {
                        Log.w(SENSOR_TAG, "Error obtaining lighting value.");
                    }
                    break;
                case MSG_RESET:
                    resetApplication();
                    break;
                case MSG_LOADING:
                    progressDialog.setMessage((String) msg.obj);
                    if (!progressDialog.isShowing())
                        progressDialog.show();
                    break;
                case MSG_LOAD_COMPLETE:
                    progressDialog.hide();
                    break;
            }
        }
    };

    private void updateTemperatureValues(BluetoothGattCharacteristic characteristic) {
        // TODO: convert sensor data into actual temperature celsius value
        currentTemperture.setText("25°"); // placeholder
    }

    private void updateHumidityValues(BluetoothGattCharacteristic characteristic) {
        // TODO: convert sensor data into actual humidity celsius value
        currentHumidity.setText("80%"); // placeholder
    }

    private void updateLightingValues(BluetoothGattCharacteristic characteristic) {
        // TODO: convert sensor data into actual lighting celsius value
        currentLighting.setText("50%"); // placeholder
    }
}