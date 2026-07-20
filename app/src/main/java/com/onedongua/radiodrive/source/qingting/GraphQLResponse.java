package com.onedongua.radiodrive.source.qingting;

import android.util.Log;

import org.json.JSONObject;

/**
 * 封装蜻蜓FM GraphQL 响应。
 */
public class GraphQLResponse {
    private static final String TAG = "GraphQLResponse";

    private final String rawBody;
    private JSONObject rootJson;

    public GraphQLResponse(String rawBody) {
        this.rawBody = rawBody;
    }

    public String getRawBody() {
        return rawBody;
    }

    /**
     * 获取响应中的 data 对象。
     * 蜻蜓FM返回格式: {"data": {"radioPage": {...}}} 或 {"data": {"searchResultsPage": {...}}}
     */
    public JSONObject getDataObject() {
        if (rootJson == null) {
            try {
                rootJson = new JSONObject(rawBody);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse response", e);
                return null;
            }
        }
        return rootJson.optJSONObject("data");
    }
}
