package com.example.goldtracker;

public class DailyWeather {
    private String dayName;
    private String minMaxTemp;
    private String rainProb;
    private String iconCode;

    public DailyWeather(String dayName, String minMaxTemp, String rainProb, String iconCode) {
        this.dayName = dayName;
        this.minMaxTemp = minMaxTemp;
        this.rainProb = rainProb;
        this.iconCode = iconCode;
    }

    public String getDayName() { return dayName; }
    public String getMinMaxTemp() { return minMaxTemp; }
    public String getRainProb() { return rainProb; }
    public String getIconCode() { return iconCode; }
}
