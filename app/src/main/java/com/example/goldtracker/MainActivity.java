package com.example.goldtracker;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvLocation, tvCurrentTemp, tvCondition, tvHumidity, tvWind;
    private ImageView imgCurrentWeather;
    private RecyclerView rvHourly, rvDaily;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_CODE = 102;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private final String API_KEY = BuildConfig.WEATHER_API_KEY;

    // Lắng nghe kết quả từ màn hình chọn thành phố
    private final ActivityResultLauncher<Intent> cityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String newCity = result.getData().getStringExtra("SELECTED_CITY");
                    if (newCity != null) fetchWeatherByCity(newCity);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerViews();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkPermissions();
    }

    private void initViews() {
        tvLocation = findViewById(R.id.tvLocation);
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        imgCurrentWeather = findViewById(R.id.imgCurrentWeather);
        findViewById(R.id.ivMenu).setOnClickListener(v -> cityLauncher.launch(new Intent(this, CityManagementActivity.class)));
    }

    private void setupRecyclerViews() {
        rvHourly = findViewById(R.id.rvHourlyForecast);
        rvDaily = findViewById(R.id.rvDailyForecast);
        rvHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDaily.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    // --- PHẦN 1: XỬ LÝ API DÙNG CHUNG (REFACTORING) ---

    private void fetchWeatherByGPS(double lat, double lon) {
        RetrofitClient.getApiService().getCurrentWeather(lat, lon, API_KEY, "metric", "vi")
                .enqueue(createWeatherCallback());
        RetrofitClient.getApiService().getForecast(lat, lon, API_KEY, "metric", "vi")
                .enqueue(createForecastCallback());
    }

    private void fetchWeatherByCity(String cityName) {
        RetrofitClient.getApiService().getWeatherByCityName(cityName, API_KEY, "metric", "vi")
                .enqueue(createWeatherCallback());
        RetrofitClient.getApiService().getForecastByCityName(cityName, API_KEY, "metric", "vi")
                .enqueue(createForecastCallback());
    }

    private Callback<WeatherResponse> createWeatherCallback() {
        return new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) updateCurrentWeatherUI(response.body());
            }
            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) { Log.e("API", "Error: " + t.getMessage()); }
        };
    }

    private Callback<ForecastResponse> createForecastCallback() {
        return new Callback<ForecastResponse>() {
            @Override
            public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                if (response.isSuccessful() && response.body() != null) processForecastData(response.body().list);
            }
            @Override
            public void onFailure(Call<ForecastResponse> call, Throwable t) { Log.e("API", "Error: " + t.getMessage()); }
        };
    }

    // --- PHẦN 2: CẬP NHẬT GIAO DIỆN ---

    private void updateCurrentWeatherUI(WeatherResponse data) {
        tvLocation.setText(data.cityName);
        tvCurrentTemp.setText(Math.round(data.main.temp) + "°");
        tvHumidity.setText(data.main.humidity + "%");
        tvWind.setText(Math.round(data.wind.speed * 3.6) + " km/h");

        if (!data.weather.isEmpty()) {
            String desc = data.weather.get(0).description;
            tvCondition.setText(desc.substring(0, 1).toUpperCase() + desc.substring(1));
            String serverIconCode = data.weather.get(0).icon;
            int beautifulIconResId = WeatherUtils.getLocalIcon(serverIconCode);
            imgCurrentWeather.setImageResource(beautifulIconResId);
        }
        checkAndShowWeatherAlert(data);
    }

    private void processForecastData(List<ForecastResponse.ForecastItem> list) {
        List<HourlyWeather> hourlyData = new ArrayList<>();
        List<DailyWeather> dailyData = new ArrayList<>();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'thg' M", new Locale("vi", "VN"));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

        for (int i = 0; i < list.size(); i++) {
            ForecastResponse.ForecastItem item = list.get(i);

            // Xử lý Hourly (5 mốc đầu)
            if (i < 5) {
                hourlyData.add(new HourlyWeather(item.dtTxt.substring(11, 16), Math.round(item.main.temp) + "°", item.weather.get(0).icon));
            }

            // Xử lý Daily (Mốc 12h trưa)
            if (item.dtTxt.contains("12:00:00")) {
                try {
                    Date date = inputFormat.parse(item.dtTxt);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);

                    String name = (cal.get(Calendar.DAY_OF_YEAR) == currentDay) ? "Hôm nay" :
                            (cal.get(Calendar.DAY_OF_YEAR) == currentDay + 1) ? "Ngày mai" :
                                    formatVietnameseDay(dayFormat.format(date));

                    dailyData.add(new DailyWeather(dateFormat.format(date) + " " + name,
                            Math.round(item.main.temp - 2) + "° / " + Math.round(item.main.temp + 2) + "°",
                            Math.round(item.pop * 100) + "%", item.weather.get(0).icon));
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        rvHourly.setAdapter(new HourlyAdapter(hourlyData));
        rvDaily.setAdapter(new DailyAdapter(dailyData));
    }

    private String formatVietnameseDay(String day) {
        day = day.substring(0, 1).toUpperCase() + day.substring(1);
        return day.replace("Hai", "2").replace("Ba", "3").replace("Tư", "4")
                .replace("Năm", "5").replace("Sáu", "6").replace("Bảy", "7");
    }

    // --- PHẦN 3: QUYỀN VÀ THÔNG BÁO ---

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            requestLocation();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
        WeatherAlertManager.createNotificationChannel(this);
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) fetchWeatherByGPS(loc.getLatitude(), loc.getLongitude());
            else fetchWeatherByCity("Ho Chi Minh");
        });
    }

    private void checkAndShowWeatherAlert(WeatherResponse data) {
        double temp = data.main.temp;
        String main = data.weather.get(0).main;
        String msg = "";

        if ("Thunderstorm".equalsIgnoreCase(main)) msg = "⚠️ Có giông bão sấm sét!";
        else if ("Rain".equalsIgnoreCase(main)) msg = "☔ Trời đang có mưa.";
        else if (temp >= 38) msg = "🔥 Trời cực kỳ nắng nóng (" + Math.round(temp) + "°C).";
        else if (temp <= 10) msg = "❄️ Trời rất lạnh (" + Math.round(temp) + "°C).";

        if (!msg.isEmpty()) WeatherAlertManager.sendNotification(this, "Cảnh báo: " + data.cityName, msg);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        }
    }
}