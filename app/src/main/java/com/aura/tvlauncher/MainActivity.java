package com.aura.tvlauncher;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "aura_launcher_prefs";
    private static final String KEY_FAVORITES = "key_favorites";
    private static final String KEY_WEB_APPS = "key_web_apps";
    private static final String KEY_FIRST_RUN = "key_first_run";
    private static final String KEY_ADBLOCK = "key_adblock_enabled";
    private static final String KEY_SPEAKER_PRIMARY_PLAYING = "key_speaker_primary_playing";
    private static final String KEY_SPEAKER_SECONDARY_PLAYING = "key_speaker_secondary_playing";
    private static final String KEY_ACTIVE_SPEAKER = "key_active_speaker";

    private TextView tvClock;
    private TextView tvDate;
    private NestedScrollView mainScrollView;
    private RecyclerView rvFavorites;
    private RecyclerView rvWebApps;
    private RecyclerView rvAllApps;

    private LinearLayout btnMusicControl;
    private TextView tvHeaderMusicText;
    private Dialog activeDialog;

    private CastController.CastMediaInfo currentCastInfo;
    private final Handler castPollHandler = new Handler(Looper.getMainLooper());

    private AppsAdapter favoritesAdapter;
    private AppsAdapter allAppsAdapter;
    private WebAppsAdapter webAppsAdapter;

    private final List<AppInfo> allAppsList = new ArrayList<>();
    private final List<AppInfo> favoritesList = new ArrayList<>();
    private final List<WebAppInfo> webAppsList = new ArrayList<>();

    private SharedPreferences prefs;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Sync AdBlock state
        boolean adblockOn = prefs.getBoolean(KEY_ADBLOCK, true);
        AdBlocker.setEnabled(adblockOn);

        // Check if AuraOS initial setup wizard has run
        if (!prefs.getBoolean(SetupActivity.KEY_SETUP_COMPLETED, false)) {
            startActivity(new Intent(this, SetupActivity.class));
        }

        initViews();
        setupClock();
        setupAdapters();
        loadWebApps();
        loadInstalledApps();
    }

    private void initViews() {
        mainScrollView = findViewById(R.id.main_scroll_view);
        tvClock = findViewById(R.id.tv_clock);
        tvDate = findViewById(R.id.tv_date);

        rvFavorites = findViewById(R.id.rv_favorites);
        rvWebApps = findViewById(R.id.rv_web_apps);
        rvAllApps = findViewById(R.id.rv_all_apps);

        btnMusicControl = findViewById(R.id.btn_music_control);
        tvHeaderMusicText = findViewById(R.id.tv_header_music_text);
        LinearLayout btnGoogleHome = findViewById(R.id.btn_google_home);
        LinearLayout btnMenuHub = findViewById(R.id.btn_menu_hub);

        btnMusicControl.setOnClickListener(v -> showMusicPlayerDialog());
        btnGoogleHome.setOnClickListener(v -> showGoogleHomeDialog());
        btnMenuHub.setOnClickListener(v -> showMenuHubDialog());

        updateMusicHeaderUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMusicHeaderUi();
        refreshCastSpeakerStatus();
    }

    private void refreshCastSpeakerStatus() {
        String ip = prefs.getString("key_speaker_ip", prefs.getString("key_speaker_jay_ip", CastController.DEFAULT_SPEAKER_IP));
        CastController.fetchStatus(ip, new CastController.StatusCallback() {
            @Override
            public void onStatus(CastController.CastMediaInfo info) {
                currentCastInfo = info;
                updateMusicHeaderUi();
            }

            @Override
            public void onError(Exception e) {
                // Silently keep last known state
            }
        });
    }

    private void updateMusicHeaderUi() {
        btnMusicControl.setVisibility(View.VISIBLE);

        String active = prefs.getString(KEY_ACTIVE_SPEAKER, BuildConfig.DEFAULT_SPEAKER_NAME);
        android.widget.ImageView ivMusicIcon = findViewById(R.id.iv_header_music_icon);

        if (currentCastInfo != null && currentCastInfo.isPlaying() && currentCastInfo.hasMedia()) {
            tvHeaderMusicText.setText(currentCastInfo.title);
            tvHeaderMusicText.setTextColor(Color.parseColor("#1DB954")); // Spotify Green
            if (ivMusicIcon != null) {
                ivMusicIcon.setColorFilter(Color.parseColor("#1DB954"));
            }
        } else if (currentCastInfo != null && currentCastInfo.isPlaying()) {
            tvHeaderMusicText.setText(active + " • Speelt af");
            tvHeaderMusicText.setTextColor(Color.parseColor("#1DB954"));
            if (ivMusicIcon != null) {
                ivMusicIcon.setColorFilter(Color.parseColor("#1DB954"));
            }
        } else {
            tvHeaderMusicText.setText(active);
            tvHeaderMusicText.setTextColor(Color.parseColor("#CBD5E1")); // Standby Slate
            if (ivMusicIcon != null) {
                ivMusicIcon.setColorFilter(Color.parseColor("#94A3B8"));
            }
        }

        LinearLayout btnGoogleHome = findViewById(R.id.btn_google_home);
        if (btnGoogleHome != null) {
            btnGoogleHome.setNextFocusLeftId(R.id.btn_music_control);
        }
    }

    private void setupClock() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("nl", "NL"));
                Date now = new Date();

                tvClock.setText(timeFormat.format(now));

                String dateStr = dateFormat.format(now);
                if (dateStr.length() > 0) {
                    dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
                }
                tvDate.setText(dateStr);

                timeHandler.postDelayed(this, 10000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private String getRamStatusString() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.getMemoryInfo(mi);
                long freeMb = mi.availMem / (1024 * 1024);
                long totalMb = mi.totalMem / (1024 * 1024);
                return String.format(Locale.getDefault(), "%d MB Vrij / %d MB", freeMb, totalMb);
            }
        } catch (Exception ignored) {}
        return "640 MB Vrij / 1347 MB";
    }

    private boolean isAdBlockActive() {
        try {
            String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
            if (mode != null) {
                return "hostname".equalsIgnoreCase(mode);
            }
        } catch (Exception ignored) {}
        return prefs.getBoolean(KEY_ADBLOCK, true);
    }

    private void setAdBlockEnabled(boolean enable) {
        prefs.edit().putBoolean(KEY_ADBLOCK, enable).apply();
        AdBlocker.setEnabled(enable);

        new Thread(() -> {
            try {
                if (enable) {
                    Settings.Global.putString(getContentResolver(), "private_dns_mode", "hostname");
                    Settings.Global.putString(getContentResolver(), "private_dns_specifier", "dns.adguard-dns.com");
                } else {
                    Settings.Global.putString(getContentResolver(), "private_dns_mode", "off");
                    Settings.Global.putString(getContentResolver(), "private_dns_specifier", "");
                }
            } catch (Exception e) {
                try {
                    String mode = enable ? "hostname" : "off";
                    String dns = enable ? "dns.adguard-dns.com" : "";
                    Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "private_dns_mode", mode});
                    Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "private_dns_specifier", dns});
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void setupAdapters() {
        // Favorites adapter
        favoritesAdapter = new AppsAdapter(this, app -> {
            saveFavorites();
            refreshFavoritesList();
        });
        rvFavorites.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFavorites.setAdapter(favoritesAdapter);

        // Web Apps adapter
        webAppsAdapter = new WebAppsAdapter(this, new WebAppsAdapter.OnWebAppActionListener() {
            @Override
            public void onAddClicked() {
                showAddWebAppDialog();
            }

            @Override
            public void onDeleted(WebAppInfo webApp) {
                deleteWebApp(webApp);
            }
        });
        rvWebApps.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvWebApps.setAdapter(webAppsAdapter);

        // All Apps grid adapter (5 columns)
        allAppsAdapter = new AppsAdapter(this, app -> {
            saveFavorites();
            refreshFavoritesList();
        });
        rvAllApps.setLayoutManager(new GridLayoutManager(this, 5));
        rvAllApps.setAdapter(allAppsAdapter);
    }

    private void loadInstalledApps() {
        new AsyncTask<Void, Void, List<AppInfo>>() {
            @Override
            protected List<AppInfo> doInBackground(Void... voids) {
                PackageManager pm = getPackageManager();
                List<AppInfo> apps = new ArrayList<>();
                Set<String> seenPackages = new HashSet<>();

                // Query Leanback launcher apps
                Intent tvIntent = new Intent(Intent.ACTION_MAIN, null);
                tvIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                List<ResolveInfo> tvActivities = pm.queryIntentActivities(tvIntent, 0);

                // Also query standard launcher apps
                Intent phoneIntent = new Intent(Intent.ACTION_MAIN, null);
                phoneIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> standardActivities = pm.queryIntentActivities(phoneIntent, 0);

                List<ResolveInfo> allActivities = new ArrayList<>(tvActivities);
                allActivities.addAll(standardActivities);
                android.util.Log.i("AuraTV", "Total activities found: " + allActivities.size() + " (tv=" + tvActivities.size() + ", standard=" + standardActivities.size() + ")");

                Set<String> favoritePackages = getFavoritePackageNames();

                for (ResolveInfo ri : allActivities) {
                    String pkg = ri.activityInfo.packageName;
                    android.util.Log.i("AuraTV", "Activity package: " + pkg);
                    if (pkg.equals(getPackageName())
                            || pkg.equals("com.google.android.tvlauncher")
                            || pkg.equals("fusion.android.tv.demo")
                            || pkg.equals("ru.ivi.client")
                            || pkg.equals("com.zdf.android.mediathek")
                            || pkg.equals("de.swr.avp.ard.tv")
                            || pkg.equals("tv.wuaki.apptv")
                            || pkg.equals("com.mstar.android.tv.disclaimercustomization")
                            || pkg.startsWith("uk.co.freeview")) {
                        continue;
                    }
                    if (seenPackages.contains(pkg)) {
                        continue;
                    }
                    seenPackages.add(pkg);

                    String label = ri.loadLabel(pm).toString();
                    Drawable icon = ri.loadIcon(pm);

                    boolean isFav = favoritePackages.contains(pkg);

                    // Ensure popular streaming apps are favorites
                    if (isPopularStreamingApp(pkg)) {
                        isFav = true;
                        favoritePackages.add(pkg);
                    }

                    android.util.Log.i("AuraTV", "Added app: " + label + " (" + pkg + "), isFav=" + isFav);
                    apps.add(new AppInfo(label, pkg, icon, isFav));
                }

                // Sort installed apps alphabetically
                Collections.sort(apps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));

                // Add AuraOS Native TV Apps at the front
                Drawable tvIcon = androidx.appcompat.content.res.AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_tv);
                Intent streamTvIntent = new Intent(MainActivity.this, StreamTvActivity.class);
                AppInfo streamTvApp = new AppInfo("Stream TV", "com.aura.tvlauncher.streamtv", tvIcon, true, streamTvIntent);

                Drawable settingsIcon = androidx.appcompat.content.res.AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_settings);
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                AppInfo settingsApp = new AppInfo("Instellingen", "com.aura.tvlauncher.settings", settingsIcon, false, settingsIntent);

                apps.add(0, streamTvApp);
                apps.add(settingsApp);
                favoritePackages.add("com.aura.tvlauncher.streamtv");
                favoritePackages.remove("com.aura.tvlauncher.settings");

                prefs.edit()
                        .putStringSet(KEY_FAVORITES, favoritePackages)
                        .putBoolean(KEY_FIRST_RUN, false)
                        .apply();
                return apps;
            }

            @Override
            protected void onPostExecute(List<AppInfo> result) {
                allAppsList.clear();
                allAppsList.addAll(result);
                allAppsAdapter.setApps(allAppsList);
                refreshFavoritesList();

                // Focus first item in favorites for immediate remote response
                if (rvFavorites.getChildCount() > 0) {
                    rvFavorites.getChildAt(0).requestFocus();
                }
            }
        }.execute();
    }

    private boolean isPopularStreamingApp(String pkg) {
        return pkg.equals("com.netflix.ninja")
                || pkg.equals("com.google.android.youtube.tv")
                || pkg.equals("com.disney.disneyplus")
                || pkg.equals("com.amazon.amazonvideo.livingroom")
                || pkg.equals("nl.rtl.videoland.v2")
                || pkg.equals("com.spotify.tv.android");
    }

    private Set<String> getFavoritePackageNames() {
        Set<String> favs = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favs.remove("com.aura.tvlauncher.settings");
        return favs;
    }

    private void saveFavorites() {
        Set<String> favs = new HashSet<>();
        for (AppInfo app : allAppsList) {
            if (app.isFavorite() && !"com.aura.tvlauncher.settings".equals(app.getPackageName())) {
                favs.add(app.getPackageName());
            }
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favs).apply();
    }

    private void refreshFavoritesList() {
        favoritesList.clear();
        for (AppInfo app : allAppsList) {
            if ("com.aura.tvlauncher.streamtv".equals(app.getPackageName())) {
                favoritesList.add(app);
                break;
            }
        }
        for (AppInfo app : allAppsList) {
            if (app.isFavorite() && !"com.aura.tvlauncher.streamtv".equals(app.getPackageName()) && !"com.aura.tvlauncher.settings".equals(app.getPackageName())) {
                favoritesList.add(app);
            }
        }
        favoritesAdapter.setApps(favoritesList);
    }

    private void loadWebApps() {
        webAppsList.clear();
        String json = prefs.getString(KEY_WEB_APPS, null);
        if (json == null) {
            webAppsList.add(new WebAppInfo("stremio", "Stremio Web", "https://web.stremio.com", "#6C5CE7", "S"));
            webAppsList.add(new WebAppInfo("nos", "NOS Nieuws", "https://nos.nl", "#EF4444", "N"));
            saveWebApps();
        } else {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    WebAppInfo app = WebAppInfo.fromJsonObject(arr.getJSONObject(i));
                    if ("bbhtv".equals(app.getId()) || (app.getUrl() != null && app.getUrl().contains("bbhtv"))) {
                        continue;
                    }
                    webAppsList.add(app);
                }
            } catch (Exception ignored) {}
        }
        webAppsAdapter.setWebApps(webAppsList);
    }

    private void saveWebApps() {
        JSONArray arr = new JSONArray();
        for (WebAppInfo info : webAppsList) {
            arr.put(info.toJsonObject());
        }
        prefs.edit().putString(KEY_WEB_APPS, arr.toString()).apply();
    }

    private void addWebApp(WebAppInfo info) {
        webAppsList.add(info);
        saveWebApps();
        webAppsAdapter.setWebApps(webAppsList);
        Toast.makeText(this, "Website vastgepind!", Toast.LENGTH_SHORT).show();
    }

    private void deleteWebApp(WebAppInfo info) {
        webAppsList.remove(info);
        saveWebApps();
        webAppsAdapter.setWebApps(webAppsList);
        Toast.makeText(this, "Website verwijderd", Toast.LENGTH_SHORT).show();
    }

    private void setupDialogWindow(Dialog dialog) {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        activeDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (activeDialog == dialog) {
                activeDialog = null;
            }
        });
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                dialog.dismiss();
                return true;
            }
            return false;
        });
    }

    private void showMenuHubDialog() {
        Dialog dialog = new Dialog(this);
        setupDialogWindow(dialog);
        dialog.setContentView(R.layout.dialog_menu_hub);

        Button btnToggleAdBlock = dialog.findViewById(R.id.btn_toggle_adblock);
        TextView tvAdBlockDesc = dialog.findViewById(R.id.tv_adblock_desc);
        TextView tvSubmenuRam = dialog.findViewById(R.id.tv_submenu_ram);
        Button btnCleanRam = dialog.findViewById(R.id.btn_clean_ram);

        Button btnPinWeb = dialog.findViewById(R.id.btn_submenu_pin_web);
        Button btnSettings = dialog.findViewById(R.id.btn_submenu_settings);
        Button btnReload = dialog.findViewById(R.id.btn_submenu_reload);
        Button btnClose = dialog.findViewById(R.id.btn_submenu_close);

        // Set initial AdBlock button state
        final boolean[] adBlockCurrent = {isAdBlockActive()};
        updateAdBlockBtnUi(btnToggleAdBlock, tvAdBlockDesc, adBlockCurrent[0]);

        btnToggleAdBlock.setOnClickListener(v -> {
            adBlockCurrent[0] = !adBlockCurrent[0];
            setAdBlockEnabled(adBlockCurrent[0]);
            updateAdBlockBtnUi(btnToggleAdBlock, tvAdBlockDesc, adBlockCurrent[0]);
            Toast.makeText(this, adBlockCurrent[0] ? "🛡️ AdBlocker Ingeschakeld" : "⚠️ AdBlocker Uitgeschakeld", Toast.LENGTH_SHORT).show();
        });

        // RAM status & cleanup
        tvSubmenuRam.setText(getRamStatusString());
        btnCleanRam.setOnClickListener(v -> {
            System.gc();
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.killBackgroundProcesses(getPackageName());
                }
            } catch (Exception ignored) {}
            tvSubmenuRam.setText(getRamStatusString());
            Toast.makeText(this, "🧹 Geheugen opgeschoond!", Toast.LENGTH_SHORT).show();
        });

        btnPinWeb.setOnClickListener(v -> {
            dialog.dismiss();
            showAddWebAppDialog();
        });

        btnSettings.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        Button btnAndroidSettings = dialog.findViewById(R.id.btn_submenu_android_settings);
        if (btnAndroidSettings != null) {
            btnAndroidSettings.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(settingsIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Kan Android instellingen niet openen", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnReload.setOnClickListener(v -> {
            loadInstalledApps();
            Toast.makeText(this, "Apps vernieuwd!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateAdBlockBtnUi(Button btn, TextView desc, boolean active) {
        if (active) {
            btn.setText("INGESCHAKELD");
            btn.setTextColor(Color.parseColor("#34D399")); // Green
            desc.setText("Actief: Blokkeert ads via DNS-over-TLS (AdGuard) & in-app Web Filter");
        } else {
            btn.setText("UITGESCHAKELD");
            btn.setTextColor(Color.parseColor("#EF4444")); // Red
            desc.setText("Uitgeschakeld: Advertenties worden doorgelaten");
        }
    }

    private void showGoogleHomeDialog() {
        Dialog dialog = new Dialog(this);
        setupDialogWindow(dialog);
        dialog.setContentView(R.layout.dialog_google_home);

        View cardTv = dialog.findViewById(R.id.card_device_tv);
        View cardBed = dialog.findViewById(R.id.card_device_bed);
        TextView tvBed = dialog.findViewById(R.id.tv_status_bed);
        final boolean[] bedOn = {prefs.getBoolean("key_bed_light_on", false)};
        if (bedOn[0]) {
            tvBed.setText("Aan");
            tvBed.setTextColor(Color.parseColor("#FBBF24"));
        } else {
            tvBed.setText("Uit");
            tvBed.setTextColor(Color.parseColor("#94A3B8"));
        }

        TextView tvHomeSectionTitle = dialog.findViewById(R.id.tv_home_section_title);
        String sRoom = prefs.getString("key_speaker_room", BuildConfig.DEFAULT_SPEAKER_ROOM);
        if (tvHomeSectionTitle != null) {
            tvHomeSectionTitle.setText(sRoom.toUpperCase() + " (3 APPARATEN)");
        }

        TextView badgeHomeName = dialog.findViewById(R.id.badge_home_name);
        if (badgeHomeName != null) {
            badgeHomeName.setText(prefs.getString("key_home_name", BuildConfig.DEFAULT_HOME_NAME));
        }

        TextView tvDeviceTv = dialog.findViewById(R.id.tv_device_tv_name);
        if (tvDeviceTv != null) {
            tvDeviceTv.setText(android.os.Build.MODEL);
        }

        View cardSpeakerPrimary = dialog.findViewById(R.id.card_device_speaker_primary);
        TextView tvNameSpeakerPrimary = dialog.findViewById(R.id.tv_name_speaker_primary);
        String sName = prefs.getString("key_speaker_name", BuildConfig.DEFAULT_SPEAKER_NAME);
        if (tvNameSpeakerPrimary != null) {
            tvNameSpeakerPrimary.setText(sName);
        }
        TextView tvSpeakerPrimary = dialog.findViewById(R.id.tv_status_speaker_primary);
        TextView iconSpeakerPrimary = dialog.findViewById(R.id.icon_device_speaker_primary);
        final boolean[] speakerPrimaryPlaying = {prefs.getBoolean(KEY_SPEAKER_PRIMARY_PLAYING, prefs.getBoolean("key_speaker_jay_playing", false))};
        if (speakerPrimaryPlaying[0]) {
            tvSpeakerPrimary.setText("Spotify • Afspelen");
            tvSpeakerPrimary.setTextColor(Color.parseColor("#1DB954"));
            if (iconSpeakerPrimary != null) iconSpeakerPrimary.setText("🎵");
        } else {
            tvSpeakerPrimary.setText("Standby");
            tvSpeakerPrimary.setTextColor(Color.parseColor("#94A3B8"));
            if (iconSpeakerPrimary != null) iconSpeakerPrimary.setText("🔊");
        }

        View cardSpeakerSecondary = dialog.findViewById(R.id.card_device_speaker_secondary);
        TextView tvSpeakerSecondary = dialog.findViewById(R.id.tv_status_speaker_secondary);
        TextView iconSpeakerSecondary = dialog.findViewById(R.id.icon_device_speaker_secondary);
        final boolean[] speakerSecondaryPlaying = {prefs.getBoolean(KEY_SPEAKER_SECONDARY_PLAYING, prefs.getBoolean("key_jays_speaker_playing", false))};
        if (speakerSecondaryPlaying[0]) {
            tvSpeakerSecondary.setText("Spotify • Afspelen");
            tvSpeakerSecondary.setTextColor(Color.parseColor("#1DB954"));
            if (iconSpeakerSecondary != null) iconSpeakerSecondary.setText("🎵");
        } else {
            tvSpeakerSecondary.setText("Standby");
            tvSpeakerSecondary.setTextColor(Color.parseColor("#94A3B8"));
            if (iconSpeakerSecondary != null) iconSpeakerSecondary.setText("🔊");
        }

        cardTv.setOnClickListener(v -> {
            Toast.makeText(this, "📺 " + android.os.Build.MODEL + " • Android TV is actief", Toast.LENGTH_SHORT).show();
        });

        cardBed.setOnClickListener(v -> {
            bedOn[0] = !bedOn[0];
            prefs.edit().putBoolean("key_bed_light_on", bedOn[0]).apply();
            String query = bedOn[0] ? "Zet het licht Bed aan" : "Zet het licht Bed uit";
            sendGoogleHomeCommand(query);

            if (bedOn[0]) {
                tvBed.setText("Aan");
                tvBed.setTextColor(Color.parseColor("#FBBF24")); // Warm gold
            } else {
                tvBed.setText("Uit");
                tvBed.setTextColor(Color.parseColor("#94A3B8"));
            }
        });

        cardSpeakerPrimary.setOnClickListener(v -> {
            speakerPrimaryPlaying[0] = !speakerPrimaryPlaying[0];
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_SPEAKER_PRIMARY_PLAYING, speakerPrimaryPlaying[0]);
            String speakerName = prefs.getString("key_speaker_name", BuildConfig.DEFAULT_SPEAKER_NAME);
            if (speakerPrimaryPlaying[0]) {
                editor.putString(KEY_ACTIVE_SPEAKER, speakerName);
            }
            editor.apply();
            updateMusicHeaderUi();

            String query = speakerPrimaryPlaying[0] ? ("Speel muziek op Spotify af op " + speakerName) : ("Pauzeer " + speakerName);
            sendGoogleHomeCommand(query);
            if (speakerPrimaryPlaying[0]) {
                tvSpeakerPrimary.setText("Spotify • Afspelen");
                tvSpeakerPrimary.setTextColor(Color.parseColor("#1DB954"));
                if (iconSpeakerPrimary != null) iconSpeakerPrimary.setText("🎵");
                Toast.makeText(this, "▶ Speelt Spotify op " + speakerName + "...", Toast.LENGTH_SHORT).show();
            } else {
                tvSpeakerPrimary.setText("Standby");
                tvSpeakerPrimary.setTextColor(Color.parseColor("#94A3B8"));
                if (iconSpeakerPrimary != null) iconSpeakerPrimary.setText("🔊");
                Toast.makeText(this, "⏸ " + speakerName + " gepauzeerd", Toast.LENGTH_SHORT).show();
            }
        });

        cardSpeakerSecondary.setOnClickListener(v -> {
            speakerSecondaryPlaying[0] = !speakerSecondaryPlaying[0];
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_SPEAKER_SECONDARY_PLAYING, speakerSecondaryPlaying[0]);
            if (speakerSecondaryPlaying[0]) {
                editor.putString(KEY_ACTIVE_SPEAKER, "Speaker 2");
            }
            editor.apply();
            updateMusicHeaderUi();

            String query = speakerSecondaryPlaying[0] ? "Speel muziek op Spotify af op Speaker 2" : "Pauzeer Speaker 2";
            sendGoogleHomeCommand(query);
            if (speakerSecondaryPlaying[0]) {
                tvSpeakerSecondary.setText("Spotify • Afspelen");
                tvSpeakerSecondary.setTextColor(Color.parseColor("#1DB954"));
                if (iconSpeakerSecondary != null) iconSpeakerSecondary.setText("🎵");
                Toast.makeText(this, "▶ Speelt Spotify op Speaker 2...", Toast.LENGTH_SHORT).show();
            } else {
                tvSpeakerSecondary.setText("Standby");
                tvSpeakerSecondary.setTextColor(Color.parseColor("#94A3B8"));
                if (iconSpeakerSecondary != null) iconSpeakerSecondary.setText("🔊");
                Toast.makeText(this, "⏸ Speaker 2 gepauzeerd", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnOpenWeb = dialog.findViewById(R.id.btn_open_google_home_web);
        btnOpenWeb.setOnClickListener(v -> {
            dialog.dismiss();
            AndroidSetupHelper.openGoogleHome(this);
        });

        Button btnClose = dialog.findViewById(R.id.btn_close_home);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showMusicPlayerDialog() {
        Dialog dialog = new Dialog(this);
        setupDialogWindow(dialog);
        dialog.setContentView(R.layout.dialog_music_player);

        TextView tvSubtitle = dialog.findViewById(R.id.tv_music_dialog_subtitle);
        TextView badgeStatus = dialog.findViewById(R.id.badge_music_status);

        android.widget.ImageView ivNowPlayingArt = dialog.findViewById(R.id.iv_now_playing_art);
        TextView tvNowPlayingTitle = dialog.findViewById(R.id.tv_now_playing_title);
        TextView tvNowPlayingArtist = dialog.findViewById(R.id.tv_now_playing_artist);
        TextView tvNowPlayingVolume = dialog.findViewById(R.id.tv_now_playing_volume);
        android.widget.ProgressBar pbNowPlayingLoading = dialog.findViewById(R.id.pb_now_playing_loading);

        View cardSelectSpeaker1 = dialog.findViewById(R.id.card_select_speaker_1);
        TextView tvChoiceSpeaker1 = dialog.findViewById(R.id.tv_choice_speaker_1);
        TextView tvSubSpeaker1 = dialog.findViewById(R.id.tv_sub_speaker_1);
        if (tvChoiceSpeaker1 != null) {
            tvChoiceSpeaker1.setText(BuildConfig.DEFAULT_SPEAKER_NAME);
        }

        View cardSelectSpeaker2 = dialog.findViewById(R.id.card_select_speaker_2);
        TextView tvChoiceSpeaker2 = dialog.findViewById(R.id.tv_choice_speaker_2);
        TextView tvSubSpeaker2 = dialog.findViewById(R.id.tv_sub_speaker_2);

        Button btnPrev = dialog.findViewById(R.id.btn_music_prev);
        Button btnPlayPause = dialog.findViewById(R.id.btn_music_play_pause);
        Button btnStop = dialog.findViewById(R.id.btn_music_stop);
        Button btnNext = dialog.findViewById(R.id.btn_music_next);

        Button btnLike = dialog.findViewById(R.id.btn_music_like);
        Button btnVolDown = dialog.findViewById(R.id.btn_music_vol_down);
        Button btnVolUp = dialog.findViewById(R.id.btn_music_vol_up);

        Button btnPresetLow = dialog.findViewById(R.id.btn_preset_low);
        Button btnPresetMid = dialog.findViewById(R.id.btn_preset_mid);
        Button btnPresetHigh = dialog.findViewById(R.id.btn_preset_high);

        Button btnOpenSpotify = dialog.findViewById(R.id.btn_open_spotify_app);
        Button btnClose = dialog.findViewById(R.id.btn_close_music_dialog);

        final String[] activeSpeaker = {prefs.getString(KEY_ACTIVE_SPEAKER, BuildConfig.DEFAULT_SPEAKER_NAME)};
        final String speakerIp = prefs.getString("key_speaker_ip", prefs.getString("key_speaker_jay_ip", CastController.DEFAULT_SPEAKER_IP));

        Runnable updateDialogUi = () -> {
            boolean isSpeaker1 = !"Speaker 2".equals(activeSpeaker[0]);
            tvSubtitle.setText("Bediening van " + activeSpeaker[0] + " (" + BuildConfig.DEFAULT_SPEAKER_ROOM + ")");

            boolean isPlaying = currentCastInfo != null && currentCastInfo.isPlaying();
            boolean hasMedia = currentCastInfo != null && currentCastInfo.hasMedia();

            if (isPlaying) {
                badgeStatus.setText("● Speelt Af");
                badgeStatus.setTextColor(Color.parseColor("#1DB954"));
                btnPlayPause.setText("⏸ Pauzeren");
                btnPlayPause.setTextColor(Color.parseColor("#1DB954"));
            } else if (hasMedia || (currentCastInfo != null && "PAUSED".equalsIgnoreCase(currentCastInfo.playerState))) {
                badgeStatus.setText("⏸ Gepauzeerd");
                badgeStatus.setTextColor(Color.parseColor("#FBBF24"));
                btnPlayPause.setText("▶ Afspelen");
                btnPlayPause.setTextColor(Color.parseColor("#38BDF8"));
            } else {
                badgeStatus.setText("Standby");
                badgeStatus.setTextColor(Color.parseColor("#94A3B8"));
                btnPlayPause.setText("▶ Afspelen");
                btnPlayPause.setTextColor(Color.parseColor("#38BDF8"));
            }

            if (hasMedia) {
                tvNowPlayingTitle.setText(currentCastInfo.title);
                String meta = currentCastInfo.artist != null
                        ? (currentCastInfo.album != null ? currentCastInfo.artist + " • " + currentCastInfo.album : currentCastInfo.artist)
                        : "Spotify";
                tvNowPlayingArtist.setText(meta);
                if (currentCastInfo.imageUrl != null && !currentCastInfo.imageUrl.isEmpty()) {
                    CastController.loadImage(currentCastInfo.imageUrl, ivNowPlayingArt);
                } else {
                    ivNowPlayingArt.setImageResource(R.drawable.ic_music);
                }
            } else {
                tvNowPlayingTitle.setText("Geen muziek actief");
                tvNowPlayingArtist.setText(activeSpeaker[0] + " • Standby");
                ivNowPlayingArt.setImageResource(R.drawable.ic_music);
            }

            if (currentCastInfo != null) {
                int volPct = Math.round(currentCastInfo.volumeLevel * 100);
                tvNowPlayingVolume.setText("🔊 Volume: " + volPct + "%");
            }

            if (isSpeaker1) {
                tvSubSpeaker1.setText(isPlaying ? (BuildConfig.DEFAULT_SPEAKER_ROOM + " • Speelt af") : (BuildConfig.DEFAULT_SPEAKER_ROOM + " • Geselecteerd"));
                tvSubSpeaker1.setTextColor(Color.parseColor("#1DB954"));
                cardSelectSpeaker1.setAlpha(1.0f);

                tvSubSpeaker2.setText("Standby");
                tvSubSpeaker2.setTextColor(Color.parseColor("#94A3B8"));
                cardSelectSpeaker2.setAlpha(0.6f);
            } else {
                tvSubSpeaker2.setText(isPlaying ? "Speelt af" : "Geselecteerd");
                tvSubSpeaker2.setTextColor(Color.parseColor("#1DB954"));
                cardSelectSpeaker2.setAlpha(1.0f);

                tvSubSpeaker1.setText("Standby");
                tvSubSpeaker1.setTextColor(Color.parseColor("#94A3B8"));
                cardSelectSpeaker1.setAlpha(0.6f);
            }
        };

        updateDialogUi.run();

        // Initial fetch from speaker
        if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
        CastController.fetchStatus(speakerIp, new CastController.StatusCallback() {
            @Override
            public void onStatus(CastController.CastMediaInfo info) {
                currentCastInfo = info;
                if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                updateDialogUi.run();
                updateMusicHeaderUi();
            }

            @Override
            public void onError(Exception e) {
                if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
            }
        });

        // Polling loop while dialog is open
        Runnable pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()) {
                    CastController.fetchStatus(speakerIp, new CastController.StatusCallback() {
                        @Override
                        public void onStatus(CastController.CastMediaInfo info) {
                            currentCastInfo = info;
                            updateDialogUi.run();
                            updateMusicHeaderUi();
                        }

                        @Override
                        public void onError(Exception e) {}
                    });
                    castPollHandler.postDelayed(this, 3000);
                }
            }
        };
        castPollHandler.postDelayed(pollRunnable, 3000);
        dialog.setOnDismissListener(d -> {
            castPollHandler.removeCallbacks(pollRunnable);
            if (activeDialog == dialog) {
                activeDialog = null;
            }
        });

        // Switch Speaker Selection
        cardSelectSpeaker1.setOnClickListener(v -> {
            activeSpeaker[0] = BuildConfig.DEFAULT_SPEAKER_NAME;
            prefs.edit().putString(KEY_ACTIVE_SPEAKER, activeSpeaker[0]).apply();
            updateMusicHeaderUi();
            updateDialogUi.run();
            Toast.makeText(this, "🔊 " + activeSpeaker[0] + " geselecteerd", Toast.LENGTH_SHORT).show();
        });

        cardSelectSpeaker2.setOnClickListener(v -> {
            activeSpeaker[0] = "Speaker 2";
            prefs.edit().putString(KEY_ACTIVE_SPEAKER, activeSpeaker[0]).apply();
            updateMusicHeaderUi();
            updateDialogUi.run();
            Toast.makeText(this, "🔊 Speaker 2 geselecteerd", Toast.LENGTH_SHORT).show();
        });

        // Previous Track
        btnPrev.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.prev(speakerIp, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    updateMusicHeaderUi();
                    Toast.makeText(MainActivity.this, "⏮ Vorig nummer", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Speel het vorige nummer af op " + activeSpeaker[0]);
                }
            });
        });

        // Play / Pause Toggle
        btnPlayPause.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.togglePlayPause(speakerIp, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    updateMusicHeaderUi();
                    Toast.makeText(MainActivity.this, info.isPlaying() ? "▶ Afspelen" : "⏸ Gepauzeerd", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Speel muziek op Spotify af op " + activeSpeaker[0]);
                }
            });
        });

        // Stop Track
        btnStop.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.stop(speakerIp, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    updateMusicHeaderUi();
                    Toast.makeText(MainActivity.this, "⏹ Muziek gestopt", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Stop muziek op " + activeSpeaker[0]);
                }
            });
        });

        // Next Track
        btnNext.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.next(speakerIp, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    updateMusicHeaderUi();
                    Toast.makeText(MainActivity.this, "⏭ Volgende nummer", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Speel het volgende nummer af op " + activeSpeaker[0]);
                }
            });
        });

        // Like / Favorite
        btnLike.setOnClickListener(v -> {
            Toast.makeText(this, "❤️ Opgeslagen in favorieten!", Toast.LENGTH_SHORT).show();
        });

        // Volume Down -5%
        btnVolDown.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.adjustVolume(speakerIp, -0.05f, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    int pct = Math.round(info.volumeLevel * 100);
                    Toast.makeText(MainActivity.this, "🔉 Volume: " + pct + "% (-5%)", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Zet volume van " + activeSpeaker[0] + " 5 procent zachter");
                }
            });
        });

        // Volume Up +5%
        btnVolUp.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.adjustVolume(speakerIp, +0.05f, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    int pct = Math.round(info.volumeLevel * 100);
                    Toast.makeText(MainActivity.this, "🔊 Volume: " + pct + "% (+5%)", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Zet volume van " + activeSpeaker[0] + " 5 procent harder");
                }
            });
        });

        // Volume Preset 1: Laag (20%)
        btnPresetLow.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.setVolume(speakerIp, 0.20f, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    Toast.makeText(MainActivity.this, "1️⃣ Volume Laag: 20%", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Zet volume van " + activeSpeaker[0] + " op 20 procent");
                }
            });
        });

        // Volume Preset 2: Midden (50%)
        btnPresetMid.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.setVolume(speakerIp, 0.50f, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    Toast.makeText(MainActivity.this, "2️⃣ Volume Midden: 50%", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Zet volume van " + activeSpeaker[0] + " op 50 procent");
                }
            });
        });

        // Volume Preset 3: Hoog (75%)
        btnPresetHigh.setOnClickListener(v -> {
            if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.VISIBLE);
            CastController.setVolume(speakerIp, 0.75f, new CastController.ActionCallback() {
                @Override
                public void onSuccess(CastController.CastMediaInfo info) {
                    currentCastInfo = info;
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    updateDialogUi.run();
                    Toast.makeText(MainActivity.this, "3️⃣ Volume Hoog: 75%", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    if (pbNowPlayingLoading != null) pbNowPlayingLoading.setVisibility(View.GONE);
                    sendGoogleHomeCommand("Zet volume van " + activeSpeaker[0] + " op 75 procent");
                }
            });
        });

        // Remote Key Shortcuts (Number keys 1, 2, 3 and Back)
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_NUMPAD_1) {
                    btnPresetLow.performClick();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_2 || keyCode == KeyEvent.KEYCODE_NUMPAD_2) {
                    btnPresetMid.performClick();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_3 || keyCode == KeyEvent.KEYCODE_NUMPAD_3) {
                    btnPresetHigh.performClick();
                    return true;
                }
            }
            return false;
        });

        // Open Spotify TV App
        btnOpenSpotify.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.spotify.tv.android");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    Toast.makeText(this, "Spotify TV app niet gevonden", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Kan Spotify niet openen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Close
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Default focus on Play/Pause button for instant remote control
        btnPlayPause.post(btnPlayPause::requestFocus);
    }

    private void sendGoogleHomeCommand(String query) {
        try {
            Intent intent = new Intent("android.speech.action.WEB_SEARCH");
            intent.putExtra("query", query);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Fout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddWebAppDialog() {
        Dialog dialog = new Dialog(this);
        setupDialogWindow(dialog);
        dialog.setContentView(R.layout.dialog_add_web_app);

        EditText etTitle = dialog.findViewById(R.id.et_web_title);
        EditText etUrl = dialog.findViewById(R.id.et_web_url);
        Button btnSave = dialog.findViewById(R.id.btn_dialog_save);
        Button btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);

        // Presets
        dialog.findViewById(R.id.preset_twitch).setOnClickListener(v -> {
            etTitle.setText("Twitch TV");
            etUrl.setText("https://m.twitch.tv");
        });
        dialog.findViewById(R.id.preset_stremio).setOnClickListener(v -> {
            etTitle.setText("Stremio Web");
            etUrl.setText("https://web.stremio.com");
        });
        dialog.findViewById(R.id.preset_nos).setOnClickListener(v -> {
            etTitle.setText("NOS Nieuws");
            etUrl.setText("https://nos.nl");
        });
        dialog.findViewById(R.id.preset_reddit).setOnClickListener(v -> {
            etTitle.setText("Reddit");
            etUrl.setText("https://sh.reddit.com");
        });
        dialog.findViewById(R.id.preset_soundcloud).setOnClickListener(v -> {
            etTitle.setText("SoundCloud");
            etUrl.setText("https://soundcloud.com");
        });
        dialog.findViewById(R.id.preset_kick).setOnClickListener(v -> {
            etTitle.setText("Kick");
            etUrl.setText("https://kick.com");
        });
        dialog.findViewById(R.id.preset_ha).setOnClickListener(v -> {
            etTitle.setText("Home Assistant");
            etUrl.setText("http://homeassistant.local:8123");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String url = etUrl.getText().toString().trim();

            if (url.isEmpty()) {
                Toast.makeText(this, "Vul een geldige URL in", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            if (title.isEmpty()) {
                title = "Web App";
            }

            String letter = title.substring(0, 1).toUpperCase();
            String[] colors = {"#3B82F6", "#8B5CF6", "#EC4899", "#10B981", "#F59E0B", "#06B6D4", "#6366F1"};
            String randomColor = colors[(int) (Math.random() * colors.length)];

            String id = "web_" + System.currentTimeMillis();
            WebAppInfo newApp = new WebAppInfo(id, title, url, randomColor, letter);
            addWebApp(newApp);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        scrollToTopAndFocus();
    }

    private void scrollToTopAndFocus() {
        if (mainScrollView != null) {
            mainScrollView.smoothScrollTo(0, 0);
        }
        if (rvFavorites != null && rvFavorites.getChildCount() > 0) {
            rvFavorites.getChildAt(0).requestFocus();
        } else {
            View homeBtn = findViewById(R.id.btn_google_home);
            if (homeBtn != null) {
                homeBtn.requestFocus();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
            return;
        }
        // On TV home launcher, Back key returns to top favorites row
        scrollToTopAndFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}
