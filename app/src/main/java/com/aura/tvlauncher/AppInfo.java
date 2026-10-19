package com.aura.tvlauncher;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private final String label;
    private final String packageName;
    private final Drawable icon;
    private boolean isFavorite;

    public AppInfo(String label, String packageName, Drawable icon, boolean isFavorite) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.isFavorite = isFavorite;
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
}
