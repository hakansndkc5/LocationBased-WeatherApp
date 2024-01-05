package com.example.mobile_project;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.provider.Settings;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap googleMap;
    private BluetoothService bluetoothService;
    private String appTempValue;
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;



    DecimalFormat df = new DecimalFormat("#.##");
    private static final String WEATHERBIT_API_KEY = "0362828048844c8fa0f2a532bfc82a8a";
    private static final String WEATHERBIT_BASE_URL = "https://api.weatherbit.io/v2.0/current";
    private TextView weatherInfoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        bluetoothService = new BluetoothService();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        weatherInfoTextView = findViewById(R.id.weatherInfoTextView);


        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAw8Z1c19WtpnuZPzaNvrl_KNEQmk7nJtc");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnMapClickListener(this);
        connectBluetoothDevice("00:11:22:33:44:55");

        getCurrentLocation();
    }

    private void connectBluetoothDevice(String deviceAddress) {

        ConnectTask connectTask = new ConnectTask();
        connectTask.execute(deviceAddress);
    }


    @Override
    public void onMapClick(LatLng latLng) {
        double latitude = latLng.latitude;
        double longitude = latLng.longitude;
        Log.d("Latitude", String.valueOf(latitude));
        Log.d("Longitude", String.valueOf(longitude));
        showBluetoothDeviceDialog();

        String weatherData = getWeatherData(latitude, longitude);
        String jsonString = weatherData;
        Log.d("weather",weatherData);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        JSONArray dataArray = null;
        try {
            dataArray = jsonObject.getJSONArray("data");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        JSONObject firstObject = null;
        try {
            firstObject = dataArray.getJSONObject(0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        JSONObject weatherObject = null;
        try {
            weatherObject = firstObject.getJSONObject("weather");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        try {
            String description = weatherObject.getString("description");
            String targetKey = "\"temp\":";
            int index = jsonString.indexOf(targetKey);

            if (index != -1) {

                int startIndex = index + targetKey.length();


                String appTempSubstring = jsonString.substring(startIndex);


                int commaIndex = appTempSubstring.indexOf(',');

                if (commaIndex == -1) {
                    commaIndex = appTempSubstring.indexOf(']');
                }


                String appTempValue = appTempSubstring.substring(0, commaIndex);
                weatherInfoTextView.setText(appTempValue + " degrees!!! and the weather is: " + description);
                //Toast.makeText(this, appTempValue, Toast.LENGTH_LONG).show();

                System.out.println("app_temp değeri: " + appTempValue);
            } else {
                System.out.println("fail.");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }



    }





    private void showBluetoothDeviceDialog() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {

            //Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();


        if (pairedDevices.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a Bluetooth Device");


            final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
            String[] deviceNames = new String[deviceList.size()];
            for (int i = 0; i < deviceList.size(); i++) {
                deviceNames[i] = deviceList.get(i).getName();
            }


            builder.setItems(deviceNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    BluetoothDevice selectedDevice = deviceList.get(which);

                    String deviceAddress = selectedDevice.getAddress();

                    connectBluetoothDevice(deviceAddress);
                }
            });


            builder.show();
        } else {

            //Toast.makeText(this, "No paired Bluetooth devices found. Pair a device first.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    }

    private String getWeatherData(double latitude, double longitude) {
        try {
            String urlString = WEATHERBIT_BASE_URL + "?lat=" + latitude + "&lon=" + longitude + "&key=" + WEATHERBIT_API_KEY;
            Log.d("string",urlString);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
                Log.d("asd","assdsad");
                System.out.println(line);
                Log.d("line",line);
            }

            reader.close();



            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }
    private void getCurrentLocation() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }



        googleMap.setMyLocationEnabled(true);


        Location location = googleMap.getMyLocation();

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            LatLng currentLocation = new LatLng(latitude, longitude);
            googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            Log.d("asd",currentLocation.toString());
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }




    private class ConnectTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String deviceAddress = params[0];
            return bluetoothService.connectToDevice(deviceAddress);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {

                sendDataOverBluetooth(appTempValue);
                Log.d("ads","veri gönderildi");
            } else {
                Log.d("asd","Bağlantı kurulamadı.");
            }
        }
    }
    private void sendDataOverBluetooth(String data) {

        bluetoothService.sendData(data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.closeConnection();
        }
    }
}

