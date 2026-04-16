package com.example.goldtracker;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class WeatherMapActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LON = "extra_lon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_map);

        // Mặc định Hồ Chí Minh nếu không có tọa độ
        double lat = getIntent().getDoubleExtra(EXTRA_LAT, 10.8);
        double lon = getIntent().getDoubleExtra(EXTRA_LON, 106.7);

        ImageView ivBack = findViewById(R.id.ivBackMap);
        ivBack.setOnClickListener(v -> finish());

        WebView wvFullMap = findViewById(R.id.wvFullMap);
        WeatherMapHelper.setupWebView(wvFullMap);
        WeatherMapHelper.load(wvFullMap, lat, lon, BuildConfig.WEATHER_API_KEY);
    }
}
