package com.aura.tvlauncher;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    // Sidebar Items
    private LinearLayout sidebarNetwork;
    private LinearLayout sidebarStreamTv;
    private LinearLayout sidebarSpeaker;
    private LinearLayout sidebarSystem;
    private LinearLayout sidebarPreferences;
    private LinearLayout sidebarAbout;
    private Button btnSettingsBack;

    // Panels
    private LinearLayout panelNetwork;
    private LinearLayout panelStreamTv;
    private LinearLayout panelSpeaker;
    private LinearLayout panelSystem;
    private LinearLayout panelPreferences;
    private LinearLayout panelAbout;

    // Network Views
    private TextView tvNetStatus;
    private TextView tvNetIp;
    private TextView tvAdBlockDesc;
    private Button btnToggleAdBlock;
    private Button btnNetTest;

    // Stream TV Views
    private TextView tvActiveProvider;
    private TextView tvProviderUrl;
    private Button btnOpenStreamTv;
    private Button btnChangeProvider;
    private Button btnToggleDirectStart;
    private Button btnClearTvCookies;

    // Speaker Views
    private TextView tvSpeakerName;
    private TextView tvSpeakerRoom;
    private TextView tvSpeakerIp;
    private Button btnScanSpeakers;
    private Button btnOpenGoogleHome;
    private Button btnEditSpeaker;
    private Button btnTestSpeaker;
    private TextView tvSettingsDiscoveryStatus;
    private LinearLayout llSettingsDiscoveredSpeakers;
    private CastDiscoveryManager discoveryManager;

    // System Views
    private TextView tvRamStats;
    private ProgressBar pbRam;
    private Button btnCleanRam;
    private Button btnOpenAndroidSettings;

    // Preferences & About Views
    private Button btnRunSetup;
    private TextView tvAboutDeviceModel;
    private TextView tvAboutAndroidVer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);

        initViews();
        setupSidebarNavigation();
        loadAllSettingsData();

        // Default select Network
        selectCategory(0);
    }

    private void initViews() {
        sidebarNetwork = findViewById(R.id.sidebar_cat_network);
        sidebarStreamTv = findViewById(R.id.sidebar_cat_stream_tv);
        sidebarSpeaker = findViewById(R.id.sidebar_cat_speaker);
        sidebarSystem = findViewById(R.id.sidebar_cat_system);
        sidebarPreferences = findViewById(R.id.sidebar_cat_preferences);
        sidebarAbout = findViewById(R.id.sidebar_cat_about);
        btnSettingsBack = findViewById(R.id.btn_settings_back);

        panelNetwork = findViewById(R.id.panel_network);
        panelStreamTv = findViewById(R.id.panel_stream_tv);
        panelSpeaker = findViewById(R.id.panel_speaker);
        panelSystem = findViewById(R.id.panel_system);
        panelPreferences = findViewById(R.id.panel_preferences);
        panelAbout = findViewById(R.id.panel_about);

        // Network
        tvNetStatus = findViewById(R.id.tv_net_status);
        tvNetIp = findViewById(R.id.tv_net_ip);
        tvAdBlockDesc = findViewById(R.id.tv_settings_adblock_desc);
        btnToggleAdBlock = findViewById(R.id.btn_settings_toggle_adblock);
        btnNetTest = findViewById(R.id.btn_net_test);

        // Stream TV
        tvActiveProvider = findViewById(R.id.tv_settings_active_provider);
        tvProviderUrl = findViewById(R.id.tv_settings_provider_url);
        btnOpenStreamTv = findViewById(R.id.btn_settings_open_stream_tv);
        btnChangeProvider = findViewById(R.id.btn_settings_change_provider);
        btnToggleDirectStart = findViewById(R.id.btn_settings_direct_start_toggle);
        btnClearTvCookies = findViewById(R.id.btn_settings_clear_tv_cookies);

        // Speaker
        tvSpeakerName = findViewById(R.id.tv_settings_speaker_name);
        tvSpeakerRoom = findViewById(R.id.tv_settings_speaker_room);
        tvSpeakerIp = findViewById(R.id.tv_settings_speaker_ip);
        btnScanSpeakers = findViewById(R.id.btn_settings_scan_speakers);
        btnOpenGoogleHome = findViewById(R.id.btn_settings_open_google_home);
        btnEditSpeaker = findViewById(R.id.btn_settings_edit_speaker);
        btnTestSpeaker = findViewById(R.id.btn_settings_test_speaker);
        tvSettingsDiscoveryStatus = findViewById(R.id.tv_settings_discovery_status);
        llSettingsDiscoveredSpeakers = findViewById(R.id.ll_settings_discovered_speakers);

        // System
        tvRamStats = findViewById(R.id.tv_settings_ram_stats);
        pbRam = findViewById(R.id.pb_settings_ram);
        btnCleanRam = findViewById(R.id.btn_settings_clean_ram);
        btnOpenAndroidSettings = findViewById(R.id.btn_settings_open_android_settings);

        // Preferences & About
        btnRunSetup = findViewById(R.id.btn_settings_run_setup);
        tvAboutDeviceModel = findViewById(R.id.tv_about_device_model);
        tvAboutAndroidVer = findViewById(R.id.tv_about_android_ver);

        btnSettingsBack.setOnClickListener(v -> finish());
    }

    private void setupSidebarNavigation() {
        sidebarNetwork.setOnClickListener(v -> selectCategory(0));
        sidebarStreamTv.setOnClickListener(v -> selectCategory(1));
        sidebarSpeaker.setOnClickListener(v -> selectCategory(2));
        sidebarSystem.setOnClickListener(v -> selectCategory(3));
        sidebarPreferences.setOnClickListener(v -> selectCategory(4));
        sidebarAbout.setOnClickListener(v -> selectCategory(5));

        // When navigating with D-pad
        sidebarNetwork.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectCategory(0); });
        sidebarStreamTv.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectCategory(1); });
        sidebarSpeaker.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectCategory(2); });
        sidebarSystem.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectCategory(3); });
        sidebarPreferences.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectCategory(4); });
        sidebarAbout.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectCategory(5); });
    }

    private void selectCategory(int index) {
        sidebarNetwork.setSelected(index == 0);
        sidebarStreamTv.setSelected(index == 1);
        sidebarSpeaker.setSelected(index == 2);
        sidebarSystem.setSelected(index == 3);
        sidebarPreferences.setSelected(index == 4);
        sidebarAbout.setSelected(index == 5);

        panelNetwork.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        panelStreamTv.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        panelSpeaker.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        panelSystem.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        panelPreferences.setVisibility(index == 4 ? View.VISIBLE : View.GONE);
        panelAbout.setVisibility(index == 5 ? View.VISIBLE : View.GONE);
    }

    private void loadAllSettingsData() {
        loadNetworkSettings();
        loadStreamTvSettings();
        loadSpeakerSettings();
        loadSystemRamSettings();
        loadPreferencesAndAbout();
    }

    // 1. NETWERK & WI-FI
    private void loadNetworkSettings() {
        tvNetIp.setText(getDeviceIpAddress());

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo active = cm.getActiveNetworkInfo();
            if (active != null && active.isConnected()) {
                if (active.getType() == ConnectivityManager.TYPE_WIFI) {
                    tvNetStatus.setText("Verbonden via Wi-Fi");
                    tvNetStatus.setTextColor(Color.parseColor("#34D399"));
                } else if (active.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    tvNetStatus.setText("Verbonden via Ethernet (Kabel)");
                    tvNetStatus.setTextColor(Color.parseColor("#34D399"));
                } else {
                    tvNetStatus.setText("Verbonden: " + active.getTypeName());
                    tvNetStatus.setTextColor(Color.parseColor("#34D399"));
                }
            } else {
                tvNetStatus.setText("Niet verbonden");
                tvNetStatus.setTextColor(Color.parseColor("#EF4444"));
            }
        }

        boolean adblockOn = prefs.getBoolean("key_adblock_enabled", true);
        updateAdBlockUi(adblockOn);

        btnToggleAdBlock.setOnClickListener(v -> {
            boolean current = prefs.getBoolean("key_adblock_enabled", true);
            boolean newState = !current;
            prefs.edit().putBoolean("key_adblock_enabled", newState).apply();
            AdBlocker.setEnabled(newState);

            new Thread(() -> {
                try {
                    String mode = newState ? "hostname" : "off";
                    String dns = newState ? "dns.adguard-dns.com" : "";
                    Settings.Global.putString(getContentResolver(), "private_dns_mode", mode);
                    Settings.Global.putString(getContentResolver(), "private_dns_specifier", dns);
                } catch (Exception e) {
                    try {
                        String mode = newState ? "hostname" : "off";
                        String dns = newState ? "dns.adguard-dns.com" : "";
                        Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "private_dns_mode", mode});
                        Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "private_dns_specifier", dns});
                    } catch (Exception ignored) {}
                }
            }).start();

            updateAdBlockUi(newState);
            Toast.makeText(this, newState ? "🛡️ AdBlocker Ingeschakeld" : "⚠️ AdBlocker Uitgeschakeld", Toast.LENGTH_SHORT).show();
        });

        btnNetTest.setOnClickListener(v -> {
            btnNetTest.setEnabled(false);
            btnNetTest.setText("⏳ Verbinding testen...");
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        InetAddress address = InetAddress.getByName("8.8.8.8");
                        return address.isReachable(2000);
                    } catch (Exception e) {
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    btnNetTest.setEnabled(true);
                    btnNetTest.setText("⚡ Netwerkverbinding Testen");
                    if (success) {
                        Toast.makeText(SettingsActivity.this, "✅ Internetverbinding is stabiel & actief!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "⚠️ Kan geen verbinding maken met internet", Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        });
    }

    private void updateAdBlockUi(boolean enabled) {
        if (enabled) {
            btnToggleAdBlock.setText("INGESCHAKELD");
            btnToggleAdBlock.setTextColor(Color.parseColor("#34D399"));
            tvAdBlockDesc.setText("Actief: Video advertenties & trackers worden geblokkeerd");
        } else {
            btnToggleAdBlock.setText("UITGESCHAKELD");
            btnToggleAdBlock.setTextColor(Color.parseColor("#EF4444"));
            tvAdBlockDesc.setText("Uitgeschakeld: Advertenties worden doorgelaten");
        }
    }

    private String getDeviceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.1.100";
    }

    // 2. STREAM TV & PROVIDER
    private void loadStreamTvSettings() {
        TvProvider provider = TvProvider.getDefaultProvider(this);
        tvActiveProvider.setText(provider.getName() + " (" + provider.getTagline() + ")");
        tvProviderUrl.setText(provider.getUrl());

        boolean directStart = TvProvider.isDirectStartEnabled(this);
        updateDirectStartUi(directStart);

        btnToggleDirectStart.setOnClickListener(v -> {
            boolean current = TvProvider.isDirectStartEnabled(this);
            TvProvider.setDirectStartEnabled(this, !current);
            updateDirectStartUi(!current);
            Toast.makeText(this, !current ? "⚡ Direct starten ingeschakeld" : "Direct starten uitgeschakeld", Toast.LENGTH_SHORT).show();
        });

        btnOpenStreamTv.setOnClickListener(v -> {
            Intent intent = new Intent(this, StreamTvActivity.class);
            startActivity(intent);
        });

        btnChangeProvider.setOnClickListener(v -> showChangeProviderDialog());

        btnClearTvCookies.setOnClickListener(v -> {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            Toast.makeText(this, "🗑️ TV Web Cookies & Sessies gewist", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDirectStartUi(boolean enabled) {
        if (enabled) {
            btnToggleDirectStart.setText("AAN");
            btnToggleDirectStart.setTextColor(Color.parseColor("#34D399"));
        } else {
            btnToggleDirectStart.setText("UIT");
            btnToggleDirectStart.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void showChangeProviderDialog() {
        List<TvProvider> list = TvProvider.getStandardProviders(this);
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = list.get(i).getIconEmoji() + "  " + list.get(i).getName() + " (" + list.get(i).getTagline() + ")";
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Kies Standaard TV Provider")
                .setItems(names, (dialog, which) -> {
                    TvProvider selected = list.get(which);
                    TvProvider.setDefaultProvider(this, selected.getId());
                    tvActiveProvider.setText(selected.getName() + " (" + selected.getTagline() + ")");
                    tvProviderUrl.setText(selected.getUrl());
                    Toast.makeText(this, "★ " + selected.getName() + " ingesteld als standaard TV provider", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuleren", null)
                .show();
    }

    // 3. SLIMME SPEAKER & CAST
    private void loadSpeakerSettings() {
        String name = prefs.getString("key_speaker_name", BuildConfig.DEFAULT_SPEAKER_NAME);
        String room = prefs.getString("key_speaker_room", BuildConfig.DEFAULT_SPEAKER_ROOM);
        String ip = prefs.getString("key_speaker_ip", CastController.DEFAULT_SPEAKER_IP);

        tvSpeakerName.setText(!name.isEmpty() ? name : "Smart Speaker");
        tvSpeakerRoom.setText(!ip.isEmpty() ? (room + " • Actief") : "Niet gekoppeld");
        tvSpeakerIp.setText(!ip.isEmpty() ? ip : "Geen IP ingesteld");

        discoveryManager = new CastDiscoveryManager(this);

        btnScanSpeakers.setOnClickListener(v -> {
            btnScanSpeakers.setEnabled(false);
            btnScanSpeakers.setText("⏳ Zoeken...");
            tvSettingsDiscoveryStatus.setText("Zoeken naar Google Home & Nest speakers op Wi-Fi...");
            discoveryManager.startDiscovery(new CastDiscoveryManager.DiscoveryCallback() {
                @Override
                public void onDeviceFound(CastDevice device) {
                    runOnUiThread(() -> {
                        List<CastDevice> current = discoveryManager.getDiscoveredDevices();
                        displayDiscoveredSpeakers(current);
                        tvSettingsDiscoveryStatus.setText("Gevonden: " + current.size() + " apparaat(en) op netwerk");
                    });
                }

                @Override
                public void onDiscoveryComplete(List<CastDevice> devices) {
                    runOnUiThread(() -> {
                        btnScanSpeakers.setEnabled(true);
                        btnScanSpeakers.setText("🔍 Zoek Speakers");
                        displayDiscoveredSpeakers(devices);
                        if (devices.isEmpty()) {
                            tvSettingsDiscoveryStatus.setText("Geen Google Cast apparaten gevonden op dit netwerk.");
                        } else {
                            tvSettingsDiscoveryStatus.setText("✓ " + devices.size() + " apparaat(en) gevonden. Tik om te koppelen:");
                        }
                    });
                }
            });
        });

        btnOpenGoogleHome.setOnClickListener(v -> AndroidSetupHelper.openGoogleHome(this));
        btnEditSpeaker.setOnClickListener(v -> showEditSpeakerDialog());

        btnTestSpeaker.setOnClickListener(v -> {
            String currentIp = prefs.getString("key_speaker_ip", "");
            if (currentIp.isEmpty()) {
                Toast.makeText(SettingsActivity.this, "Configureer eerst een speaker IP", Toast.LENGTH_SHORT).show();
                return;
            }
            btnTestSpeaker.setEnabled(false);
            btnTestSpeaker.setText("⏳ Testen...");
            CastController.fetchStatus(currentIp, new CastController.StatusCallback() {
                @Override
                public void onStatus(CastController.CastMediaInfo info) {
                    btnTestSpeaker.setEnabled(true);
                    btnTestSpeaker.setText("🔊 Testen");
                    int vol = Math.round(info.volumeLevel * 100);
                    Toast.makeText(SettingsActivity.this, "✅ Verbonden met speaker! Volume: " + vol + "%", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(Exception e) {
                    btnTestSpeaker.setEnabled(true);
                    btnTestSpeaker.setText("🔊 Testen");
                    Toast.makeText(SettingsActivity.this, "⚠️ Kan speaker niet bereiken op " + currentIp, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void displayDiscoveredSpeakers(List<CastDevice> devices) {
        llSettingsDiscoveredSpeakers.removeAllViews();
        if (devices == null || devices.isEmpty()) return;

        String currentIp = prefs.getString("key_speaker_ip", "");
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        for (CastDevice device : devices) {
            View itemView = inflater.inflate(R.layout.item_discovered_speaker, llSettingsDiscoveredSpeakers, false);
            TextView tvIcon = itemView.findViewById(R.id.tv_speaker_item_icon);
            TextView tvName = itemView.findViewById(R.id.tv_speaker_item_name);
            TextView tvDetails = itemView.findViewById(R.id.tv_speaker_item_details);
            TextView tvStatus = itemView.findViewById(R.id.tv_speaker_item_status);

            tvName.setText(device.getName());

            String details = device.getIp();
            if (device.getModel() != null && !device.getModel().isEmpty()) {
                details += " • " + device.getModel();
            }
            if (device.getRoom() != null && !device.getRoom().isEmpty()) {
                details += " • " + device.getRoom();
            }
            tvDetails.setText(details);

            String nameLower = device.getName().toLowerCase();
            String modelLower = device.getModel().toLowerCase();
            if (modelLower.contains("tv") || nameLower.contains("tv") || nameLower.contains("screen")) {
                tvIcon.setText("📺");
            } else {
                tvIcon.setText("🔊");
            }

            boolean isSelected = device.getIp().equalsIgnoreCase(currentIp);
            if (isSelected) {
                tvStatus.setText("✓ Gekoppeld");
                tvStatus.setTextColor(Color.parseColor("#34D399"));
                tvStatus.setBackgroundResource(R.drawable.bg_badge);
                itemView.setAlpha(1.0f);
            } else {
                tvStatus.setText("Koppelen");
                tvStatus.setTextColor(Color.parseColor("#38BDF8"));
                tvStatus.setBackgroundResource(R.drawable.bg_button_selector);
                itemView.setAlpha(0.9f);
            }

            itemView.setOnClickListener(v -> {
                prefs.edit().putString("key_speaker_name", device.getName()).apply();
                prefs.edit().putString("key_speaker_room", device.getRoom()).apply();
                prefs.edit().putString("key_speaker_ip", device.getIp()).apply();
                loadSpeakerSettings();
                Toast.makeText(this, "✓ " + device.getName() + " gekoppeld als actieve speaker!", Toast.LENGTH_SHORT).show();
                displayDiscoveredSpeakers(discoveryManager.getDiscoveredDevices());
            });

            llSettingsDiscoveredSpeakers.addView(itemView);
        }
    }

    private void showEditSpeakerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Speaker Handmatig Configureren");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Speaker Naam (bijv. Nest Audio)");
        etName.setText(prefs.getString("key_speaker_name", ""));
        layout.addView(etName);

        final EditText etRoom = new EditText(this);
        etRoom.setHint("Kamer (bijv. Woonkamer)");
        etRoom.setText(prefs.getString("key_speaker_room", ""));
        layout.addView(etRoom);

        final EditText etIp = new EditText(this);
        etIp.setHint("Speaker IP (bijv. 192.168.1.100)");
        etIp.setText(prefs.getString("key_speaker_ip", ""));
        layout.addView(etIp);

        builder.setView(layout);

        builder.setPositiveButton("Opslaan", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newRoom = etRoom.getText().toString().trim();
            String newIp = etIp.getText().toString().trim();

            if (!newName.isEmpty()) prefs.edit().putString("key_speaker_name", newName).apply();
            if (!newRoom.isEmpty()) prefs.edit().putString("key_speaker_room", newRoom).apply();
            if (!newIp.isEmpty()) {
                prefs.edit().putString("key_speaker_ip", newIp).apply();
            }

            loadSpeakerSettings();
            Toast.makeText(this, "Speaker gegevens opgeslagen!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Annuleren", null);
        builder.show();
    }

    // 4. SYSTEEM & GEHEUGEN (RAM)
    private void loadSystemRamSettings() {
        updateRamStats();

        btnCleanRam.setOnClickListener(v -> {
            System.gc();
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.killBackgroundProcesses(getPackageName());
                }
            } catch (Exception ignored) {}
            updateRamStats();
            Toast.makeText(this, "🧹 Geheugen succesvol opgeschoond!", Toast.LENGTH_SHORT).show();
        });

        btnOpenAndroidSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Kan Android instellingen niet openen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRamStats() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                long freeMb = mi.availMem / (1024 * 1024);
                long totalMb = mi.totalMem / (1024 * 1024);
                long usedMb = totalMb - freeMb;
                int percentUsed = (int) ((usedMb * 100) / totalMb);

                tvRamStats.setText(String.format(Locale.getDefault(), "%d MB Vrij / %d MB", freeMb, totalMb));
                pbRam.setProgress(percentUsed);
            }
        } catch (Exception ignored) {}
    }

    // 5. VOORKEUREN & OVER AURAOS
    private void loadPreferencesAndAbout() {
        btnRunSetup.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetupActivity.class);
            startActivity(intent);
        });

        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toUpperCase() : "PHILIPS";
        String model = Build.MODEL != null ? Build.MODEL : "32PFS6906/12";
        tvAboutDeviceModel.setText(manufacturer + " Ambilight (" + model + ")");
        tvAboutAndroidVer.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (discoveryManager != null) {
            discoveryManager.stopDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (discoveryManager != null) {
            discoveryManager.stopDiscovery();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
