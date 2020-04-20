package com.bencarlisle.killswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.HashMap;
import java.util.Set;

class Connections extends Thread {

    private final DataControl dataControl;
    private final BluetoothAdapter bluetoothAdapter;
    private final HashMap<String, Client> devices = new HashMap<>();

    Connections(DataControl dataControl, BluetoothAdapter bluetoothAdapter) {
        this.dataControl = dataControl;
        this.bluetoothAdapter = bluetoothAdapter;
        this.start();
    }

    void toggleState() {
        Client.toggleState();
        if (Client.isRunning()) {
            Log.e("KILLSWITCH", "Running");
            Thread.currentThread().interrupt();
            this.interrupt();
        }
    }

    @SuppressWarnings("ConstantConditions")
    void removeDevice(String name) {
        if (devices.containsKey(name)) {
            devices.remove(name).stopClient();
        }
    }

    public void run() {
        while (true) {
            if (Client.isRunning()) {
                Log.e("KILLSWITCH", "Connecting");
                refresh();
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
                Log.e("KILLSWITCH", "Sleep interrupted");
            }
        }
    }

    private void refresh() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress();
            Log.e("KILLSWITCH", devices.containsKey(deviceName) + " " + Client.isRunning() + " " + dataControl.deviceExists(deviceName));
            if (!devices.containsKey(deviceName) && Client.isRunning() && dataControl.deviceExists(deviceName)) {
                devices.put(deviceName, new Client(dataControl, device, bluetoothAdapter));
            }
        }
    }
}
