package com.aura.tvlauncher;

import org.json.JSONException;
import org.json.JSONObject;

public class WebAppInfo {
    private final String id;
    private final String title;
    private final String url;
    private final String colorHex;
    private final String letter;

    public WebAppInfo(String id, String title, String url, String colorHex, String letter) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.colorHex = colorHex;
        this.letter = letter;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getLetter() {
        return letter;
    }

    public String getDomain() {
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            return domain != null ? domain : url;
        } catch (Exception e) {
            return url;
        }
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("url", url);
            obj.put("colorHex", colorHex);
            obj.put("letter", letter);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static WebAppInfo fromJsonObject(JSONObject obj) {
        String id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
        String title = obj.optString("title", "Web App");
        String url = obj.optString("url", "https://google.com");
        String colorHex = obj.optString("colorHex", "#3B82F6");
        String letter = obj.optString("letter", title.isEmpty() ? "W" : title.substring(0, 1).toUpperCase());
        return new WebAppInfo(id, title, url, colorHex, letter);
    }
}
