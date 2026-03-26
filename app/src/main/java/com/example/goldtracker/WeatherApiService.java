package com.example.goldtracker;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("data/2.5/weather")
    Call<WeatherResponse> getCurrentWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );

    // API lấy dự báo 5 ngày / mỗi 3 giờ
    @GET("data/2.5/forecast")
    Call<ForecastResponse> getForecast(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );

    // Lấy thời tiết theo Tên thành phố
    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeatherByCityName(
            @Query("q") String cityName,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );

    // Lấy dự báo 5 ngày theo Tên thành phố
    @GET("data/2.5/forecast")
    Call<ForecastResponse> getForecastByCityName(
            @Query("q") String cityName,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
}
