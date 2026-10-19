package com.aura.tvlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SetupActivity extends AppCompatActivity {

    public static final String KEY_SETUP_COMPLETED = "key_auraos_setup_completed";

    private int currentStep = 1;
    private SharedPreferences prefs;

    private TextView tvStepIndicator;

    // Steps
    private LinearLayout step1;
    private LinearLayout step2;
    private ScrollView step3;
    private LinearLayout step4;
    private LinearLayout step5;

    // Nav
    private LinearLayout llNavBar;
    private Button btnPrev;
    private Button btnNext;

    // Step 1
    private Button btnStart;

    // Step 2
    private RecyclerView rvProviders;
    private TvProviderAdapter providerAdapter;
    private String selectedProviderId = "bbhtv";

    // Step 3
    private TextView tvSetupAndroidNet;
    private TextView tvSetupAndroidAccount;
    private ProgressBar pbSetupScanning;
    private TextView tvSetupScanStatus;
    private Button btnSetupRescan;
    private Button btnSetupOpenGoogleHome;
    private LinearLayout llSetupDiscoveredSpeakers;
    private TextView tvSetupNoSpeakers;
    private TextView tvSetupSelectedSpeakerBadge;
    private View cardSetupAdvancedToggle;
    private TextView tvSetupAdvancedArrow;
    private LinearLayout llSetupAdvancedContainer;
    private EditText etSpeakerName;
    private EditText etSpeakerRoom;
    private EditText etSpeakerIp;
    private Button btnSetupTestManualIp;

    private CastDiscoveryManager discoveryManager;
    private String selectedSpeakerIp = "";
    private String selectedSpeakerName = "";
    private String selectedSpeakerRoom = "";
    private boolean isAdvancedExpanded = false;

    // Step 4
    private LinearLayout cardAdblockEnable;
    private LinearLayout cardAdblockDisable;
    private boolean adblockEnabled = true;

    // Step 5
    private TextView tvSummaryProvider;
    private TextView tvSummarySpeaker;
    private TextView tvSummaryAdblock;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        prefs = getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);

        initViews();
        setupStep2Providers();
        setupStep3Speaker();
        setupStep4Adblock();
        showStep(1);
    }

    private void initViews() {
        tvStepIndicator = findViewById(R.id.tv_step_indicator);

        step1 = findViewById(R.id.setup_step_1);
        step2 = findViewById(R.id.setup_step_2);
        step3 = findViewById(R.id.setup_step_3);
        step4 = findViewById(R.id.setup_step_4);
        step5 = findViewById(R.id.setup_step_5);

        llNavBar = findViewById(R.id.ll_setup_nav_bar);
        btnPrev = findViewById(R.id.btn_setup_prev);
        btnNext = findViewById(R.id.btn_setup_next);

        btnStart = findViewById(R.id.btn_setup_start);
        rvProviders = findViewById(R.id.rv_setup_providers);

        // Step 3 views
        tvSetupAndroidNet = findViewById(R.id.tv_setup_android_net);
        tvSetupAndroidAccount = findViewById(R.id.tv_setup_android_account);
        pbSetupScanning = findViewById(R.id.pb_setup_scanning);
        tvSetupScanStatus = findViewById(R.id.tv_setup_scan_status);
        btnSetupRescan = findViewById(R.id.btn_setup_rescan);
        btnSetupOpenGoogleHome = findViewById(R.id.btn_setup_open_google_home);
        llSetupDiscoveredSpeakers = findViewById(R.id.ll_setup_discovered_speakers);
        tvSetupNoSpeakers = findViewById(R.id.tv_setup_no_speakers);
        tvSetupSelectedSpeakerBadge = findViewById(R.id.tv_setup_selected_speaker_badge);
        cardSetupAdvancedToggle = findViewById(R.id.card_setup_advanced_toggle);
        tvSetupAdvancedArrow = findViewById(R.id.tv_setup_advanced_arrow);
        llSetupAdvancedContainer = findViewById(R.id.ll_setup_advanced_container);
        etSpeakerName = findViewById(R.id.et_setup_speaker_name);
        etSpeakerRoom = findViewById(R.id.et_setup_speaker_room);
        etSpeakerIp = findViewById(R.id.et_setup_speaker_ip);
        btnSetupTestManualIp = findViewById(R.id.btn_setup_test_manual_ip);

        // Step 4 views
        cardAdblockEnable = findViewById(R.id.card_setup_adblock_enable);
        cardAdblockDisable = findViewById(R.id.card_setup_adblock_disable);

        // Step 5 views
        tvSummaryProvider = findViewById(R.id.tv_summary_provider);
        tvSummarySpeaker = findViewById(R.id.tv_summary_speaker);
        tvSummaryAdblock = findViewById(R.id.tv_summary_adblock);
        btnFinish = findViewById(R.id.btn_setup_finish);

        btnStart.setOnClickListener(v -> showStep(2));

        btnPrev.setOnClickListener(v -> {
            if (currentStep > 1) {
                showStep(currentStep - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentStep < 5) {
                showStep(currentStep + 1);
            }
        });

        btnFinish.setOnClickListener(v -> saveAllAndFinish());
    }

    private void setupStep2Providers() {
        List<TvProvider> list = TvProvider.getStandardProviders(this);
        selectedProviderId = prefs.getString(TvProvider.PREF_DEFAULT_PROVIDER_ID, "bbhtv");

        providerAdapter = new TvProviderAdapter(this, list, selectedProviderId, new TvProviderAdapter.OnProviderClickListener() {
            @Override
            public void onProviderSelected(TvProvider provider) {
                selectedProviderId = provider.getId();
                providerAdapter.setDefaultProviderId(selectedProviderId);
                Toast.makeText(SetupActivity.this, "✓ " + provider.getName() + " geselecteerd", Toast.LENGTH_SHORT).show();
                showStep(3);
            }

            @Override
            public void onProviderDefaultChanged(TvProvider provider) {
                selectedProviderId = provider.getId();
            }
        });

        rvProviders.setLayoutManager(new GridLayoutManager(this, 2));
        rvProviders.setAdapter(providerAdapter);
    }

    private void setupStep3Speaker() {
        discoveryManager = new CastDiscoveryManager(this);

        // Sync Android TV setup network & account info
        AndroidSetupHelper.NetworkSetupInfo netInfo = AndroidSetupHelper.getNetworkInfo(this);
        tvSetupAndroidNet.setText(netInfo.getSummary());

        AndroidSetupHelper.GoogleAccountInfo accInfo = AndroidSetupHelper.getGoogleAccountInfo(this);
        tvSetupAndroidAccount.setText(accInfo.getSummary());

        // Action buttons
        btnSetupRescan.setOnClickListener(v -> startSpeakerDiscovery());
        btnSetupOpenGoogleHome.setOnClickListener(v -> AndroidSetupHelper.openGoogleHome(this));

        // Advanced collapsible toggle
        cardSetupAdvancedToggle.setOnClickListener(v -> {
            isAdvancedExpanded = !isAdvancedExpanded;
            llSetupAdvancedContainer.setVisibility(isAdvancedExpanded ? View.VISIBLE : View.GONE);
            tvSetupAdvancedArrow.setText(isAdvancedExpanded ? "▲" : "▼");
            if (isAdvancedExpanded) {
                etSpeakerName.requestFocus();
            }
        });

        // Test manual IP
        btnSetupTestManualIp.setOnClickListener(v -> {
            String ip = etSpeakerIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(SetupActivity.this, "Voer eerst een IP-adres in", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSetupTestManualIp.setText("⏳");
            btnSetupTestManualIp.setEnabled(false);
            CastController.fetchStatus(ip, new CastController.StatusCallback() {
                @Override
                public void onStatus(CastController.CastMediaInfo info) {
                    btnSetupTestManualIp.setText("🔊 Testen");
                    btnSetupTestManualIp.setEnabled(true);
                    selectedSpeakerIp = ip;
                    if (etSpeakerName.getText().toString().trim().isEmpty()) {
                        String name = info.appName != null && !info.appName.isEmpty() && !"Backdrop".equalsIgnoreCase(info.appName)
                                ? info.appName + " Speaker"
                                : "Google Cast Speaker";
                        etSpeakerName.setText(name);
                        selectedSpeakerName = name;
                    }
                    tvSetupSelectedSpeakerBadge.setText("✓ Verbinding gelukt met " + ip);
                    tvSetupSelectedSpeakerBadge.setVisibility(View.VISIBLE);
                    Toast.makeText(SetupActivity.this, "✅ Verbinding geslaagd!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    btnSetupTestManualIp.setText("🔊 Testen");
                    btnSetupTestManualIp.setEnabled(true);
                    Toast.makeText(SetupActivity.this, "⚠️ Kan speaker niet bereiken op " + ip, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Restore any previously configured values if user reopened setup
        String savedIp = prefs.getString("key_speaker_ip", "");
        if (!savedIp.isEmpty()) {
            selectedSpeakerIp = savedIp;
            selectedSpeakerName = prefs.getString("key_speaker_name", "Smart Speaker");
            selectedSpeakerRoom = prefs.getString("key_speaker_room", "Woonkamer");
            etSpeakerIp.setText(selectedSpeakerIp);
            etSpeakerName.setText(selectedSpeakerName);
            etSpeakerRoom.setText(selectedSpeakerRoom);
            tvSetupSelectedSpeakerBadge.setText("✓ Gekoppeld aan: " + selectedSpeakerName + " (" + selectedSpeakerIp + ")");
            tvSetupSelectedSpeakerBadge.setVisibility(View.VISIBLE);
        }
    }

    private void displayDiscoveredDevices(List<CastDevice> devices) {
        llSetupDiscoveredSpeakers.removeAllViews();
        if (devices == null || devices.isEmpty()) {
            tvSetupNoSpeakers.setVisibility(View.VISIBLE);
            return;
        }
        tvSetupNoSpeakers.setVisibility(View.GONE);

        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        for (CastDevice device : devices) {
            View itemView = inflater.inflate(R.layout.item_discovered_speaker, llSetupDiscoveredSpeakers, false);
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

            boolean isSelected = device.getIp().equalsIgnoreCase(selectedSpeakerIp);
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
                selectedSpeakerIp = device.getIp();
                selectedSpeakerName = device.getName();
                selectedSpeakerRoom = device.getRoom();

                etSpeakerName.setText(selectedSpeakerName);
                etSpeakerRoom.setText(selectedSpeakerRoom);
                etSpeakerIp.setText(selectedSpeakerIp);

                tvSetupSelectedSpeakerBadge.setText("✓ Gekoppeld aan: " + selectedSpeakerName + " (" + selectedSpeakerIp + ")");
                tvSetupSelectedSpeakerBadge.setVisibility(View.VISIBLE);
                Toast.makeText(SetupActivity.this, "✓ " + selectedSpeakerName + " gekoppeld", Toast.LENGTH_SHORT).show();

                displayDiscoveredDevices(discoveryManager.getDiscoveredDevices());
            });

            llSetupDiscoveredSpeakers.addView(itemView);
        }
    }

    private void startSpeakerDiscovery() {
        if (discoveryManager == null) return;

        pbSetupScanning.setVisibility(View.VISIBLE);
        tvSetupScanStatus.setText("Zoeken naar Google Home & Nest speakers...");
        tvSetupNoSpeakers.setVisibility(View.GONE);

        discoveryManager.startDiscovery(new CastDiscoveryManager.DiscoveryCallback() {
            @Override
            public void onDeviceFound(CastDevice device) {
                runOnUiThread(() -> {
                    List<CastDevice> current = discoveryManager.getDiscoveredDevices();
                    displayDiscoveredDevices(current);
                    tvSetupScanStatus.setText("Gevonden: " + current.size() + " apparaat(en) op netwerk");
                });
            }

            @Override
            public void onDiscoveryComplete(List<CastDevice> devices) {
                runOnUiThread(() -> {
                    pbSetupScanning.setVisibility(View.GONE);
                    displayDiscoveredDevices(devices);
                    if (devices.isEmpty()) {
                        tvSetupScanStatus.setText("Klaar met zoeken • Geen speakers gevonden");
                        tvSetupNoSpeakers.setVisibility(View.VISIBLE);
                    } else {
                        tvSetupScanStatus.setText("✓ " + devices.size() + " Google Home / Cast apparaat(en) gevonden");
                        tvSetupNoSpeakers.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void setupStep4Adblock() {
        adblockEnabled = prefs.getBoolean("key_adblock_enabled", true);
        updateAdblockSelectionUi();

        cardAdblockEnable.setOnClickListener(v -> {
            adblockEnabled = true;
            updateAdblockSelectionUi();
        });

        cardAdblockDisable.setOnClickListener(v -> {
            adblockEnabled = false;
            updateAdblockSelectionUi();
        });
    }

    private void updateAdblockSelectionUi() {
        cardAdblockEnable.setAlpha(adblockEnabled ? 1.0f : 0.6f);
        cardAdblockDisable.setAlpha(!adblockEnabled ? 1.0f : 0.6f);
    }

    private void showStep(int step) {
        // Stop discovery if leaving step 3
        if (currentStep == 3 && step != 3 && discoveryManager != null) {
            discoveryManager.stopDiscovery();
        }

        currentStep = step;
        tvStepIndicator.setText("Stap " + currentStep + " van 5");

        step1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        step4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);
        step5.setVisibility(step == 5 ? View.VISIBLE : View.GONE);

        // Show/hide bottom nav bar
        llNavBar.setVisibility(step > 1 && step < 5 ? View.VISIBLE : View.GONE);

        if (step == 1) {
            btnStart.requestFocus();
        } else if (step == 2) {
            rvProviders.post(() -> {
                if (rvProviders.getChildCount() > 0) {
                    rvProviders.getChildAt(0).requestFocus();
                }
            });
        } else if (step == 3) {
            startSpeakerDiscovery();
            btnSetupRescan.requestFocus();
        } else if (step == 4) {
            cardAdblockEnable.requestFocus();
        } else if (step == 5) {
            buildSummary();
            btnFinish.requestFocus();
        }
    }

    private void buildSummary() {
        TvProvider provider = null;
        for (TvProvider p : TvProvider.getStandardProviders(this)) {
            if (p.getId().equalsIgnoreCase(selectedProviderId)) {
                provider = p;
                break;
            }
        }
        String pName = provider != null ? provider.getName() : "BBH TV";
        tvSummaryProvider.setText("📺 TV Streaming Dienst: " + pName);

        String sIp = !selectedSpeakerIp.isEmpty() ? selectedSpeakerIp : etSpeakerIp.getText().toString().trim();
        String sName = !selectedSpeakerName.isEmpty() ? selectedSpeakerName : etSpeakerName.getText().toString().trim();
        String sRoom = !selectedSpeakerRoom.isEmpty() ? selectedSpeakerRoom : etSpeakerRoom.getText().toString().trim();

        if (!sIp.isEmpty()) {
            tvSummarySpeaker.setText("🔊 Slimme Speaker: " + (sName.isEmpty() ? "Google Cast Speaker" : sName)
                    + " (" + (sRoom.isEmpty() ? "Woonkamer" : sRoom) + " • " + sIp + ")");
            tvSummarySpeaker.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvSummarySpeaker.setText("🔊 Slimme Speaker: Niet gekoppeld (optioneel via Instellingen)");
            tvSummarySpeaker.setTextColor(Color.parseColor("#94A3B8"));
        }

        if (adblockEnabled) {
            tvSummaryAdblock.setText("🛡️ Reclameblokkering: Actief (AdGuard Private DNS)");
            tvSummaryAdblock.setTextColor(Color.parseColor("#34D399"));
        } else {
            tvSummaryAdblock.setText("Reclameblokkering: Uitgeschakeld");
            tvSummaryAdblock.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void saveAllAndFinish() {
        // Save Provider
        TvProvider.setDefaultProvider(this, selectedProviderId);

        // Save Speaker info only if configured or selected
        String sIp = !selectedSpeakerIp.isEmpty() ? selectedSpeakerIp : etSpeakerIp.getText().toString().trim();
        String sName = !selectedSpeakerName.isEmpty() ? selectedSpeakerName : etSpeakerName.getText().toString().trim();
        String sRoom = !selectedSpeakerRoom.isEmpty() ? selectedSpeakerRoom : etSpeakerRoom.getText().toString().trim();

        if (!sIp.isEmpty()) {
            prefs.edit().putString("key_speaker_ip", sIp).apply();
            prefs.edit().putString("key_speaker_name", sName.isEmpty() ? "Smart Speaker" : sName).apply();
            prefs.edit().putString("key_speaker_room", sRoom.isEmpty() ? "Woonkamer" : sRoom).apply();
        }

        // Save AdBlocker
        prefs.edit().putBoolean("key_adblock_enabled", adblockEnabled).apply();
        AdBlocker.setEnabled(adblockEnabled);

        new Thread(() -> {
            try {
                String mode = adblockEnabled ? "hostname" : "off";
                String dns = adblockEnabled ? "dns.adguard-dns.com" : "";
                Settings.Global.putString(getContentResolver(), "private_dns_mode", mode);
                Settings.Global.putString(getContentResolver(), "private_dns_specifier", dns);
            } catch (Exception e) {
                try {
                    String mode = adblockEnabled ? "hostname" : "off";
                    String dns = adblockEnabled ? "dns.adguard-dns.com" : "";
                    Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "private_dns_mode", mode});
                    Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "private_dns_specifier", dns});
                } catch (Exception ignored) {}
            }
        }).start();

        // Mark setup complete
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply();

        Toast.makeText(this, "🎉 Welkom bij AuraOS!", Toast.LENGTH_LONG).show();

        // Launch / restart MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
            if (currentStep > 1) {
                showStep(currentStep - 1);
                return true;
            }
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
