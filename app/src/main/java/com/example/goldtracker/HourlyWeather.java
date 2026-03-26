package com.example.goldtracker;

public class HourlyWeather {
    private String time;
    private String temperature;
    private String iconCode; //

    public HourlyWeather(String time, String temperature, String iconCode) {
        this.time = time;
        this.temperature = temperature;
        this.iconCode = iconCode;
    }

    public String getTime() { return time; }
    public String getTemperature() { return temperature; }
    public String getIconCode() { return iconCode; }
}