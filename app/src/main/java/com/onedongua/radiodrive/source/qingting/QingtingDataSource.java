package com.onedongua.radiodrive.source.qingting;

import android.text.TextUtils;
import android.util.Log;

import com.onedongua.radiodrive.data.DataCategory;
import com.onedongua.radiodrive.source.RadioDataSource;
import com.onedongua.radiodrive.station.DataRadioStation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

/**
 * 蜻蜓FM 数据源实现。
 * 通过 QingtingApiClient 调用蜻蜓 GraphQL API。
 */
public class QingtingDataSource implements RadioDataSource {
    private static final String TAG = "QingtingDS";

    public static final String SOURCE_TYPE = "qingting";

    private final QingtingApiClient apiClient;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public QingtingDataSource(OkHttpClient httpClient) {
        this.apiClient = new QingtingApiClient(httpClient);
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public void getCategories(CategoryType type, CategoryCallback callback) {
        executor.execute(() -> {
            try {
                switch (type) {
                    case REGIONS: {
                        GraphQLResponse resp = apiClient.fetchHomeData();
                        List<QingtingModels.Region> regions = apiClient.parseRegions(resp);
                        List<DataCategory> categories = new ArrayList<>();
                        for (QingtingModels.Region r : regions) {
                            DataCategory cat = new DataCategory();
                            cat.Name = r.title;
                            cat.UsedCount = r.id; // 用 UsedCount 存储 regionId
                            categories.add(cat);
                        }
                        callback.onResult(categories);
                        break;
                    }
                    case CLASSES: {
                        GraphQLResponse resp = apiClient.fetchHomeData();
                        List<QingtingModels.RadioClass> classes = apiClient.parseClasses(resp);
                        List<DataCategory> categories = new ArrayList<>();
                        for (QingtingModels.RadioClass c : classes) {
                            DataCategory cat = new DataCategory();
                            cat.Name = c.title;
                            cat.UsedCount = c.id; // 用 UsedCount 存储 classId
                            categories.add(cat);
                        }
                        callback.onResult(categories);
                        break;
                    }
                    default:
                        callback.onError(new UnsupportedOperationException(
                                "CategoryType " + type + " not supported by Qingting"));
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "getCategories error", e);
                callback.onError(e);
            }
        });
    }

    @Override
    public void getStationsByCategory(String categoryId, int page, StationsCallback callback) {
        executor.execute(() -> {
            try {
                int cid = Integer.parseInt(categoryId);
                GraphQLResponse resp = apiClient.fetchStationsByCategory(cid, page);
                List<QingtingModels.StationItem> items = apiClient.parseStationItems(resp);
                int totalCount = apiClient.parseTotalCount(resp);

                List<DataRadioStation> stations = new ArrayList<>();
                for (QingtingModels.StationItem item : items) {
                    stations.add(stationFromItem(item));
                }
                callback.onResult(stations, totalCount);
            } catch (Exception e) {
                Log.e(TAG, "getStationsByCategory error", e);
                callback.onError(e);
            }
        });
    }

    @Override
    public void search(String keyword, int page, StationsCallback callback) {
        executor.execute(() -> {
            try {
                GraphQLResponse resp = apiClient.search(keyword, page);
                List<QingtingModels.SearchData> results = apiClient.parseSearchResults(resp);
                int totalCount = apiClient.parseSearchTotalCount(resp);

                List<DataRadioStation> stations = new ArrayList<>();
                for (QingtingModels.SearchData item : results) {
                    stations.add(stationFromSearch(item));
                }
                callback.onResult(stations, totalCount);
            } catch (Exception e) {
                Log.e(TAG, "search error", e);
                callback.onError(e);
            }
        });
    }

    @Override
    public void getFeaturedStations(int page, StationsCallback callback) {
        // 蜻蜓FM: 首页 radioPlaying 作为推荐
        // 由Fragment层直接用关联查询获取
        executor.execute(() -> {
            try {
                // 使用默认的综合台分类（cid:432）获取热门电台
                GraphQLResponse resp = apiClient.query(
                        "radioPage(cid:432, page:1) { radioPlaying }");
                List<DataRadioStation> stations = new ArrayList<>();

                // 解析 radioPlaying
                org.json.JSONObject data = resp.getDataObject();
                if (data != null) {
                    org.json.JSONArray playingArr = findJsonArray(data, "radioPlaying");
                    if (playingArr != null) {
                        for (int i = 0; i < playingArr.length(); i++) {
                            org.json.JSONObject obj = playingArr.optJSONObject(i);
                            if (obj != null) {
                                stations.add(stationFromPlayingItem(obj));
                            }
                        }
                    }
                }
                callback.onResult(stations, stations.size());
            } catch (Exception e) {
                Log.e(TAG, "getFeaturedStations error", e);
                callback.onError(e);
            }
        });
    }

    @Override
    public String getPlayUrl(DataRadioStation station) {
        if (station == null) return null;
        String uuid = station.StationUuid;
        if (TextUtils.isEmpty(uuid)) return null;
        return QingtingUrlSigner.buildLiveUrl(uuid);
    }

    @Override
    public boolean ownsStation(DataRadioStation station) {
        if (station == null) return false;
        return SOURCE_TYPE.equals(station.CountryCode); // 用 CountryCode 存储来源标记
    }

    // ===== 数据转换 =====

    private DataRadioStation stationFromItem(QingtingModels.StationItem item) {
        DataRadioStation station = new DataRadioStation();
        station.Name = item.title;
        station.StationUuid = String.valueOf(item.id);
        station.IconUrl = item.imgUrl;
        station.ClickCount = item.playcount;
        station.Votes = item.clout;
        station.HomePageUrl = item.desc != null ? item.desc : "";
        station.TagsAll = "";
        station.Country = "";
        station.Language = "";
        station.Working = true;
        station.CountryCode = SOURCE_TYPE; // 标记来源
        if (item.podcaster != null) {
            station.State = item.podcaster.username;
        } else {
            station.State = "";
        }
        return station;
    }

    private DataRadioStation stationFromSearch(QingtingModels.SearchData item) {
        DataRadioStation station = new DataRadioStation();
        station.Name = item.title;
        station.StationUuid = String.valueOf(item.id);
        station.IconUrl = item.cover;
        station.ClickCount = item.clout;
        station.Votes = 0;
        station.HomePageUrl = item.description != null ? item.description : "";
        station.TagsAll = "";
        station.Country = "";
        station.Language = "";
        station.State = "";
        station.Working = true;
        station.CountryCode = SOURCE_TYPE;
        return station;
    }

    private DataRadioStation stationFromPlayingItem(org.json.JSONObject obj) {
        DataRadioStation station = new DataRadioStation();
        station.Name = obj.optString("name", obj.optString("channelTitle", ""));
        station.StationUuid = extractChannelId(obj.optString("to", ""));
        station.IconUrl = QingtingModels.fixImageUrl(obj.optString("imgUrl"));
        station.HomePageUrl = obj.optString("desc", "");
        station.TagsAll = "";
        station.Country = "";
        station.Language = "";
        station.State = "";
        station.Working = true;
        station.CountryCode = SOURCE_TYPE;
        station.ClickCount = obj.optInt("playcount");
        station.Votes = 0;
        return station;
    }

    /**
     * 从蜻蜓的 /radios/xxx 路径中提取频道ID
     */
    private String extractChannelId(String toPath) {
        if (toPath == null) return "";
        int idx = toPath.lastIndexOf('/');
        if (idx >= 0) {
            return toPath.substring(idx + 1);
        }
        return "";
    }

    /**
     * 在 JSON 树中递归查找数组 — 内联版本用于 radioPlaying 解析
     */
    private static org.json.JSONArray findJsonArray(org.json.JSONObject root, String key) {
        if (root.has(key)) return root.optJSONArray(key);
        org.json.JSONArray names = root.names();
        if (names == null) return null;
        for (int i = 0; i < names.length(); i++) {
            org.json.JSONObject child = root.optJSONObject(names.optString(i));
            if (child != null) {
                org.json.JSONArray found = findJsonArray(child, key);
                if (found != null) return found;
            }
        }
        return null;
    }
}
