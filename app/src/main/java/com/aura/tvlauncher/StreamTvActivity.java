package com.aura.tvlauncher;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StreamTvActivity extends AppCompatActivity {

    public static final String EXTRA_FORCE_HUB = "extra_force_hub";
    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";

    private FrameLayout flPlayerContainer;
    private NestedScrollView scrollProviderHub;
    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout customViewContainer;
    private LinearLayout llStreamHud;

    private TextView tvHudProviderName;
    private Button btnHudSwitchProvider;
    private Button btnHudReload;
    private Button btnHudSetDefault;
    private Button btnHudClose;

    private Button btnToggleDirectStart;
    private Button btnHubExit;
    private Button btnCustomStreamUrl;
    private RecyclerView rvProviders;
    private TvProviderAdapter providerAdapter;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private TvProvider currentProvider;
    private SharedPreferences prefs;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during TV watching
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_stream_tv);
        prefs = getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE);

        hideSystemUI();
        initViews();
        setupProviderGrid();

        boolean forceHub = getIntent().getBooleanExtra(EXTRA_FORCE_HUB, false);
        String specificProviderId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);

        if (specificProviderId != null && !specificProviderId.isEmpty()) {
            TvProvider target = findProviderById(specificProviderId);
            if (target != null) {
                launchProvider(target);
                return;
            }
        }

        boolean directStart = TvProvider.isDirectStartEnabled(this);
        if (directStart && !forceHub) {
            TvProvider defaultProvider = TvProvider.getDefaultProvider(this);
            launchProvider(defaultProvider);
        } else {
            showProviderHub();
        }
    }

    private void initViews() {
        flPlayerContainer = findViewById(R.id.fl_player_container);
        scrollProviderHub = findViewById(R.id.scroll_provider_hub);
        webView = findViewById(R.id.stream_web_view);
        progressBar = findViewById(R.id.stream_progress_bar);
        customViewContainer = findViewById(R.id.stream_custom_view_container);
        llStreamHud = findViewById(R.id.ll_stream_hud);

        tvHudProviderName = findViewById(R.id.tv_hud_provider_name);
        btnHudSwitchProvider = findViewById(R.id.btn_hud_switch_provider);
        btnHudReload = findViewById(R.id.btn_hud_reload);
        btnHudSetDefault = findViewById(R.id.btn_hud_set_default);
        btnHudClose = findViewById(R.id.btn_hud_close);

        btnToggleDirectStart = findViewById(R.id.btn_toggle_direct_start);
        btnHubExit = findViewById(R.id.btn_hub_exit);
        btnCustomStreamUrl = findViewById(R.id.btn_custom_stream_url);
        rvProviders = findViewById(R.id.rv_providers);

        // HUD Listeners
        btnHudSwitchProvider.setOnClickListener(v -> {
            llStreamHud.setVisibility(View.GONE);
            showProviderHub();
        });

        btnHudReload.setOnClickListener(v -> {
            if (webView != null) webView.reload();
            llStreamHud.setVisibility(View.GONE);
        });

        btnHudSetDefault.setOnClickListener(v -> {
            if (currentProvider != null) {
                TvProvider.setDefaultProvider(this, currentProvider.getId());
                if (providerAdapter != null) {
                    providerAdapter.setDefaultProviderId(currentProvider.getId());
                }
                Toast.makeText(this, "★ " + currentProvider.getName() + " ingesteld als standaard TV provider", Toast.LENGTH_SHORT).show();
            }
            llStreamHud.setVisibility(View.GONE);
        });

        btnHudClose.setOnClickListener(v -> finish());

        // Hub Listeners
        updateDirectStartButton();
        btnToggleDirectStart.setOnClickListener(v -> {
            boolean current = TvProvider.isDirectStartEnabled(this);
            TvProvider.setDirectStartEnabled(this, !current);
            updateDirectStartButton();
            Toast.makeText(this, !current ? "⚡ Direct starten ingeschakeld" : "Direct starten uitgeschakeld", Toast.LENGTH_SHORT).show();
        });

        btnHubExit.setOnClickListener(v -> finish());

        btnCustomStreamUrl.setOnClickListener(v -> showCustomUrlDialog());

        setupWebView();
    }

    private void updateDirectStartButton() {
        boolean direct = TvProvider.isDirectStartEnabled(this);
        if (direct) {
            btnToggleDirectStart.setText("⚡ Direct Starten: AAN");
            btnToggleDirectStart.setTextColor(getColor(R.color.accent_blue));
        } else {
            btnToggleDirectStart.setText("Direct Starten: UIT");
            btnToggleDirectStart.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void setupProviderGrid() {
        List<TvProvider> list = TvProvider.getStandardProviders(this);
        String defaultId = TvProvider.getDefaultProvider(this).getId();

        providerAdapter = new TvProviderAdapter(this, list, defaultId, new TvProviderAdapter.OnProviderClickListener() {
            @Override
            public void onProviderSelected(TvProvider provider) {
                launchProvider(provider);
            }

            @Override
            public void onProviderDefaultChanged(TvProvider provider) {
                // Handled in adapter
            }
        });

        rvProviders.setLayoutManager(new GridLayoutManager(this, 2));
        rvProviders.setAdapter(providerAdapter);
    }

    private TvProvider findProviderById(String id) {
        for (TvProvider p : TvProvider.getStandardProviders(this)) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    private void showProviderHub() {
        flPlayerContainer.setVisibility(View.GONE);
        scrollProviderHub.setVisibility(View.VISIBLE);
        if (webView != null) {
            webView.onPause();
        }
        rvProviders.post(() -> {
            if (rvProviders.getChildCount() > 0) {
                rvProviders.getChildAt(0).requestFocus();
            }
        });
    }

    private void launchProvider(TvProvider provider) {
        currentProvider = provider;
        tvHudProviderName.setText(provider.getName());
        scrollProviderHub.setVisibility(View.GONE);
        flPlayerContainer.setVisibility(View.VISIBLE);
        llStreamHud.setVisibility(View.GONE);

        if (webView != null) {
            webView.onResume();
            webView.loadUrl(provider.getUrl());
            webView.requestFocus();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setNeedInitialFocus(true);

        // Full desktop / smart tv user agent for clean layout & HD streaming
        settings.setUserAgentString("Mozilla/5.0 (X11; Linux armv7l) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 SmartTV");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (AdBlocker.isAd(url)) {
                        return AdBlocker.createEmptyResponse();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                AdBlocker.injectCosmeticAdBlock(view);

                // Auto-focus username if on login page and high visibility focus ring
                view.evaluateJavascript(
                        "(function() {" +
                                "   var u = document.getElementById('username') || document.querySelector('input[type=email]') || document.querySelector('input[type=text]');" +
                                "   if (u && !document.activeElement) { u.focus(); }" +
                                "   var s = document.createElement('style');" +
                                "   s.innerHTML = ':focus { outline: 3px solid #38BDF8 !important; outline-offset: 2px !important; }';" +
                                "   document.head.appendChild(s);" +
                                "})();", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                customViewContainer.setVisibility(View.VISIBLE);
                customViewContainer.addView(view);
                hideSystemUI();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                customViewContainer.removeView(customView);
                customView = null;
                customViewContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
                hideSystemUI();
            }
        });
    }

    private void showCustomUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Eigen Stream URL Invoeren");

        final EditText input = new EditText(this);
        input.setHint("https://voorbeeld.nl/live");
        String currentCustom = prefs.getString(TvProvider.PREF_CUSTOM_URL, "");
        if (!currentCustom.isEmpty()) {
            input.setText(currentCustom);
        }
        builder.setView(input);

        builder.setPositiveButton("Opslaan & Kijken", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                prefs.edit().putString(TvProvider.PREF_CUSTOM_URL, url).apply();
                TvProvider custom = new TvProvider("custom", "Aangepaste Stream", url, "Eigen URL: " + url, "#38BDF8", "🌐", "CUSTOM");
                launchProvider(custom);
            }
        });
        builder.setNegativeButton("Annuleren", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (flPlayerContainer.getVisibility() == View.VISIBLE) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MENU:
                    toggleHud();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (customView != null) {
                        if (webView.getWebChromeClient() != null) {
                            webView.getWebChromeClient().onHideCustomView();
                        }
                        return true;
                    }
                    if (llStreamHud.getVisibility() == View.VISIBLE) {
                        llStreamHud.setVisibility(View.GONE);
                        return true;
                    }
                    toggleHud();
                    return true;
            }
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void toggleHud() {
        if (llStreamHud.getVisibility() == View.VISIBLE) {
            llStreamHud.setVisibility(View.GONE);
        } else {
            llStreamHud.setVisibility(View.VISIBLE);
            llStreamHud.bringToFront();
            btnHudSwitchProvider.requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
