package com.bencarlisle.killswitch;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class NewDevices extends Thread {

    private final Activity activity;
    private final HashMap<String, View> newDevices;
    private final BluetoothAdapter bluetoothAdapter;
    private final DataControl dataControl;

    NewDevices(Activity activity, BluetoothAdapter bluetoothAdapter, DataControl dataControl) {
        this.activity = activity;
        this.bluetoothAdapter = bluetoothAdapter;
        this.dataControl = dataControl;
        newDevices = new HashMap<>();
    }

    public void run() {
        while (true) {
            refreshList();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void refreshList() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Set<String> unusedNames = new HashSet<>(newDevices.keySet());
        for (BluetoothDevice device : pairedDevices) {
            String name = device.getName();
            if (!newDevices.containsKey(name) && !dataControl.deviceExists(name)) {
                newDevices.put(name, addDeviceToList(name));
            } else {
                unusedNames.remove(name);
            }
        }
        for (String name: unusedNames) {
            removeDeviceFromList(name);
            newDevices.remove(name);
        }
    }

    void removeFromList(String name) {
        final View newDevice = newDevices.remove(name);
        final LinearLayout newDevices = activity.findViewById(R.id.new_devices);
        assert newDevice != null;
        ((TextView) newDevice.findViewById(R.id.name)).setText(name);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newDevices.removeView(newDevice);
            }
        });
    }

    private View addDeviceToList(String name) {
        final LinearLayout newDevices = activity.findViewById(R.id.new_devices);
        final LinearLayout newDevice = (LinearLayout) View.inflate(activity, R.layout.new_device, null);
        ((TextView) newDevice.findViewById(R.id.name)).setText(name);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newDevices.addView(newDevice);
            }
        });
        return newDevice;
    }

    private void removeDeviceFromList(String name) {
        LinearLayout newDevicesList = activity.findViewById(R.id.new_devices);
        View newDevice = newDevices.get(name);
        newDevicesList.removeView(newDevice);
    }
}
