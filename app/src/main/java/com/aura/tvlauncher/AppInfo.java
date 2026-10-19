package com.aura.tvlauncher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppInfo {
    private final String label;
    private final String packageName;
    private final Drawable icon;
    private boolean isFavorite;
    private Intent customIntent;

    public AppInfo(String label, String packageName, Drawable icon, boolean isFavorite) {
        this(label, packageName, icon, isFavorite, null);
    }

    public AppInfo(String label, String packageName, Drawable icon, boolean isFavorite, Intent customIntent) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.isFavorite = isFavorite;
        this.customIntent = customIntent;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public Intent getCustomIntent() {
        return customIntent;
    }

    public void setCustomIntent(Intent customIntent) {
        this.customIntent = customIntent;
    }
}
