package com.example.coolweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.db.Utility;
import com.example.coolweather.util.HttpUtil;

import org.json.JSONException;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private List<String> list = new ArrayList<>();
    private ListView area_list;
    private List<Province> provinces;
    private List<City> cities;
    private List<County> counties;
    private TextView title;
    private Button back;
    private Province province;
    private City city;
    private County county;
    private int currentlevel;
    private ArrayAdapter<String> arrayAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        area_list = view.findViewById(R.id.area_list);
        title = view.findViewById(R.id.title_text);
        back = view.findViewById(R.id.back);
        arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, list);
        area_list.setAdapter(arrayAdapter);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        area_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentlevel == LEVEL_PROVINCE) {
                    province = provinces.get(position);
                    ToCities();
                } else if (currentlevel == LEVEL_CITY) {
                    city = cities.get(position);
                    ToCounties();
                }else if (currentlevel == LEVEL_COUNTY){
                    String weatherId = counties.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if (getActivity() instanceof  WeatherActivity){
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentlevel == LEVEL_COUNTY) {
                    ToCities();
                } else if (currentlevel == LEVEL_CITY) {
                    ToProvinces();
                }
            }
        });
        ToProvinces();
    }

    private void ToProvinces() {
        title.setText("中国");
        back.setVisibility(View.GONE);
        provinces = DataSupport.findAll(Province.class);
        if (provinces.size() > 0) {
            list.clear();
            for (Province province : provinces) {
                list.add(province.getProvinceName());
            }
            arrayAdapter.notifyDataSetChanged();
            area_list.setSelection(0);
            currentlevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            FromServer(address, "province");
        }

    }

    private void ToCities() {
        title.setText(province.getProvinceName());
        back.setVisibility(View.VISIBLE);
        cities = DataSupport.where("provinceid = ?", String.valueOf(province.getId())).find(City.class);
        if (cities.size() > 0) {
            list.clear();
            for (City city : cities) {
                list.add(city.getCityName());
            }
            arrayAdapter.notifyDataSetChanged();
            area_list.setSelection(0);
            currentlevel = LEVEL_CITY;
        } else {
            int provincecode = province.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provincecode;
            FromServer(address, "city");
        }
    }

    private void ToCounties() {
        title.setText(city.getCityName());
        back.setVisibility(View.VISIBLE);
        counties = DataSupport.where("cityid = ?", String.valueOf(city.getId())).find(County.class);
        if (counties.size() > 0) {
            list.clear();
            for (County county : counties) {
                list.add(county.getCountyName());
            }
            arrayAdapter.notifyDataSetChanged();
            area_list.setSelection(0);
            currentlevel = LEVEL_COUNTY;
        } else {
            int provincecode = province.getProvinceCode();
            int citycode = city.getCityCode();
            String address = "http://guolin.tech/api/china/" + provincecode + "/" + citycode;
            FromServer(address, "county");
        }

    }

    private void FromServer(String address, final String type) {
        showProgress();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgress();
                        Log.e("MAINAVTI=========",e.toString());
                        Toast.makeText(getContext(), "加载失败<.>", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,province.getId());
                }else if ("county".equals(type)){
                    try {
                        result = Utility.handleCountyResponse(responseText,city.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (result){
                    //切换主线程更新界面
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgress();
                            if ("province".equals(type)){
                                ToProvinces();
                            }
                            else if ("city".equals(type)){
                                ToCities();
                            }else if ("county".equals(type)){
                                ToCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("拼命加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
