package com.zrq.mapdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.LocationSource.OnLocationChangedListener;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.AlphaAnimation;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Context context;
    private MapView mMapView;
    //???????????????
    private static final int REQUEST_PERMISSIONS = 1;
    private AMapLocationClient mLocationClient;
    private AMap aMap;
    private OnLocationChangedListener mListener;
    private PoiSearch.Query query;
    private PoiSearch poiSearch;
    private String cityCode;
    private FloatingActionButton fabPOI;
    private FloatingActionButton fabClear;
    private FloatingActionButton fabRoute;
    private EditText mEtSearch;
    //??????????????????
    private GeocodeSearch geocodeSearch;
    private static final int PARSE_SUCCESS_CODE = 1000;
    private String city;
    private List<Marker> markerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        initView();
        initData(savedInstanceState);
        checkAndroidVersion();
        initEvent();
    }

    private void initView() {
        mMapView = findViewById(R.id.map_view);
        fabPOI = findViewById(R.id.fab_poi);
        fabClear = findViewById(R.id.fab_clear);
        fabRoute = findViewById(R.id.fab_route);
        mEtSearch = findViewById(R.id.et_search);
    }

    private void initData(Bundle savedInstanceState) {
        markerList = new ArrayList<>();
        initMap(savedInstanceState);
        initLocation();
        try {
            geocodeSearch = new GeocodeSearch(context);
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    //???????????????
    private void initMap(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMap.setMinZoomLevel(12);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.azi_2));
        myLocationStyle.strokeColor(Color.argb(0x55, 0xB8, 0xAF, 0xDC));
        myLocationStyle.strokeWidth(5f);
        myLocationStyle.radiusFillColor(Color.argb(0x55, 0x72, 0x58, 0xE1));
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setLocationSource(new LocationSource() {
            @Override
            public void activate(OnLocationChangedListener onLocationChangedListener) {
                mListener = onLocationChangedListener;
                if (mLocationClient != null) {
                    mLocationClient.startLocation();
                }
            }

            @Override
            public void deactivate() {
                mListener = null;
                if (mLocationClient != null) {
                    mLocationClient.stopLocation();
                    mLocationClient.onDestroy();
                }
                mLocationClient = null;
            }
        });
        aMap.setMyLocationEnabled(true);
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setScaleControlsEnabled(true);
    }

    //???????????????
    private void initLocation() {
        try {
            AMapLocationClient.updatePrivacyShow(context, true, true);
            AMapLocationClient.updatePrivacyAgree(context, true);
            mLocationClient = new AMapLocationClient(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //?????????AMapLocationClientOption??????
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        //?????????????????????AMapLocationMode.High_Accuracy??????????????????
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //????????????3s????????????????????????????????????
        mLocationOption.setOnceLocationLatest(true);
        //????????????????????????????????????????????????????????????
        mLocationOption.setNeedAddress(true);
        //?????????????????????????????????????????????????????????30000???????????????????????????????????????8000??????
        mLocationOption.setHttpTimeOut(20_000);
        //???????????????????????????????????????????????????
        mLocationOption.setLocationCacheEnable(false);
        //??????????????????????????????????????????
        mLocationClient.setLocationOption(mLocationOption);
    }

    private void initEvent() {
        //???????????????????????????
        fabPOI.setOnClickListener(v -> {
            query = new PoiSearch.Query("??????", "", cityCode);
            query.setPageSize(10);
            query.setPageNum(1);
            try {
                poiSearch = new PoiSearch(this, query);
                poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                    @Override
                    public void onPoiSearched(PoiResult poiResult, int i) {
                        //??????POI????????????
                        ArrayList<PoiItem> poiItems = poiResult.getPois();
                        for (PoiItem poiItem : poiItems) {
                            Log.d("MainActivity", " Title???" + poiItem.getTitle() + " Snippet???" + poiItem.getSnippet());
                        }
                    }

                    @Override
                    public void onPoiItemSearched(PoiItem poiItem, int i) {

                    }
                });
                poiSearch.searchPOIAsyn();
            } catch (AMapException e) {
                e.printStackTrace();
            }
        });
        fabClear.setOnClickListener(v -> {
            for (Marker marker : markerList) {
                marker.remove();
            }
            markerList.clear();
            fabClear.hide();
        });
        fabRoute.setOnClickListener(v -> {
            startActivity(new Intent(context, RouteActivity.class));
        });
        //????????????????????????
        aMap.setOnMapClickListener((latLng) -> {
            addMarker(latLng);
            updateMapCenter(latLng);
        });
        aMap.setOnMapLongClickListener(this::latLngToAddress);
        //???????????????????????????
        aMap.setOnMarkerClickListener((Marker marker) -> {
            Toast.makeText(context, "???????????????", Toast.LENGTH_SHORT).show();
            return true;
        });
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                Log.d(TAG, "onMarkerDragStart");
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                Log.d(TAG, "onMarkerDrag");
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Log.d(TAG, "onMarkerDragEnd");
            }
        });
        //????????????????????????
        mLocationClient.setLocationListener((aMapLocation) -> {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    String sb
                            = "?????????" +
                            aMapLocation.getLatitude() +
                            "?????????" +
                            aMapLocation.getLongitude() +
                            "?????????" +
                            aMapLocation.getAddress();
                    Log.d(TAG, "initLocation: " + sb);
                    mLocationClient.stopLocation();
                    if (mListener != null) {
                        mListener.onLocationChanged(aMapLocation);
                    }
                    fabPOI.show();
                    cityCode = aMapLocation.getCityCode();
                    city = aMapLocation.getCity();
                } else {
                    Log.e(TAG, "location Error, Error Code : " + aMapLocation.getErrorCode() +
                            ", Error Info : " + aMapLocation.getErrorInfo());
                }
            }
        });
        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            //???????????????
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                if (i == PARSE_SUCCESS_CODE) {
                    RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                    Toast.makeText(context, "??????" + regeocodeAddress.getFormatAddress(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
                }
            }

            //???????????????
            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
                if (i == PARSE_SUCCESS_CODE) {
                    List<GeocodeAddress> geocodeAddressList = geocodeResult.getGeocodeAddressList();
                    if (geocodeAddressList != null && geocodeAddressList.size() > 0) {
                        LatLonPoint latLonPoint = geocodeAddressList.get(0).getLatLonPoint();
                        Toast.makeText(context, "??????" + latLonPoint.getLongitude() + ", "
                                + latLonPoint.getLatitude(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        mEtSearch.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                String address = mEtSearch.getText().toString().trim();
                if (address.isEmpty()) {
                    Toast.makeText(context, "???????????????", Toast.LENGTH_SHORT).show();
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                    GeocodeQuery query = new GeocodeQuery(address, city);
                    geocodeSearch.getFromLocationNameAsyn(query);
                }
                return true;
            }
            return false;
        });
    }

    private void latLngToAddress(LatLng latLng) {
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 20, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
    }

    /**
     * ????????????
     *
     * @param latLng ?????????
     */
    private void addMarker(LatLng latLng) {
        fabClear.show();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions
                .position(latLng)
                .draggable(true)
                .title("??????")
                .snippet("????????????");
        Marker marker = aMap.addMarker(markerOptions);
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.azi_2));
        Animation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(300);
        animation.setInterpolator(new LinearInterpolator());
        marker.setAnimation(animation);
        marker.startAnimation();
        markerList.add(marker);
    }

    /**
     * ?????????????????????
     *
     * @param latLng ?????????
     */
    private void updateMapCenter(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition(latLng, 16, 30, 0);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        aMap.animateCamera(cameraUpdate);
    }

    @SuppressLint("ObsoleteSdkInt")
    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        } else {
            mLocationClient.startLocation();
        }
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (EasyPermissions.hasPermissions(context, permissions)) {
            Toast.makeText(context, "???????????????", Toast.LENGTH_SHORT).show();
            mLocationClient.startLocation();
        } else {
            EasyPermissions.requestPermissions(this, "????????????", REQUEST_PERMISSIONS, permissions);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
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
        mLocationClient.onDestroy();
        mMapView.onDestroy();
    }
}