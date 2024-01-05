// BluetoothService.java
package com.example.mobile_project;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.util.UUID;

public class BluetoothService {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private UUID MY_UUID;

    public BluetoothService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    public boolean checkBluetoothPermission() {

        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean connectToDevice(String deviceAddress) {
        if (!checkBluetoothPermission()) {

            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {

            closeConnection();

            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            bluetoothDevice = device; // Bağlandığımız cihazı kaydet
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendData(String data) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.getOutputStream().write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String receiveData() {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                bytes = bluetoothSocket.getInputStream().read(buffer);
                return new String(buffer, 0, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void closeConnection() {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bluetoothSocket = null;
            bluetoothDevice = null;
        }
    }
}
