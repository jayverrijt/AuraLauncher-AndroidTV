package com.aura.tvlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class TvProvider {
    public static final String PREF_DEFAULT_PROVIDER_ID = "key_default_tv_provider_id";
    public static final String PREF_DIRECT_START = "key_stream_tv_direct_start";
    public static final String PREF_CUSTOM_URL = "key_custom_tv_url";

    private final String id;
    private final String name;
    private final String url;
    private final String tagline;
    private final String accentColor;
    private final String iconEmoji;
    private final String badgeText;

    public TvProvider(String id, String name, String url, String tagline, String accentColor, String iconEmoji, String badgeText) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.tagline = tagline;
        this.accentColor = accentColor;
        this.iconEmoji = iconEmoji;
        this.badgeText = badgeText;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getTagline() {
        return tagline;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public static List<TvProvider> getStandardProviders(Context context) {
        List<TvProvider> list = new ArrayList<>();
        list.add(new TvProvider(
                "bbhtv",
                "BBH TV",
                "https://ott.bbhtv.nl/pctv/",
                "Breedband Helmond • Live TV, Gids & Replay",
                "#0284C7",
                "📡",
                "HELMOND"
        ));
        list.add(new TvProvider(
                "ziggo",
                "Ziggo GO",
                "https://www.ziggogo.tv",
                "Ziggo TV • Live zenders, Replay TV & TV Gids",
                "#F97316",
                "🟠",
                "ZIGGO"
        ));
        list.add(new TvProvider(
                "kpn",
                "KPN TV",
                "https://tv.kpn.com",
                "KPN Interactieve TV • Live kijken & Begin Gemist",
                "#10B981",
                "🟢",
                "KPN"
        ));
        list.add(new TvProvider(
                "odido",
                "Odido TV",
                "https://tv.odido.nl",
                "Odido Online TV • Live TV, Programma Gemist & Gids",
                "#8B5CF6",
                "🟣",
                "ODIDO"
        ));
        list.add(new TvProvider(
                "nlziet",
                "NLZIET",
                "https://app.nlziet.nl",
                "NLZIET • NPO, RTL, KIJK & 40+ Live TV Zenders",
                "#EF4444",
                "🔴",
                "NLZIET"
        ));
        list.add(new TvProvider(
                "canalplus",
                "Canal+",
                "https://www.canalplus.nl",
                "Canal+ Online • Live TV, Films, Series & Sport",
                "#EAB308",
                "🟡",
                "CANAL+"
        ));
        return list;
    }

    public static TvProvider getDefaultProvider(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);
        String savedId = prefs.getString(PREF_DEFAULT_PROVIDER_ID, "bbhtv");

        for (TvProvider p : getStandardProviders(context)) {
            if (p.getId().equalsIgnoreCase(savedId)) {
                return p;
            }
        }

        if ("custom".equalsIgnoreCase(savedId)) {
            String customUrl = prefs.getString(PREF_CUSTOM_URL, "https://ott.bbhtv.nl/pctv/");
            return new TvProvider("custom", "Aangepaste TV", customUrl, "Eigen IPTV / Web Stream URL", "#38BDF8", "🌐", "CUSTOM");
        }

        return getStandardProviders(context).get(0);
    }

    public static void setDefaultProvider(Context context, String providerId) {
        SharedPreferences prefs = context.getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_DEFAULT_PROVIDER_ID, providerId).apply();
    }

    public static boolean isDirectStartEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_DIRECT_START, false);
    }

    public static void setDirectStartEnabled(Context context, boolean directStart) {
        SharedPreferences prefs = context.getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_DIRECT_START, directStart).apply();
    }
}
