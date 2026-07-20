package com.onedongua.radiodrive.source;

import android.content.Context;
import android.text.TextUtils;

import com.onedongua.radiodrive.Utils;
import com.onedongua.radiodrive.data.DataCategory;
import com.onedongua.radiodrive.source.qingting.QingtingDataSource;
import com.onedongua.radiodrive.station.DataRadioStation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * RadioBrowser 数据源实现。
 * 封装现有的 Utils.downloadFeedRelative + DataRadioStation.DecodeJson 逻辑。
 */
public class RadioBrowserDataSource implements RadioDataSource {

    public static final String SOURCE_TYPE = "radiobrowser";

    private final Context appContext;
    private final OkHttpClient httpClient;

    public RadioBrowserDataSource(Context appContext, OkHttpClient httpClient) {
        this.appContext = appContext;
        this.httpClient = httpClient;
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public void getCategories(CategoryType type, CategoryCallback callback) {
        String relativeUrl;
        switch (type) {
            case TAGS:
                relativeUrl = "json/tags";
                break;
            case COUNTRIES:
                relativeUrl = "json/countries";
                break;
            case LANGUAGES:
                relativeUrl = "json/languages";
                break;
            default:
                callback.onError(new UnsupportedOperationException(
                        "RadioBrowser does not support category type: " + type));
                return;
        }

        try {
            boolean showBroken = getShowBroken();
            Map<String, String> params = new HashMap<>();
            params.put("hidebroken", "" + (!showBroken));

            String result = Utils.downloadFeedRelative(httpClient, appContext, relativeUrl, false, params);
            if (result != null) {
                DataCategory[] categories = DataCategory.DecodeJson(result);
                callback.onResult(Arrays.asList(categories));
            } else {
                callback.onError(new Exception("Network error"));
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void getStationsByCategory(String categoryId, int page, StationsCallback callback) {
        // RadioBrowser uses different URL patterns per search style
        // This is for generic/flexible use
        try {
            String relativeUrl = "json/stations/search?name=";
            Map<String, String> params = new HashMap<>();
            params.put("order", "clickcount");
            params.put("reverse", "true");
            params.put("hidebroken", "" + (!getShowBroken()));

            String result = Utils.downloadFeedRelative(httpClient, appContext, relativeUrl, false, params);
            if (result != null) {
                List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                callback.onResult(stations, stations.size());
            } else {
                callback.onError(new Exception("Network error"));
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void search(String keyword, int page, StationsCallback callback) {
        try {
            String queryEncoded = URLEncoder.encode(keyword, "utf-8");
            queryEncoded = queryEncoded.replace("+", "%20");

            String relativeUrl = "json/stations/byname/" + queryEncoded;
            Map<String, String> params = new HashMap<>();
            params.put("order", "clickcount");
            params.put("reverse", "true");
            params.put("hidebroken", "" + (!getShowBroken()));

            String result = Utils.downloadFeedRelative(httpClient, appContext, relativeUrl, false, params);
            if (result != null) {
                List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                callback.onResult(stations, stations.size());
            } else {
                callback.onError(new Exception("Network error"));
            }
        } catch (UnsupportedEncodingException e) {
            callback.onError(e);
        }
    }

    @Override
    public void getFeaturedStations(int page, StationsCallback callback) {
        try {
            String relativeUrl = "json/stations/topclick";
            Map<String, String> params = new HashMap<>();
            params.put("hidebroken", "" + (!getShowBroken()));
            params.put("limit", "50");

            String result = Utils.downloadFeedRelative(httpClient, appContext, relativeUrl, false, params);
            if (result != null) {
                List<DataRadioStation> stations = DataRadioStation.DecodeJson(result);
                callback.onResult(stations, stations.size());
            } else {
                callback.onError(new Exception("Network error"));
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public String getPlayUrl(DataRadioStation station) {
        if (station == null) return null;
        if (!TextUtils.isEmpty(station.playableUrl)) return station.playableUrl;
        if (!TextUtils.isEmpty(station.StreamUrl)) return station.StreamUrl;

        String uuid = station.StationUuid;
        if (TextUtils.isEmpty(uuid)) return null;

        return Utils.getRealStationLink(httpClient, appContext, uuid);
    }

    @Override
    public boolean ownsStation(DataRadioStation station) {
        if (station == null) return false;
        return !QingtingDataSource.SOURCE_TYPE.equals(station.CountryCode); // RadioBrowser 是默认
    }

    /**
     * 获取已存在的 station 的 URL
     */
    public String getStationUrlByUuid(String uuid) {
        return Utils.getRealStationLink(httpClient, appContext, uuid);
    }

    /**
     * 传给 FragmentBase 使用的相对 URL
     */
    public static String[] getTabUrls(String countryCode) {
        String[] urls = new String[9];
        urls[0] = countryCode != null
                ? "json/stations/bycountrycodeexact/" + countryCode + "?order=clickcount&reverse=true"
                : "json/stations/bycountrycodeexact/?order=clickcount&reverse=true";
        urls[1] = "json/stations/topclick";
        urls[2] = "json/stations/topvote";
        urls[3] = "json/stations/lastchange";
        urls[4] = "json/stations/lastclick";
        urls[5] = "json/tags";
        urls[6] = "json/countries";
        urls[7] = "json/languages";
        urls[8] = ""; // search
        return urls;
    }

    private boolean getShowBroken() {
        try {
            return androidx.preference.PreferenceManager
                    .getDefaultSharedPreferences(appContext)
                    .getBoolean("show_broken", false);
        } catch (Exception e) {
            return false;
        }
    }
}
