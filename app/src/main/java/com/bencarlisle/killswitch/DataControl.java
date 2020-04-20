package com.bencarlisle.killswitch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

class DataControl extends SQLiteOpenHelper {

    private final SQLiteDatabase database;

    DataControl(Context context) {
        super(context, "KillSwitch.db", null, 1);
        database = getWritableDatabase();
//        onCreate(database);
//        onUpgrade(database, 0, 0);
    }

    ArrayList<String> getDevices() {
        String[] columns = new String[]{ "name" };
        Cursor cursor = database.query("Devices", columns, null, null, null, null, null);
        ArrayList<String> addedDevices = new ArrayList<>();
        if (cursor == null)  {
            return addedDevices;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            addedDevices.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return addedDevices;
    }

    void removeDevice(String name) {
        String[] args = new String[] { name };
        database.delete("Devices", "name = ?", args);
    }

    boolean deviceExists(String name) {
        String[] args = new String[] { name };
        String[] columns = new String[]{ "name" };
        Cursor cursor = database.query("Devices", columns, "name = ?", args, null, null, null);
        if (cursor == null) {
            return false;
        }
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    boolean addDevice(String name) {
        if (deviceExists(name)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("name", name);
        database.insert("Devices", null, values);
        return true;
    }

    byte[] getPrivateKey(String name) {
        return getKey(name, "privateKey");
    }

    byte[] getPublicKey(String name) {
        return getKey(name, "publicKey");
    }

    private byte[] getKey(String name, String column) {
        String[] columns = new String[]{ column };
        String[] args = new String[]{ name };
        Cursor cursor = database.query("Connections", columns, "name = ?", args, null, null, null);
        byte[] privateKey = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            privateKey = cursor.getBlob(0);
        }
        if (cursor != null) {
            cursor.close();
        }
        return privateKey;
    }

    void setKeys(String name, byte[] privateKey, byte[] publicKey) {
        String[] columns = new String[]{ "name" };
        String[] args = new String[]{ name };
        Cursor cursor = database.query("Connections", columns, "name = ?", args, null, null, null);
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("privateKey", privateKey);
        values.put("publicKey", publicKey);
        if (cursor == null || cursor.getCount() == 0) {
            database.insert("Connections", null, values);
        } else {
            database.update("Connections", values, "name = ?", args);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE Connections (name VARCHAR(255) PRIMARY KEY UNIQUE NOT NULL, publicKey VARBINARY(2048) NOT NULL, privateKey VARBINARY(2048) NOT NULL);");
        database.execSQL("CREATE TABLE Devices (name VARCHAR(255) PRIMARY KEY UNIQUE NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE Connections;");
        database.execSQL("DROP TABLE Devices;");
        onCreate(database);
    }
}
