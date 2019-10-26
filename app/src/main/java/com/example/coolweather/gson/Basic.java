package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    @SerializedName("location")
    public String cityname;

    @SerializedName("cid")
    public String weatherId;
}
