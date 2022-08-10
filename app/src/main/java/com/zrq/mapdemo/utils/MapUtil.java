package com.zrq.mapdemo.utils;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;

import java.text.DecimalFormat;

/**
 * 地图帮助类
 * @author llw
 */
public class MapUtil {

    /**
     * 把LatLng对象转化为LatLonPoint对象
     */
    public static LatLonPoint convertToLatLonPoint(LatLng latLng) {
        return new LatLonPoint(latLng.latitude, latLng.longitude);
    }

    /**
     * 把LatLonPoint对象转化为LatLon对象
     */
    public static LatLng convertToLatLng(LatLonPoint latLonPoint) {
        return new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude());
    }

    public static String getFriendlyTime(int second) {
        if (second > 3600) {
            int hour = second / 3600;
            int minute = (second % 3600) / 60;
            return hour + "小时" + minute + "分钟";
        }
        if (second >= 60) {
            int minute = second / 60;
            return minute + "分钟";
        }
        return second + "秒";
    }

    public static String getFriendlyLength(int lenMeter) {
        if (lenMeter > 10000) // 10 km
        {
            int dis = lenMeter / 1000;
            return dis + ChString.Kilometer;
        }

        if (lenMeter > 1000) {
            float dis = (float) lenMeter / 1000;
            DecimalFormat fnum = new DecimalFormat("##0.0");
            String dstr = fnum.format(dis);
            return dstr + ChString.Kilometer;
        }

        if (lenMeter > 100) {
            int dis = lenMeter / 50 * 50;   //保证50的倍数
            return dis + ChString.Meter;
        }

        int dis = lenMeter / 10 * 10;   //保证10的倍数
        if (dis == 0) {
            dis = 10;
        }

        return dis + ChString.Meter;
    }

}

