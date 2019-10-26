package com.example.coolweather;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.db.Utility;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Suggestion;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.Code;
import interfaces.heweather.com.interfacesmodule.bean.Lang;
import interfaces.heweather.com.interfacesmodule.bean.Unit;
import interfaces.heweather.com.interfacesmodule.bean.air.Air;
import interfaces.heweather.com.interfacesmodule.bean.air.now.AirNow;
import interfaces.heweather.com.interfacesmodule.bean.weather.Weather;
import interfaces.heweather.com.interfacesmodule.bean.weather.now.Now;
import interfaces.heweather.com.interfacesmodule.bean.weather.now.NowBase;
import interfaces.heweather.com.interfacesmodule.view.HeConfig;
import interfaces.heweather.com.interfacesmodule.view.HeWeather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button navbutton;
    List<Forecast> list = new ArrayList<>();
    private static final String TAG = "WeatherActivity";
    String jsonnow;
    String jsonbasic;
    String jsonupdate;
    String aqilevel;
    String jsonweather;
    JSONObject jsonObject;
    JSONObject jsonObject1;
    JSONObject jsonObject2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HeConfig.init("HE1910250805181661","813d7a9f66b44efe92c18a0d6bd7a127");
        HeConfig.switchToFreeServerNode();
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21){
            View decorview = getWindow().getDecorView();
            decorview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = findViewById(R.id.drawer_layout);
        navbutton = findViewById(R.id.nav_button);

        //先查询一波本地是否保存
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String weatherString = sharedPreferences.getString("weather", null);
        if (weatherString != null) {
            requestWeather(weatherString);
        } else {
            mWeatherId = getIntent().getStringExtra("weather");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        String bingPic = sharedPreferences.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        navbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        list.clear();
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });

    }


    public void requestWeather(final String weatherId) {

        HeWeather.getWeatherNow(WeatherActivity.this, weatherId, Lang.CHINESE_SIMPLIFIED, Unit.IMPERIAL, new HeWeather.OnResultWeatherNowBeanListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "onErrornow: "+throwable );
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onSuccess(Now now) {
                Log.e(TAG, "onSuccess: "+now.getStatus());
                    Log.i(TAG, " Weather Now onSuccess: " + new Gson().toJson(now));
                    jsonnow = new Gson().toJson(now.getNow());
                    jsonbasic = new Gson().toJson(now.getBasic());
                    jsonupdate = new Gson().toJson(now.getUpdate());
                    try {
                        jsonObject = new JSONObject(jsonnow);
                        jsonObject1 = new JSONObject(jsonbasic);
                        jsonObject2 = new JSONObject(jsonupdate);
                        String cityname = jsonObject1.getString("location");
                        @SuppressLint("DefaultLocale") String tep = String.format("%.0f",(jsonObject.getDouble("tmp")-32)/1.8);
                        String cityid = jsonObject1.getString("cid");
                        String updatetime = jsonObject2.getString("loc").substring(10,16);
                        String wind = jsonObject.getString("wind_dir");
                        showWeatherInfo(cityname, tep,updatetime,wind);
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                        editor.putString("weather",cityid);
                        editor.apply();
                        swipeRefresh.setRefreshing(false);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        swipeRefresh.setRefreshing(false);

                    }


            }

        });

        HeWeather.getAir(WeatherActivity.this, weatherId, Lang.CHINESE_TRADITIONAL, Unit.IMPERIAL, new HeWeather.OnResultAirBeanListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "onError: "+throwable );
            }

            @Override
            public void onSuccess(Air air) {
                Log.e(TAG, "onSuccess: "+new Gson().toJson(air));
                aqilevel = new Gson().toJson(air.getAir_now_city());
                try {
                    JSONObject jsonObject = new JSONObject(aqilevel);
                    String airlevel = jsonObject.getString("aqi");
                    String pm25 = jsonObject.getString("pm25");
                    showWeatherAir(airlevel,pm25);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        HeWeather.getWeather(WeatherActivity.this, weatherId, Lang.CHINESE_TRADITIONAL, Unit.IMPERIAL, new HeWeather.OnResultWeatherDataListBeansListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "onErrorweather: "+throwable );
            }

            @Override
            public void onSuccess(Weather weather) {
                Log.e(TAG, "onSuccess:weather "+new Gson().toJson(weather) );
                jsonweather = new Gson().toJson(weather.getDaily_forecast());
                try {
                    JSONArray jsonArray = new JSONArray(jsonweather);
                    list.clear();
                    forecastLayout.removeAllViews();
                    for (int i=0;i<jsonArray.length();i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        list.add(new Forecast(jsonObject.getString("date"),jsonObject.getString("cond_txt_d"),String.format("%.0f",(jsonObject.getDouble("tmp_max")-32)/1.8)+"℃",String.format("%.0f",(jsonObject.getDouble("tmp_min")-32)/1.8)+"℃"));
                        Log.e(TAG, "onSuccess: "+list.size() );
                        //Log.e(TAG, "onSuccess: "+list.get(i).getCondition() );
                        showWeatherForeCast();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }
    private void showWeatherForeCast(){
        //
        View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
        for (int i=0;i<list.size();i++){
            Forecast forecast = list.get(i);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.getDate());
            infoText.setText(forecast.condition);
            maxText.setText(forecast.getTemperature_max());
            minText.setText(forecast.temperature_min);
        }
        forecastLayout.addView(view);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);


    }
    private void showWeatherInfo(String cityname,String tep,String update,String wind) {
        String cityName = cityname;
//        把一个字符串分割开并获取字符串中第二个元素的值
        String updateTime = update;
        String degree = tep + "℃";
        String weatherInfo = wind;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);


    }
    private void showWeatherAir(String airlevel,String pm25){
        aqiText.setText(airlevel);
        pm25Text.setText(pm25);
    }

}
