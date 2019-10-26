package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
   
    @SerializedName("update")
    public Update update;
    public String status;
    @SerializedName("now")
    public Now now;
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
    @SerializedName("lifestyle")
    public List<Suggestion> suggestionList;
}
