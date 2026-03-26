package com.example.goldtracker;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Intent;
import android.widget.RelativeLayout;

public class CityManagementActivity extends AppCompatActivity {

    private EditText etSearchCity;
    private TextView tvCityNameItem, tvCityTempItem;
    private RelativeLayout layoutCityResult;
    private String currentSearchedCity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_management);

        // 1. Ánh xạ View
        ImageView ivBack = findViewById(R.id.ivBack);
        etSearchCity = findViewById(R.id.etSearchCity);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        tvCityNameItem = findViewById(R.id.tvCityNameItem);
        tvCityTempItem = findViewById(R.id.tvCityTempItem);

        // 1. Ánh xạ thẻ kết quả
        layoutCityResult = findViewById(R.id.layoutCityResult);

        // 2. Bắt sự kiện khi người dùng bấm vào thẻ kết quả
        layoutCityResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentSearchedCity.isEmpty()) {
                    // Gói tên thành phố vào Intent để gửi về MainActivity
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("SELECTED_CITY", currentSearchedCity);
                    setResult(RESULT_OK, returnIntent);
                    finish(); // Đóng màn hình này lại
                }
            }
        });

        // 2. Nút Back (Mũi tên quay lại)
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 3. Bấm nút Dấu cộng để tìm kiếm
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = etSearchCity.getText().toString().trim();
                if (!cityName.isEmpty()) {
                    searchWeatherForCity(cityName);
                } else {
                    Toast.makeText(CityManagementActivity.this, "Vui lòng nhập tên thành phố!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void searchWeatherForCity(String cityName) {
        String apiKey = BuildConfig.WEATHER_API_KEY;

        RetrofitClient.getApiService().getWeatherByCityName(cityName, apiKey, "metric", "vi")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherData = response.body();

                            // Lưu lại tên thành phố chuẩn từ API trả về
                            currentSearchedCity = weatherData.cityName;

                            tvCityNameItem.setText(currentSearchedCity);
                            tvCityTempItem.setText(Math.round(weatherData.main.temp) + "°");
                            etSearchCity.setText("");
                        } else {
                            Toast.makeText(CityManagementActivity.this, "Không tìm thấy!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        Toast.makeText(CityManagementActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}