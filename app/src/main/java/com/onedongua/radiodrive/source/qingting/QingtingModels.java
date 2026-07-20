package com.onedongua.radiodrive.source.qingting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 蜻蜓FM API 响应数据模型
 */
public class QingtingModels {

    // ---- 地区 ----
    public static class Region {
        public int id;
        public String title;

        public static Region fromJson(JSONObject obj) {
            Region r = new Region();
            r.id = obj.optInt("id");
            r.title = obj.optString("title");
            return r;
        }
    }

    // ---- 电台分类 ----
    public static class RadioClass {
        public int id;
        public String title;

        public static RadioClass fromJson(JSONObject obj) {
            RadioClass c = new RadioClass();
            c.id = obj.optInt("id");
            c.title = obj.optString("title");
            return c;
        }
    }

    // ---- 电台列表项 ----
    public static class StationItem {
        public int id;
        public String title;
        public String imgUrl;
        public int playcount;
        public int clout;
        public String desc;
        public Podcaster podcaster;

        public static StationItem fromJson(JSONObject obj) {
            StationItem item = new StationItem();
            item.id = obj.optInt("id");
            item.title = obj.optString("title");
            item.imgUrl = fixImageUrl(obj.optString("imgUrl"));
            item.playcount = obj.optInt("playcount");
            item.clout = obj.optInt("clout");
            item.desc = obj.optString("desc");

            if (obj.has("podcaster") && !obj.isNull("podcaster")) {
                item.podcaster = Podcaster.fromJson(obj.optJSONObject("podcaster"));
            }
            return item;
        }
    }

    // ---- 主播 ----
    public static class Podcaster {
        public int id;
        public String username;

        public static Podcaster fromJson(JSONObject obj) {
            Podcaster p = new Podcaster();
            p.id = obj.optInt("id");
            p.username = obj.optString("username");
            return p;
        }
    }

    // ---- 电台内容页 ----
    public static class Contents {
        public List<StationItem> items;
        public int count;

        public static Contents fromJson(JSONObject obj) {
            Contents c = new Contents();
            c.items = new ArrayList<>();
            c.count = obj.optInt("count");

            if (obj.has("items")) {
                JSONArray arr = obj.optJSONArray("items");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject itemObj = arr.optJSONObject(i);
                        if (itemObj != null) {
                            c.items.add(StationItem.fromJson(itemObj));
                        }
                    }
                }
            }
            return c;
        }
    }

    // ---- 搜索结果 ----
    public static class SearchData {
        public int id;
        public String title;
        public String type;
        public String cover;
        public String description;
        public int clout;

        public static SearchData fromJson(JSONObject obj) {
            SearchData s = new SearchData();
            s.id = obj.optInt("id");
            s.title = obj.optString("title");
            s.type = obj.optString("type");
            s.cover = fixImageUrl(obj.optString("cover"));
            s.description = obj.optString("description");
            s.clout = obj.optInt("clout");
            return s;
        }
    }

    // ---- 播放链接占位常量 ----
    /**
     * 蜻蜓FM播放链接模板: https://lhttp-hw.qtfm.cn/live/{channelId}/64k.mp3
     */
    public static final String PLAY_URL_PREFIX = "https://lhttp-hw.qtfm.cn/live/";
    public static final String PLAY_URL_SUFFIX = "/64k.mp3";

    /**
     * 将蜻蜓的 //pic.qtfm.cn/... 补全为完整URL
     */
    public static String fixImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        if (rawUrl.startsWith("//")) {
            return "https:" + rawUrl;
        }
        if (!rawUrl.startsWith("http")) {
            return "https://" + rawUrl;
        }
        return rawUrl;
    }
}
