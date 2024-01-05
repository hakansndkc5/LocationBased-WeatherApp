package com.example.mobile_project;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private Switch nightModeSwitch;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private TextView weatherTextView;
    private TextView forecastTextView;
    private boolean isButtonClicked = false;
    private LocationHelper locationHelper;

    private LinearLayout forecastLayout;
    // Weatherbit API Key
    private static final String API_KEY = "0362828048844c8fa0f2a532bfc82a8a";
    private static final String API_URL = "https://api.weatherbit.io/v2.0/current";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        forecastLayout = findViewById(R.id.forecastLayout);
        //NotificationHelper.showNotification(this, "Welcome to app!", "Uygulama başlatıldı.");

        Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        nightModeSwitch = findViewById(R.id.nightModeSwitch);
        nightModeSwitch.setChecked(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);

        nightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                recreate();
            }
        });
        EditText cityEditText = findViewById(R.id.cityEditText);
        Button searchButton = findViewById(R.id.searchButton);
        final Animation buttonClickAnimation = AnimationUtils.loadAnimation(this, R.anim.button_click_animation);


        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClickAnimation);
                String cityName = cityEditText.getText().toString().trim();
                isButtonClicked = true;

                if (!TextUtils.isEmpty(cityName)) {
                    new GetWeatherTask().execute(cityName);
                } else {
                    Toast.makeText(MainActivity.this, "Lütfen bir şehir adı girin", Toast.LENGTH_SHORT).show();
                }
            }
        });
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                if (item.getItemId() == R.id.nav_go_to_map) {
                    Intent mapIntent = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(mapIntent);
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });

        weatherTextView = findViewById(R.id.weatherTextView);
        forecastTextView = findViewById(R.id.forecastTextView);



        locationHelper = new LocationHelper(this);


        new GetWeatherTask().execute();
        new GetForecastTask().execute();

    }

    private class GetWeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String apiUrl;

                Location location = locationHelper.getLastKnownLocation();
                if (location != null && isButtonClicked != true) {
                    apiUrl = API_URL + "?key=" + API_KEY +
                            "&lat=" + location.getLatitude() +
                            "&lon=" + location.getLongitude();
                    Log.d("locaiton","girdi");
                } else {
                    String cityName = params[0];

                    if (TextUtils.isEmpty(cityName)) {
                        Log.e("GetWeatherTask", "Şehir adı boş.");
                        return null;
                    }

                    apiUrl = API_URL + "?key=" + API_KEY +
                            "&city=" + cityName + "&days=3";
                }

                Log.d("url", apiUrl);

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder json = new StringBuilder(1024);
                String tmp;
                while ((tmp = reader.readLine()) != null)
                    json.append(tmp).append("\n");
                reader.close();
                return json.toString();
            } catch (Exception e) {
                Log.e("GetWeatherTask", "Hata: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }




        @Override
        protected void onPostExecute(String json) {
            try {
                String cityName = null;
                if (json != null) {
                    JSONObject data = new JSONObject(json);
                    Log.d("json", data.toString());
                    JSONArray forecastArray = data.getJSONArray("data");


                    String temperature = data.getJSONArray("data").getJSONObject(0).getString("app_temp");
                    cityName = data.getJSONArray("data").getJSONObject(0).getString("city_name");
                    String description = data.getJSONArray("data").getJSONObject(0).getJSONObject("weather").getString("description");
                    String weatherCode = data.getJSONArray("data").getJSONObject(0).getJSONObject("weather").getString("code");
                    for (int i = 0; i < forecastArray.length(); i++) {
                        JSONObject forecast = forecastArray.getJSONObject(i);
                        String date = forecast.getString("datetime");
                        String temperature2 = forecast.getString("temp");

                        Log.d("Weather Forecast", "Date: " + date + ", Temperature: " + temperature2);


                    }
                    String notificationContent = "Temperature at " + cityName + ": " + temperature + " °C. Weather: " + description;
                    NotificationHelper.showNotification(MainActivity.this, "Weather Update", notificationContent);

                    weatherTextView.setText("Temperature at " + cityName + " :" + temperature + " °C" + "\n The weather is : " + description);
                    showWeatherIcon(weatherCode);
                } else {

                    Log.e("Weather", "Hava durumu bilgileri alınamıyor.");
                }
                new GetForecastTask().execute(cityName);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Weather", "JSON işleme hatası: " + e.getMessage());
                   }
        }

    }
    private void setWeatherIcon(ImageView imageView, String weatherCode) {
        if (weatherCode.equals("800") || (weatherCode.startsWith("8") && !weatherCode.equals("800"))) {
            // 800 veya "8" ile başlıyorsa sunny ikonunu ata
            imageView.setImageResource(R.drawable.sunny);
        } else if (weatherCode.startsWith("5")) {
            // "5" ile başlıyorsa rainy ikonunu ata
            imageView.setImageResource(R.drawable.rainy);
        } else if (weatherCode.startsWith("6")) {
            // "6" ile başlıyorsa snow ikonunu ata
            imageView.setImageResource(R.drawable.snow);
        } else {
            // Diğer durumlar için varsayılan ikonu ata
            imageView.setImageResource(R.drawable.cloudy);
        }
    }

    private class GetForecastTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String cityName = params[0];
                if (TextUtils.isEmpty(cityName)) {
                    Log.e("GetForecastTask", "Şehir adı boş.");
                    return null;
                }

                String apiUrl = "https://api.weatherbit.io/v2.0/forecast/daily?key=" + API_KEY +
                        "&city=" + cityName + "&days=7";

                Log.d("url", apiUrl);

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder json = new StringBuilder(1024);
                String tmp;
                while ((tmp = reader.readLine()) != null)
                    json.append(tmp).append("\n");
                reader.close();
                return json.toString();
            } catch (Exception e) {
                Log.e("GetForecastTask", "Hata: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }


        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected void onPostExecute(String json) {
            try {
                if (json != null) {
                    JSONObject data = new JSONObject(json);
                    Log.d("forecast json", data.toString());
                    JSONArray forecastArray = data.getJSONArray("data");
                    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE", new Locale("en"));
                    StringBuilder forecastText = new StringBuilder();
                    //String weatherCode = data.getJSONArray("data").getJSONObject(0).getJSONObject("weather").getString("code");

                    String forecastInfo = null;
                    forecastLayout.removeAllViews();
                    for (int i = 0; i < forecastArray.length(); i++) {
                        JSONObject forecast = forecastArray.getJSONObject(i);
                        String dateString = forecast.getString("datetime");
                        LocalDate date = LocalDate.parse(dateString, inputFormatter); // Tarihi parse et

                        String formattedDate = outputFormatter.format(date); // Tarihi gün adı olarak çevir

                        String maxTemperature = forecast.getString("app_max_temp");
                        String minTemperature = forecast.getString("app_min_temp");
                        String weatherCode = forecast.getJSONObject("weather").getString("code");
                        Log.d("TAG", weatherCode);




                        ImageView weatherIcon = new ImageView(MainActivity.this);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(60, 60);
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END); // ALIGN_PARENT_RIGHT deprecated olduğu için ALIGN_PARENT_END kullanıldı
                        weatherIcon.setLayoutParams(layoutParams);
                        weatherIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        weatherIcon.setVisibility(View.VISIBLE);

                        LinearLayout forecastIconsLayout = findViewById(R.id.forecastLayout);

                        setWeatherIcon(weatherIcon, weatherCode);

                        forecastInfo = String.format("%s    Max: %s     Min: %s", formattedDate, maxTemperature, minTemperature);

                        RelativeLayout forecastItemLayout = new RelativeLayout(MainActivity.this);

                        TextView forecastTextView = new TextView(MainActivity.this);
                        forecastTextView.setText(forecastInfo);

                        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                        textParams.addRule(RelativeLayout.ALIGN_PARENT_START); // Textview'i sola daya
                        forecastTextView.setLayoutParams(textParams);

                        forecastItemLayout.addView(forecastTextView);
                        forecastItemLayout.addView(weatherIcon);

                        forecastLayout.addView(forecastItemLayout);

                    }


                } else {
                    Log.e("Weather", "Hava durumu tahminleri alınamıyor.");
                    Log.d("hey","buraya giriyor");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Weather", "JSON işleme hatası: " + e.getMessage());
            }
        }
    }




    private void showWeatherIcon(String weatherCode) {
        ImageView sunnyImageView = findViewById(R.id.sunnyImageView);
        ImageView rainyImageView = findViewById(R.id.rainyImageView);
        ImageView cloudImageView = findViewById(R.id.cloudImageView);
        ImageView snowImageView = findViewById(R.id.snowImageView);

        if (weatherCode.equals("800")) {
            cloudImageView.setVisibility(View.GONE);
            sunnyImageView.setVisibility(View.VISIBLE);
            rainyImageView.setVisibility(View.GONE);
            snowImageView.setVisibility(View.GONE);
        } else if ((weatherCode.equals("600") && weatherCode.length() == 3) || (weatherCode.startsWith("6") && weatherCode.length() == 3)) {
            cloudImageView.setVisibility(View.GONE);
            sunnyImageView.setVisibility(View.GONE);
            rainyImageView.setVisibility(View.VISIBLE);
            snowImageView.setVisibility(View.GONE);
        } else if ((weatherCode.equals("801") || (weatherCode.equals("802") && weatherCode.length() == 3) || (weatherCode.equals("803") && weatherCode.length() == 3) || (weatherCode.equals("804") && weatherCode.length() == 3))) {
            sunnyImageView.setVisibility(View.GONE);
            rainyImageView.setVisibility(View.GONE);
            cloudImageView.setVisibility(View.VISIBLE);
            snowImageView.setVisibility(View.GONE);
        } else if (weatherCode.equals("504") || (weatherCode.startsWith("5") && weatherCode.length() == 3)) {
            cloudImageView.setVisibility(View.GONE);
            sunnyImageView.setVisibility(View.GONE);
            rainyImageView.setVisibility(View.VISIBLE);
            snowImageView.setVisibility(View.GONE);
        } else {
            cloudImageView.setVisibility(View.GONE);
            sunnyImageView.setVisibility(View.GONE);
            rainyImageView.setVisibility(View.GONE);
            snowImageView.setVisibility(View.GONE);
        }
    }
}


