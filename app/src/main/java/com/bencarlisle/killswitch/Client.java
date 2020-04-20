package com.bencarlisle.killswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Client extends Thread {

    private static boolean running = false;
    private boolean stopped = false;
    private BluetoothSocket socket;
    private final BluetoothAdapter bluetoothAdapter;
    private final DataControl dataControl;
    private final String name;

    Client(DataControl dataControl, BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
        this.dataControl = dataControl;
        this.bluetoothAdapter = bluetoothAdapter;
        this.name = device.getName();
        try {
            //noinspection JavaReflectionMemberAccess
            Method method = device.getClass().getMethod("createInsecureRfcommSocket", int.class);
            socket = (BluetoothSocket) method.invoke(device, 1);
        } catch (NoSuchMethodException e) {
            Log.e("KILLSWITCH", "Socket's create() method failed", e);
        } catch (IllegalAccessException e) {
            Log.e("KILLSWITCH", "Socket's create() method failed", e);
        } catch (InvocationTargetException e) {
            Log.e("KILLSWITCH", "Socket's create() method failed", e);
        }
        this.start();
    }

    static boolean isRunning() {
        return running;
    }

    static void toggleState() {
        running ^= true;
    }

    void stopClient() {
        stopped = true;
    }

    private byte[] readNextBytes(InputStream inputStream, int size) {
        byte[] bytes = new byte[size];
        int pos = 0;
        while (pos < size) {
            int length = Math.min(1024, size - pos);
            try {
                int result = inputStream.read(bytes, pos, length);
                if (result > 0) {
                    pos += result;
                } else {
                    yield();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }

    public void run() {
        bluetoothAdapter.cancelDiscovery();
        OutputStream outputStream;
        InputStream inputStream;
        try {
            socket.connect();
            Log.e("KILLSWITCH", "Accepted");
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            outputStream.write(bluetoothAdapter.getName().getBytes());
            byte[] result = readNextBytes(inputStream, 1);
            if (result[0] == 0) {
                return;
            }
            byte[] needsKeys = readNextBytes(inputStream, 1);
            Crypto crypto;
            if (needsKeys[0] == 1) {
                crypto = new Crypto(dataControl, name, true);
                byte[] publicKey = crypto.getPublicKey();
                byte[] length = new byte[]{(byte) (publicKey.length / 256), (byte) (publicKey.length % 256)};
                outputStream.write(length);
                outputStream.write(publicKey);
            } else {
                crypto = new Crypto(dataControl, name, false);
            }
            int challengeSize = crypto.getChallengeSize();
            while (!stopped && running) {
                byte[] challenge = readNextBytes(inputStream, challengeSize);
                Log.e("KILLSWITCH", "Received challenge");
                byte[] message = crypto.decryptChallenge(challenge);
                outputStream.write(message);
            }
            Log.e("SOCKET", "CLOSED");
        } catch (IOException connectException) {
            Log.e("KILLSWITCH", "Could not connect to the client socket", connectException);
        }
    }
}