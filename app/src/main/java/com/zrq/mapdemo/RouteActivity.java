package com.zrq.mapdemo;

import static com.zrq.mapdemo.utils.MapUtil.convertToLatLng;
import static com.zrq.mapdemo.utils.MapUtil.convertToLatLonPoint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.LocationSource.OnLocationChangedListener;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.zrq.mapdemo.overlay.BusRouteOverlay;
import com.zrq.mapdemo.overlay.DrivingRouteOverlay;
import com.zrq.mapdemo.overlay.RideRouteOverlay;
import com.zrq.mapdemo.overlay.WalkRouteOverlay;
import com.zrq.mapdemo.utils.MapUtil;

public class RouteActivity extends AppCompatActivity {

    private static final String TAG = "RouteActivity";
    private Context context;
    private MapView mMapView;
    private AMapLocationClient mLocationClient;
    private AMap aMap;
    private OnLocationChangedListener mLocationChangeListener;
    private LatLonPoint mStartPoint;
    private LatLonPoint mEndPoint;
    private RouteSearch routeSearch;
    private Spinner mSpTravelMode;
    private static final String[] travelModeArray = {"????????????", "????????????", "????????????", "????????????"};
    private static int TRAVEL_MODE = 0;
    private String city;
    private TextView mTvTime;
    private ExtendedFloatingActionButton mFabDetail;

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
        mSpTravelMode = findViewById(R.id.spinner_travel_mode);
        mTvTime = findViewById(R.id.tv_time);
        mFabDetail = findViewById(R.id.fab_detail);
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
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, travelModeArray);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpTravelMode.setAdapter(arrayAdapter);
    }

    private void initEvent() {
        mSpTravelMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TRAVEL_MODE = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //???????????????????????????
        mLocationClient.setLocationListener((AMapLocation aMapLocation) -> {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    String address = aMapLocation.getAddress();
                    double latitude = aMapLocation.getLatitude();
                    double longitude = aMapLocation.getLongitude();
                    Log.d(TAG, "address: " + address);
                    Log.d(TAG, "latitude: " + latitude);
                    Log.d(TAG, "longitude: " + longitude);
                    //???????????????
                    mStartPoint = new LatLonPoint(latitude, longitude);
                    //?????????????????????????????????????????????
                    mLocationClient.stopLocation();
                    if (mLocationChangeListener != null) {
                        mLocationChangeListener.onLocationChanged(aMapLocation);
                    }
                    city = aMapLocation.getCity();
                } else {
                    Log.d(TAG, "location error, error code : " + aMapLocation.getErrorCode() +
                            ", error info : " + aMapLocation.getErrorInfo());
                }
            }
        });
        //??????????????????
        aMap.setOnMapClickListener((LatLng latLng) -> {
            mEndPoint = convertToLatLonPoint(latLng);
            startRouteSearch();
        });

        routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            //????????????????????????
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {
                aMap.clear();// ?????????????????????????????????
                if (i == AMapException.CODE_AMAP_SUCCESS) {
                    if (busRouteResult != null && busRouteResult.getPaths() != null) {
                        if (busRouteResult.getPaths().size() > 0) {
                            final BusPath busPath = busRouteResult.getPaths().get(0);
                            if (busPath == null) {
                                return;
                            }
                            BusRouteOverlay busRouteOverlay = new BusRouteOverlay(
                                    context, aMap, busPath,
                                    busRouteResult.getStartPos(),
                                    busRouteResult.getTargetPos());
                            busRouteOverlay.removeFromMap();
                            busRouteOverlay.addToMap();
                            busRouteOverlay.zoomToSpan();
                            busRouteOverlay.setNodeIconVisibility(false);
                            int dis = (int) busPath.getDistance();
                            int dur = (int) busPath.getDuration();
                            String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                            Log.d(TAG, des);
                        } else if (busRouteResult.getPaths() == null) {
                            Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "?????????" + i, Toast.LENGTH_SHORT).show();
                }
            }

            //????????????????????????
            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
                aMap.clear();// ?????????????????????????????????
                if (i == AMapException.CODE_AMAP_SUCCESS) {
                    if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                        if (driveRouteResult.getPaths().size() > 0) {
                            final DrivePath drivePath = driveRouteResult.getPaths()
                                    .get(0);
                            if (drivePath == null) {
                                return;
                            }
                            DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                                    context, aMap, drivePath,
                                    driveRouteResult.getStartPos(),
                                    driveRouteResult.getTargetPos(), null);
                            drivingRouteOverlay.removeFromMap();
                            drivingRouteOverlay.addToMap();
                            drivingRouteOverlay.zoomToSpan();
                            drivingRouteOverlay.setNodeIconVisibility(false);
                            int dis = (int) drivePath.getDistance();
                            int dur = (int) drivePath.getDuration();
                            String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                            Log.d(TAG, des);
                        } else if (driveRouteResult.getPaths() == null) {
                            Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "?????????" + i, Toast.LENGTH_SHORT).show();
                }
            }

            //????????????????????????
            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {
                aMap.clear();
                if (i == AMapException.CODE_AMAP_SUCCESS) {
                    if (walkRouteResult != null && walkRouteResult.getPaths() != null) {
                        if (walkRouteResult.getPaths().size() > 0) {
                            final WalkPath walkPath = walkRouteResult.getPaths().get(0);
                            if (walkPath == null) {
                                return;
                            }
                            WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(context, aMap,
                                    walkPath, walkRouteResult.getStartPos(), walkRouteResult.getTargetPos());
                            walkRouteOverlay.removeFromMap();
                            walkRouteOverlay.addToMap();
                            walkRouteOverlay.zoomToSpan();
                            walkRouteOverlay.setNodeIconVisibility(false);
                            int dis = (int) walkPath.getDistance();
                            int dur = (int) walkPath.getDuration();
                            String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                            Log.d(TAG, des);
                            mTvTime.setText(des);
                            mFabDetail.setOnClickListener(v -> {
                                Intent intent = new Intent(context, RouteDetailActivity.class);
                                intent.putExtra("type", 0);
                                intent.putExtra("path", walkPath);
                                startActivity(intent);
                            });
                        } else if (walkRouteResult.getPaths() == null) {
                            Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "?????????" + i, Toast.LENGTH_SHORT).show();
                }
            }

            //????????????????????????
            @Override
            public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {
                aMap.clear();
                if (i == AMapException.CODE_AMAP_SUCCESS) {
                    if (rideRouteResult != null && rideRouteResult.getPaths() != null) {
                        if (rideRouteResult.getPaths().size() > 0) {
                            final RidePath ridePath = rideRouteResult.getPaths().get(0);
                            if (ridePath == null) return;
                            RideRouteOverlay rideRouteOverlay = new RideRouteOverlay(context, aMap, ridePath, mStartPoint, mEndPoint);
                            rideRouteOverlay.removeFromMap();
                            rideRouteOverlay.addToMap();
                            rideRouteOverlay.zoomToSpan();
                            rideRouteOverlay.setNodeIconVisibility(false);
                            int dis = (int) ridePath.getDistance();
                            int dur = (int) ridePath.getDuration();
                            String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                            Log.d(TAG, des);
                        } else if (rideRouteResult.getPaths() == null) {
                            Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "??????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "?????????" + i, Toast.LENGTH_SHORT).show();
                }
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
        option.setOnceLocationLatest(true);
        option.setNeedAddress(true);
        option.setHttpTimeOut(20_000);
        option.setLocationCacheEnable(false);
        mLocationClient.setLocationOption(option);
    }

    private void initMap(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        //???????????????????????????16 ????????????????????????[3, 20]
        aMap.setMinZoomLevel(12);
        //??????????????????
//        aMap.showIndoorMap(true);
        //?????????UiSettings?????????
        UiSettings mUiSettings = aMap.getUiSettings();
        //?????????????????? ????????????
        mUiSettings.setZoomControlsEnabled(false);
        //??????????????? ???????????????
        mUiSettings.setScaleControlsEnabled(true);
        // ?????????????????????
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_1));
        //???????????????Style
        aMap.setMyLocationStyle(myLocationStyle);

        // ??????????????????
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
        // ?????????true??????????????????????????????????????????false??????????????????????????????????????????????????????false
        aMap.setMyLocationEnabled(true);
    }

    private void startRouteSearch() {
        aMap.addMarker(new MarkerOptions()
                .position(convertToLatLng(mStartPoint))
                .title("??????")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_1)));
        aMap.addMarker(new MarkerOptions()
                .position(convertToLatLng(mEndPoint))
                .title("??????")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.azi_2)));
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(mStartPoint, mEndPoint);
        switch (TRAVEL_MODE) {
            case 0:
                RouteSearch.WalkRouteQuery walkRouteQuery = new RouteSearch.WalkRouteQuery(fromAndTo);
                routeSearch.calculateWalkRouteAsyn(walkRouteQuery);
                break;
            case 1:
                RouteSearch.RideRouteQuery rideRouteQuery = new RouteSearch.RideRouteQuery(fromAndTo);
                routeSearch.calculateRideRouteAsyn(rideRouteQuery);
                break;
            case 2:
                RouteSearch.DriveRouteQuery driveRouteQuery = new RouteSearch.DriveRouteQuery(fromAndTo,
                        RouteSearch.DRIVING_SINGLE_DEFAULT, null, null, "");
                routeSearch.calculateDriveRouteAsyn(driveRouteQuery);
            case 3:
                //?????????????????????????????? ?????????????????????????????????????????????????????????????????????????????????????????????0???????????????,1????????????
                RouteSearch.BusRouteQuery busRouteQuery = new RouteSearch.BusRouteQuery(fromAndTo,
                        RouteSearch.BUS_LEASE_WALK, city, 0);
                routeSearch.calculateBusRouteAsyn(busRouteQuery);
            default:
                break;
        }
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