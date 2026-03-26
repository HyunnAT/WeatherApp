package com.example.goldtracker;

public class WeatherUtils {
    public static int getLocalIcon(String iconCode) {
        if (iconCode == null) return R.drawable.ic_sunny;

        switch (iconCode) {
            case "01d": return R.drawable.ic_sunny;
            case "01n": return R.drawable.ic_clear_night;
            case "02d": return R.drawable.ic_cloudy_sunny;
            case "02n": return R.drawable.ic_cloudy_night;
            case "03d": case "03n":
            case "04d": case "04n": return R.drawable.ic_cloudy;
            case "09d": case "09n": return R.drawable.ic_shower_rain;
            case "10d": return R.drawable.ic_rainy_sunny;
            case "10n": return R.drawable.ic_rainy_night;
            case "11d": case "11n": return R.drawable.ic_thunderstorm;
            case "13d": case "13n": return R.drawable.ic_snow;
            case "50d": case "50n": return R.drawable.ic_mist;
            default: return R.drawable.ic_sunny;
        }
    }
}
