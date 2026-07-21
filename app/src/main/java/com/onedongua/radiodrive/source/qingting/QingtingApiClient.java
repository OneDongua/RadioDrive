package com.onedongua.radiodrive.source.qingting;

import android.util.Log;

import com.onedongua.radiodrive.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 蜻蜓FM GraphQL API 客户端。
 * 封装 POST 请求到 https://webbff.qtfm.cn/www，发送 GraphQL query。
 */
public class QingtingApiClient {
    private static final String TAG = "QingtingApi";
    private static final String BASE_URL = "https://webbff.qtfm.cn/www";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    public QingtingApiClient(OkHttpClient httpClient) {
        // 为蜻蜓API使用较长的超时时间
        this.httpClient = httpClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * GraphQL query 模板:
     * { "query": "{ radioPage(cid:432, page:1) { regions, classes } }" }
     */
    public GraphQLResponse query(String graphqlQuery) throws IOException {
        String payload = "{\"query\":\"{" + escapeGraphQL(graphqlQuery) + "}\"}";

        if (BuildConfig.DEBUG) Log.d(TAG, "GraphQL query: " + graphqlQuery);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, payload);
        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", "https://www.qtfm.cn")
                .header("Referer", "https://www.qtfm.cn/")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "API error: " + response.code() + " " + response.message());
                throw new IOException("HTTP " + response.code());
            }
            String bodyStr = response.body() != null ? response.body().string() : "";
            return new GraphQLResponse(bodyStr);
        }
    }

    /**
     * 转义 GraphQL 查询字符串中的特殊字符
     */
    private static String escapeGraphQL(String query) {
        return query.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    // ===== 业务方法 =====

    /**
     * 获取首页数据（regions + classes）
     */
    public GraphQLResponse fetchHomeData() throws IOException {
        return query("radioPage(cid:432, page:1) { regions, classes }");
    }

    /**
     * 获取指定地区/分类的电台列表
     *
     * @param cid  地区或分类ID
     * @param page 页码，从1开始
     */
    public GraphQLResponse fetchStationsByCategory(int cid, int page) throws IOException {
        return query("radioPage(cid:" + cid + ", page:" + page + ") { contents }");
    }

    /**
     * 搜索电台
     */
    public GraphQLResponse search(String keyword, int page) throws IOException {
        return query("searchResultsPage(keyword:\"" + keyword + "\", page:" + page
                + ", include:\"channel_live\") { searchData, numFound }");
    }

    // ===== 解析方法 =====

    /**
     * 从 GraphQLResponse 中解析 regions 列表
     */
    public List<QingtingModels.Region> parseRegions(GraphQLResponse response) {
        List<QingtingModels.Region> list = new ArrayList<>();
        try {
            JSONObject data = response.getDataObject();
            if (data == null) return list;

            JSONArray regions = findJsonArray(data, "regions");
            if (regions != null) {
                for (int i = 0; i < regions.length(); i++) {
                    JSONObject obj = regions.optJSONObject(i);
                    if (obj != null) list.add(QingtingModels.Region.fromJson(obj));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseRegions error", e);
        }
        return list;
    }

    /**
     * 从 GraphQLResponse 中解析 classes 列表
     */
    public List<QingtingModels.RadioClass> parseClasses(GraphQLResponse response) {
        List<QingtingModels.RadioClass> list = new ArrayList<>();
        try {
            JSONObject data = response.getDataObject();
            if (data == null) return list;

            JSONArray classes = findJsonArray(data, "classes");
            if (classes != null) {
                for (int i = 0; i < classes.length(); i++) {
                    JSONObject obj = classes.optJSONObject(i);
                    if (obj != null) list.add(QingtingModels.RadioClass.fromJson(obj));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseClasses error", e);
        }
        return list;
    }

    /**
     * 从 GraphQLResponse 中解析电台列表（contents.items）
     */
    public List<QingtingModels.StationItem> parseStationItems(GraphQLResponse response) {
        List<QingtingModels.StationItem> list = new ArrayList<>();
        try {
            JSONObject data = response.getDataObject();
            if (data == null) return list;

            JSONObject contents = findJsonObject(data, "contents");
            if (contents != null) {
                JSONArray items = contents.optJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject obj = items.optJSONObject(i);
                        if (obj != null) list.add(QingtingModels.StationItem.fromJson(obj));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseStationItems error", e);
        }
        return list;
    }

    /**
     * 获取电台列表总数
     */
    public int parseTotalCount(GraphQLResponse response) {
        try {
            JSONObject data = response.getDataObject();
            if (data == null) return 0;

            JSONObject contents = findJsonObject(data, "contents");
            if (contents != null) {
                return contents.optInt("count", 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseTotalCount error", e);
        }
        return 0;
    }

    /**
     * 解析搜索结果
     */
    public List<QingtingModels.SearchData> parseSearchResults(GraphQLResponse response) {
        List<QingtingModels.SearchData> list = new ArrayList<>();
        try {
            JSONObject data = response.getDataObject();
            if (data == null) return list;

            JSONArray searchData = findJsonArray(data, "searchData");
            if (searchData != null) {
                for (int i = 0; i < searchData.length(); i++) {
                    JSONObject obj = searchData.optJSONObject(i);
                    if (obj != null) list.add(QingtingModels.SearchData.fromJson(obj));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseSearchResults error", e);
        }
        return list;
    }

    /**
     * 获取搜索结果总数。
     * 搜索响应结构: { data: { searchResultsPage: { numFound: 123 } } }
     * 需要先找到 searchResultsPage 对象再读取 numFound。
     */
    public int parseSearchTotalCount(GraphQLResponse response) {
        try {
            JSONObject data = response.getDataObject();
            if (data == null) return 0;
            JSONObject searchPage = findJsonObject(data, "searchResultsPage");
            if (searchPage != null) {
                return searchPage.optInt("numFound", 0);
            }
            // 回退：尝试直接在 data 层级读取
            return data.optInt("numFound", 0);
        } catch (Exception e) {
            Log.e(TAG, "parseSearchTotalCount error", e);
        }
        return 0;
    }

    // ===== JSON 工具方法 =====

    /**
     * 在 JSON 树中递归查找数组
     */
    private JSONArray findJsonArray(JSONObject root, String key) {
        if (root.has(key)) return root.optJSONArray(key);
        JSONArray names = root.names();
        if (names == null) return null;
        for (int i = 0; i < names.length(); i++) {
            JSONObject child = root.optJSONObject(names.optString(i));
            if (child != null) {
                JSONArray found = findJsonArray(child, key);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * 在 JSON 树中递归查找对象
     */
    private JSONObject findJsonObject(JSONObject root, String key) {
        if (root.has(key)) return root.optJSONObject(key);
        JSONArray names = root.names();
        if (names == null) return null;
        for (int i = 0; i < names.length(); i++) {
            JSONObject child = root.optJSONObject(names.optString(i));
            if (child != null) {
                JSONObject found = findJsonObject(child, key);
                if (found != null) return found;
            }
        }
        return null;
    }
}
