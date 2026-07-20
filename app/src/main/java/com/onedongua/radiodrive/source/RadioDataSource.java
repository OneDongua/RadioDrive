package com.onedongua.radiodrive.source;

import com.onedongua.radiodrive.data.DataCategory;
import com.onedongua.radiodrive.station.DataRadioStation;

import java.util.List;

/**
 * 电台数据源抽象接口。
 * 不同数据源（RadioBrowser、蜻蜓FM 等）各自实现该接口，
 * 通过 RadioDataSourceManager 在设置中切换。
 */
public interface RadioDataSource {

    /**
     * 数据源类型标识
     */
    String getSourceType();

    /**
     * 获取分类/地区列表（用于浏览页的 Tags/Regions tab）
     */
    void getCategories(CategoryType type, CategoryCallback callback);

    /**
     * 按分类获取电台列表，支持分页
     */
    void getStationsByCategory(String categoryId, int page, StationsCallback callback);

    /**
     * 搜索电台
     */
    void search(String keyword, int page, StationsCallback callback);

    /**
     * 获取热门/推荐电台
     */
    void getFeaturedStations(int page, StationsCallback callback);

    /**
     * 获取电台的播放链接
     */
    String getPlayUrl(DataRadioStation station);

    /**
     * 判断某个 station 是否来自本数据源
     */
    boolean ownsStation(DataRadioStation station);

    // ---- 回调 ----

    interface CategoryCallback {
        void onResult(List<DataCategory> categories);

        void onError(Exception e);
    }

    interface StationsCallback {
        void onResult(List<DataRadioStation> stations, int totalCount);

        void onError(Exception e);
    }

    enum CategoryType {
        TAGS,
        COUNTRIES,
        LANGUAGES,
        REGIONS,    // 蜻蜓FM: 地区
        CLASSES     // 蜻蜓FM: 电台分类
    }
}
