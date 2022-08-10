package com.zrq.mapdemo;

import static com.zrq.mapdemo.utils.MapUtil.convertToLatLng;
import static com.zrq.mapdemo.utils.MapUtil.convertToLatLonPoint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;

public class RouteActivity extends AppCompatActivity {

    private static final String TAG = "RouteActivity";
    private Context context;
    private MapView mMapView;
    private AMapLocationClient mLocationClient;
    private AMap aMap;
    private LocationSource.OnLocationChangedListener mLocationChangeListener;
    private LatLonPoint mStartPoint;
    private LatLonPoint mEndPoint;
    private RouteSearch routeSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_route);
        initView();
        initData(savedInstanceState);
        initEvent();
    }

    private void initView() {
        mMapView = findViewById(R.id.map_view_route);
    }

    private void initData(Bundle savedInstanceState) {
        initLocation();
        initMap(savedInstanceState);
        mLocationClient.startLocation();
        try {
            routeSearch = new RouteSearch(context);
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    private void initEvent() {
        //本地定位服务的监听
        mLocationClient.setLocationListener((AMapLocation aMapLocation) -> {
            if (aMapLocation.getErrorCode() == 0) {
                String address = aMapLocation.getAddress();
                double latitude = aMapLocation.getLatitude();
                double longitude = aMapLocation.getLongitude();
                Log.d(TAG, "address: " + address);
                Log.d(TAG, "latitude: " + latitude);
                Log.d(TAG, "longitude: " + longitude);
                //初始化起点
                mStartPoint = new LatLonPoint(latitude, longitude);
                //停止定位并不会销毁本地定位服务
                mLocationClient.stopLocation();
                mLocationChangeListener.onLocationChanged(aMapLocation);
            } else {
                Log.d(TAG, "location error, error code : " + aMapLocation.getErrorCode() +
                        ", error info : " + aMapLocation.getErrorInfo());
            }
        });
        // 设置定位监听
        aMap.setLocationSource(new LocationSource() {
            @Override
            public void activate(OnLocationChangedListener onLocationChangedListener) {
                mLocationChangeListener = onLocationChangedListener;
                if (mLocationClient != null) {
                    mLocationClient.startLocation();
                }
            }

            @Override
            public void deactivate() {
                mLocationChangeListener = null;
                if (mLocationClient != null) {
                    mLocationClient.stopLocation();
                    mLocationClient.onDestroy();
                }
                mLocationClient = null;
            }
        });
        //地图点击监听
        aMap.setOnMapClickListener((LatLng latLng) -> {
            mEndPoint = convertToLatLonPoint(latLng);
            startRouteSearch();
        });
        routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

            }

            //步行规划路径结果
            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

            }

            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

            }
        });
    }

    private void initLocation() {
        try {
            AMapLocationClient.updatePrivacyShow(context, true, true);
            AMapLocationClient.updatePrivacyAgree(context, true);
            mLocationClient = new AMapLocationClient(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        AMapLocationClientOption option = new AMapLocationClientOption();
        option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        option.setOnceLocation(true);
        option.setNeedAddress(true);
        option.setHttpTimeOut(20_000);
        option.setLocationCacheEnable(false);
        mLocationClient.setLocationOption(option);
    }

    private void initMap(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        //设置最小缩放等级为16 ，缩放级别范围为[3, 20]
        aMap.setMinZoomLevel(16);
        //开启室内地图
        aMap.showIndoorMap(true);
        //实例化UiSettings类对象
        UiSettings mUiSettings = aMap.getUiSettings();
        //隐藏缩放按钮 默认显示
        mUiSettings.setZoomControlsEnabled(false);
        //显示比例尺 默认不显示
        mUiSettings.setScaleControlsEnabled(true);
        // 自定义定位图标
        MyLocationStyle myLocationStyle = new MyLocationStyle();
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.azi_2));
        //设置定位的Style
        aMap.setMyLocationStyle(myLocationStyle);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
    }

    private void startRouteSearch() {
        aMap.addMarker(new MarkerOptions()
                .position(convertToLatLng(mStartPoint))
                .title("起点")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.azi_2)));
        aMap.addMarker(new MarkerOptions()
                .position(convertToLatLng(mEndPoint))
                .title("终点")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.azi_2)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }
}