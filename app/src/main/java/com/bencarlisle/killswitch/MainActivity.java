package com.bencarlisle.killswitch;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private Connections connections;
    private DataControl dataControl;
    private NewDevices newDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("KILLSWITCH", "Sorry bluetooth is not supported on this device");
            return;
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        this.dataControl = new DataControl(this);
        connections = new Connections(dataControl, bluetoothAdapter);
        initAddedDevices();
        this.newDevices = new NewDevices(this, bluetoothAdapter, dataControl);
        this.newDevices.start();
    }

    private void initAddedDevices() {
        ArrayList<String> addedDevices = dataControl.getDevices();
        for (String name: addedDevices) {
            addDeviceToList(name);
        }
    }

    public void toggleState(View view) {
        connections.toggleState();
        Button button = (Button) view;
        if (button.getText().equals("Start")) {
            button.setText(R.string.stop);
        } else {
            button.setText(R.string.start);
        }
    }

    @SuppressWarnings("unused")
    public void submitDeviceName(View view) {
        String name = ((TextView) findViewById(R.id.new_device)).getText().toString();
        addDevice(name);
    }

    public void addDeviceName(View view) {
        String name = ((TextView) view).getText().toString();
        if (!addDevice(name)) {
            return;
        }
        newDevices.removeFromList(name);
    }

    private boolean addDevice(String name) {
        if (!dataControl.addDevice(name)) {
            Toast.makeText(this, "Device already exists", Toast.LENGTH_SHORT).show();
            return false;
        }
        addDeviceToList(name);
        return true;
    }

    private void addDeviceToList(String name) {
        LinearLayout addedDevices = findViewById(R.id.added_devices);
        LinearLayout newDevice = (LinearLayout) View.inflate(this, R.layout.added_device, null);
        ((TextView) newDevice.findViewById(R.id.name)).setText(name);
        addedDevices.addView(newDevice);
        connections.interrupt();
    }

    public void removeDevice(View view) {
        String name = ((TextView) view).getText().toString();
        connections.removeDevice(name);
        dataControl.removeDevice(name);
        LinearLayout addedDevices = findViewById(R.id.added_devices);
        LinearLayout parent = (LinearLayout) view.getParent();
        addedDevices.removeView(parent);
        newDevices.refreshList();
    }
}
