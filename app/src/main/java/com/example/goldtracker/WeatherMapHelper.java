package com.example.goldtracker;

import android.annotation.SuppressLint;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Tạo HTML bản đồ thời tiết Leaflet + OWM tiles, dùng chung cho preview và full screen.
 */
public class WeatherMapHelper {

    @SuppressLint("SetJavaScriptEnabled")
    public static void setupWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setWebViewClient(new WebViewClient());
    }

    public static void load(WebView webView, double lat, double lon, String apiKey) {
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
                + "    'https://tile.openweathermap.org/map/'+layer+'/{z}/{x}/{y}.png?appid=" + apiKey + "',"
                + "    {opacity:0.85,attribution:'© OpenWeatherMap',zIndex:500}).addTo(map);"
                + "  labelLayer.bringToFront();"
                + "}"
                + "setLayer('temp_new',document.querySelector('.btn.active'));"
                + "</script></body></html>";
        webView.loadDataWithBaseURL("https://openweathermap.org", html, "text/html", "UTF-8", null);
    }
}
