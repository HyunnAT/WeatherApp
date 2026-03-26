package com.example.goldtracker;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class DailyAdapter extends RecyclerView.Adapter<DailyAdapter.DailyViewHolder> {

    // Danh sách chứa dữ liệu thời tiết theo ngày
    private List<DailyWeather> dailyList;

    public DailyAdapter(List<DailyWeather> dailyList) {
        this.dailyList = dailyList;
    }


    @NonNull
    @Override
    public DailyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Trỏ đến file Layout item_daily_forecast.xml bạn đã tạo
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_forecast, parent, false);
        return new DailyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyViewHolder holder, int position) {
        DailyWeather weather = dailyList.get(position);

        holder.tvDayName.setText(weather.getDayName());
        holder.tvMinMaxTemp.setText(weather.getMinMaxTemp());

        int iconId = WeatherUtils.getLocalIcon(weather.getIconCode());
        holder.imgIcon.setImageResource(iconId);
    }

    @Override
    public int getItemCount() {
        return dailyList.size();
    }

    // Class ViewHolder để ánh xạ các view từ Layout XML
    public class DailyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvMinMaxTemp, tvRainProb;
        ImageView imgIcon;

        public DailyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDayName = itemView.findViewById(R.id.tvDailyDay);
            imgIcon = itemView.findViewById(R.id.imgDailyIcon);
            tvRainProb = itemView.findViewById(R.id.tvDailyRainProb);
            tvMinMaxTemp = itemView.findViewById(R.id.tvDailyTemp);
        }
    }
}
