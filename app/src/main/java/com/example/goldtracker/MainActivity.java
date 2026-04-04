package com.example.goldtracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvLocation, tvCurrentTemp, tvCondition, tvHumidity, tvWind, tvRain;
    private ImageView imgCurrentWeather;
    private RecyclerView rvHourly, rvDaily;
    private WebView wvWeatherMap;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_CODE = 102;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_LAST_CITY = "last_city";
    private final String API_KEY = BuildConfig.WEATHER_API_KEY;

    // Lắng nghe kết quả từ màn hình chọn thành phố
    private final ActivityResultLauncher<Intent> cityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String newCity = result.getData().getStringExtra("SELECTED_CITY");
                    if (newCity != null) {
                        // Lưu thành phố vào SharedPreferences để dùng lại lần sau
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit().putString(KEY_LAST_CITY, newCity).apply();
                        fetchWeatherByCity(newCity);
                    }
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
        tvRain = findViewById(R.id.tvRain);
        imgCurrentWeather = findViewById(R.id.imgCurrentWeather);
        findViewById(R.id.ivMenu).setOnClickListener(v -> cityLauncher.launch(new Intent(this, CityManagementActivity.class)));

        // Khởi tạo WebView bản đồ thời tiết
        wvWeatherMap = findViewById(R.id.wvWeatherMap);
        wvWeatherMap.getSettings().setJavaScriptEnabled(true);
        wvWeatherMap.getSettings().setDomStorageEnabled(true);
        wvWeatherMap.getSettings().setUseWideViewPort(true);
        wvWeatherMap.getSettings().setLoadWithOverviewMode(true);
        wvWeatherMap.setWebViewClient(new WebViewClient());
        // Tải bản đồ mặc định căn giữa Hồ Chí Minh
        loadWeatherMap(10.8, 106.7);
    }

    private void setupRecyclerViews() {
        rvHourly = findViewById(R.id.rvHourlyForecast);
        rvDaily = findViewById(R.id.rvDailyForecast);
        rvHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDaily.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    // --- PHẦN 1: XỬ LÝ API ---

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

        // Tải bản đồ thời tiết Windy căn giữa vị trí hiện tại
        if (data.coord != null) {
            loadWeatherMap(data.coord.lat, data.coord.lon);
        }

        checkAndShowWeatherAlert(data);
    }

    private void loadWeatherMap(double lat, double lon) {
        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>"
                + "body{margin:0;padding:0;font-family:sans-serif;background:#0B132B;}"
                + "#controls{display:flex;flex-wrap:wrap;background:#0B132B;padding:5px;gap:4px;}"
                + ".btn{flex:1;min-width:55px;padding:6px 2px;border:none;border-radius:8px;"
                + "     background:#1E2A45;color:#90A4AE;font-size:11px;cursor:pointer;}"
                + ".btn.active{background:#E8614C;color:#fff;font-weight:bold;}"
                + "#map{width:100%;height:calc(100vh - 46px);}"
                + "</style>"
                + "</head><body>"
                + "<div id='controls'>"
                + "<button class='btn active' onclick='setLayer(\"temp_new\",this)'>🌡 Nhiệt độ</button>"
                + "<button class='btn' onclick='setLayer(\"pressure_new\",this)'>🔵 Áp suất</button>"
                + "<button class='btn' onclick='setLayer(\"wind_new\",this)'>💨 Gió</button>"
                + "<button class='btn' onclick='setLayer(\"precipitation_new\",this)'>🌧 Mưa</button>"
                + "<button class='btn' onclick='setLayer(\"clouds_new\",this)'>☁️ Mây</button>"
                + "</div>"
                + "<div id='map'></div>"
                + "<script>"
                + "var map=L.map('map',{zoomControl:true}).setView([" + lat + "," + lon + "],5);"
                + "L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}{r}.png',"
                + "  {attribution:'© CartoDB',subdomains:'abcd'}).addTo(map);"
                + "var labelLayer=L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}{r}.png',"
                + "  {subdomains:'abcd',zIndex:1000}).addTo(map);"
                + "var weatherLayer=null;"
                + "function setLayer(layer,btn){"
                + "  document.querySelectorAll('.btn').forEach(b=>b.classList.remove('active'));"
                + "  btn.classList.add('active');"
                + "  if(weatherLayer) map.removeLayer(weatherLayer);"
                + "  weatherLayer=L.tileLayer("
                + "    'https://tile.openweathermap.org/map/'+layer+'/{z}/{x}/{y}.png?appid=" + API_KEY + "',"
                + "    {opacity:0.85,attribution:'© OpenWeatherMap',zIndex:500}).addTo(map);"
                + "  labelLayer.bringToFront();"
                + "}"
                + "setLayer('temp_new',document.querySelector('.btn.active'));"
                + "</script></body></html>";
        wvWeatherMap.loadDataWithBaseURL("https://openweathermap.org", html, "text/html", "UTF-8", null);
    }

    private void processForecastData(List<ForecastResponse.ForecastItem> list) {
        if (list.isEmpty()) return;

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'thg' M", new Locale("vi", "VN"));
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);

        // Cập nhật xác suất mưa hiện tại từ mốc forecast đầu tiên
        tvRain.setText(Math.round(list.get(0).pop * 100) + "%");

        // --- Hourly: 5 mốc đầu tiên ---
        List<HourlyWeather> hourlyData = new ArrayList<>();
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            ForecastResponse.ForecastItem item = list.get(i);
            hourlyData.add(new HourlyWeather(
                    item.dtTxt.substring(11, 16),
                    Math.round(item.main.temp) + "°",
                    item.weather.get(0).icon));
        }

        // --- Daily: gom nhóm theo ngày, tính min/max thật từ API ---
        // Dùng LinkedHashMap để giữ đúng thứ tự ngày
        Map<String, double[]> dailyMinMax = new LinkedHashMap<>(); // key=ngày, value=[min,max]
        Map<String, String> dailyIcon = new HashMap<>();
        Map<String, Double> dailyPop = new HashMap<>();
        Map<String, String> dailyLabel = new LinkedHashMap<>();

        for (ForecastResponse.ForecastItem item : list) {
            String dateKey = item.dtTxt.substring(0, 10); // "yyyy-MM-dd"
            double temp = item.main.temp;

            if (!dailyMinMax.containsKey(dateKey)) {
                // Ngày mới: khởi tạo
                dailyMinMax.put(dateKey, new double[]{temp, temp});
                dailyIcon.put(dateKey, item.weather.get(0).icon);
                dailyPop.put(dateKey, item.pop);
                try {
                    Date date = inputFormat.parse(item.dtTxt);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    String name = (cal.get(Calendar.DAY_OF_YEAR) == currentDay) ? "Hôm nay" :
                            (cal.get(Calendar.DAY_OF_YEAR) == currentDay + 1) ? "Ngày mai" :
                                    formatVietnameseDay(dayFormat.format(date));
                    dailyLabel.put(dateKey, dateFormat.format(date) + " " + name);
                } catch (Exception e) { e.printStackTrace(); }
            } else {
                // Cập nhật min/max thực tế
                double[] minMax = dailyMinMax.get(dateKey);
                if (temp < minMax[0]) minMax[0] = temp;
                if (temp > minMax[1]) minMax[1] = temp;
                // Ưu tiên icon lúc 12h trưa làm đại diện cho ngày
                if (item.dtTxt.contains("12:00:00")) {
                    dailyIcon.put(dateKey, item.weather.get(0).icon);
                }
                // Lấy xác suất mưa cao nhất trong ngày
                if (item.pop > dailyPop.get(dateKey)) {
                    dailyPop.put(dateKey, item.pop);
                }
            }
        }

        List<DailyWeather> dailyData = new ArrayList<>();
        for (String dateKey : dailyLabel.keySet()) {
            double[] minMax = dailyMinMax.get(dateKey);
            dailyData.add(new DailyWeather(
                    dailyLabel.get(dateKey),
                    Math.round(minMax[0]) + "° / " + Math.round(minMax[1]) + "°",
                    Math.round(dailyPop.get(dateKey) * 100) + "%",
                    dailyIcon.get(dateKey)));
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
            // Vừa xin quyền, vừa tải thời tiết cho thành phố mặc định ngay lập tức
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            String savedCity = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString(KEY_LAST_CITY, "Ho Chi Minh");
            fetchWeatherByCity(savedCity);
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
            if (loc != null) {
                fetchWeatherByGPS(loc.getLatitude(), loc.getLongitude());
            } else {
                // Dùng thành phố đã lưu lần trước, mặc định là Hồ Chí Minh
                String savedCity = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .getString(KEY_LAST_CITY, "Ho Chi Minh");
                fetchWeatherByCity(savedCity);
            }
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
