package com.aura.tvlauncher;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class AndroidSetupHelper {
    private static final String TAG = "AndroidSetupHelper";

    public static class NetworkSetupInfo {
        public boolean isConnected = false;
        public String typeName = "Niet verbonden";
        public String networkName = "Geen netwerk";
        public String ipAddress = "127.0.0.1";

        public String getSummary() {
            if (!isConnected) {
                return "Geen actieve internetverbinding gedetecteerd";
            }
            return networkName + " • " + ipAddress;
        }
    }

    public static class GoogleAccountInfo {
        public boolean isLinked = false;
        public String accountName = "Geen Google Account gedetecteerd";

        public String getSummary() {
            return isLinked ? accountName : "Niet gekoppeld via Android TV";
        }
    }

    public static NetworkSetupInfo getNetworkInfo(Context context) {
        NetworkSetupInfo info = new NetworkSetupInfo();
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    info.isConnected = true;
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        info.typeName = "Wi-Fi";
                        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifi != null) {
                            WifiInfo wifiInfo = wifi.getConnectionInfo();
                            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                                String ssid = wifiInfo.getSSID().replace("\"", "");
                                if (!"<unknown ssid>".equalsIgnoreCase(ssid) && !ssid.isEmpty()) {
                                    info.networkName = ssid;
                                } else {
                                    info.networkName = "Wi-Fi Verbonden";
                                }
                            }
                        }
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        info.typeName = "Ethernet";
                        info.networkName = "Bekabelde verbinding (LAN)";
                    } else {
                        info.typeName = activeNetwork.getTypeName();
                        info.networkName = "Verbonden netwerk";
                    }
                }
            }
            String ip = CastDiscoveryManager.getLocalDeviceIpAddress();
            if (ip != null) {
                info.ipAddress = ip;
            }
        } catch (Exception e) {
            Log.w(TAG, "getNetworkInfo exception: " + e.getMessage());
        }
        return info;
    }

    public static GoogleAccountInfo getGoogleAccountInfo(Context context) {
        GoogleAccountInfo info = new GoogleAccountInfo();
        try {
            AccountManager am = AccountManager.get(context);
            Account[] accounts = am.getAccountsByType("com.google");
            if (accounts != null && accounts.length > 0) {
                info.isLinked = true;
                info.accountName = accounts[0].name;
                return info;
            }
        } catch (SecurityException ignored) {
            // Normal when GET_ACCOUNTS needs user grant on Android 8+
        } catch (Exception e) {
            Log.w(TAG, "getGoogleAccountInfo exception: " + e.getMessage());
        }

        // Check if Google Play Services / Google TV framework is active on this TV
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.google.android.gms", 0);
            info.isLinked = true;
            info.accountName = "Google TV Account gedetecteerd";
            return info;
        } catch (Exception ignored) {}

        return info;
    }

    public static void openGoogleHome(Context context) {
        PackageManager pm = context.getPackageManager();
        String googleHomePkg = "com.google.android.apps.chromecast.app";
        Intent intent = pm.getLaunchIntentForPackage(googleHomePkg);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // Fallback: Open Google Home web interface in WebActivity
            Intent webIntent = new Intent(context, WebActivity.class);
            webIntent.putExtra("url", "https://home.google.com");
            webIntent.putExtra("title", "Google Home");
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
        }
    }

    public static void openCastSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_CAST_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {}
        }
    }
}
