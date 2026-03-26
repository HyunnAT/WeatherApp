package com.example.goldtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HourlyAdapter extends RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder> {

    private List<HourlyWeather> hourlyList;

    public HourlyAdapter(List<HourlyWeather> hourlyList) {
        this.hourlyList = hourlyList;
    }

    @NonNull
    @Override
    public HourlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hourly_forecast, parent, false);
        return new HourlyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyViewHolder holder, int position) {
        HourlyWeather weather = hourlyList.get(position);
        holder.tvHour.setText(weather.getTime());
        holder.tvTemp.setText(weather.getTemperature());

        int iconId = WeatherUtils.getLocalIcon(weather.getIconCode());
        holder.imgIcon.setImageResource(iconId);
    }

    @Override
    public int getItemCount() {
        return hourlyList.size();
    }

    static class HourlyViewHolder extends RecyclerView.ViewHolder {
        TextView tvHour, tvTemp;
        ImageView imgIcon;

        public HourlyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHour = itemView.findViewById(R.id.tvHour);
            tvTemp = itemView.findViewById(R.id.tvHourlyTemp);
            imgIcon = itemView.findViewById(R.id.imgHourlyIcon);
        }
    }
}
