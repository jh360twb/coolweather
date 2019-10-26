package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Suggestion {
    @SerializedName("type")
    public String type;
    @SerializedName("brf")
    public String feel;
    @SerializedName("txt")
    public String feel_detail;
}
