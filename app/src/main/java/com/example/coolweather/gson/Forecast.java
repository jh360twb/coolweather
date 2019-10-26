package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Forecast {
    public String date;
    public String condition;
    public String temperature_max;
    public String temperature_min;
    public Forecast(String date,String condition,String temperature_max,String temperature_min){
        this.date = date;
        this.condition = condition;
        this.temperature_max = temperature_max;
        this.temperature_min = temperature_min;
    }

    public String getDate() {
        return date;
    }

    public String getCondition() {
        return condition;
    }

    public String getTemperature_max() {
        return temperature_max;
    }

    public String getTemperature_min() {
        return temperature_min;
    }
}
