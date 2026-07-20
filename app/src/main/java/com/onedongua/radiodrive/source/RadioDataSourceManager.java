package com.onedongua.radiodrive.source;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.onedongua.radiodrive.source.qingting.QingtingDataSource;

import okhttp3.OkHttpClient;

/**
 * 电台数据源管理器 — 单例。
 * 根据 SharedPreferences 中的设置选择当前数据源，
 * 并在设置变更时切换。
 */
public class RadioDataSourceManager {
    private static final String TAG = "DataSourceManager";

    public static final String PREF_KEY_DATA_SOURCE = "radio_data_source";
    public static final String DEFAULT_SOURCE = "radiobrowser";

    private static volatile RadioDataSourceManager instance;

    private final Context appContext;
    private OkHttpClient httpClient;
    private RadioDataSource currentSource;
    private String currentSourceType;

    private RadioDataSourceManager(Context appContext, OkHttpClient httpClient) {
        this.appContext = appContext;
        this.httpClient = httpClient;
        this.currentSourceType = getConfiguredSourceType();
        this.currentSource = createSource(currentSourceType);
    }

    public static RadioDataSourceManager getInstance(Context appContext, OkHttpClient httpClient) {
        if (instance == null) {
            synchronized (RadioDataSourceManager.class) {
                if (instance == null) {
                    instance = new RadioDataSourceManager(appContext, httpClient);
                }
            }
        }
        return instance;
    }

    public static RadioDataSourceManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RadioDataSourceManager not initialized. Call getInstance(Context, OkHttpClient) first.");
        }
        return instance;
    }

    /**
     * 获取当前数据源
     */
    public RadioDataSource getCurrentSource() {
        String configured = getConfiguredSourceType();
        if (!configured.equals(currentSourceType)) {
            Log.d(TAG, "Source changed from " + currentSourceType + " to " + configured);
            currentSourceType = configured;
            currentSource = createSource(configured);
        }
        return currentSource;
    }

    /**
     * 更新 HTTP 客户端（代理设置变更后）
     */
    public void updateHttpClient(OkHttpClient newClient) {
        this.httpClient = newClient;
        // 重建数据源以使用新客户端
        this.currentSource = createSource(currentSourceType);
    }

    private String getConfiguredSourceType() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            return prefs.getString(PREF_KEY_DATA_SOURCE, DEFAULT_SOURCE);
        } catch (Exception e) {
            return DEFAULT_SOURCE;
        }
    }

    private RadioDataSource createSource(String type) {
        switch (type) {
            case "qingting":
                return new QingtingDataSource(httpClient);
            case "radiobrowser":
            default:
                return new RadioBrowserDataSource(appContext, httpClient);
        }
    }
}
