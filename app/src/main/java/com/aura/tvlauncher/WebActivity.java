package com.aura.tvlauncher;

import android.annotation.SuppressLint;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WebActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout customViewContainer;
    private LinearLayout llHud;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on while watching web apps/videos
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_web);

        hideSystemUI();

        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.web_progress_bar);
        customViewContainer = findViewById(R.id.custom_view_container);
        llHud = findViewById(R.id.ll_hud);
        TextView tvHudTitle = findViewById(R.id.tv_hud_title);
        Button btnHudReload = findViewById(R.id.btn_hud_reload);
        Button btnHudClose = findViewById(R.id.btn_hud_close);

        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (title != null) {
            tvHudTitle.setText(title);
        }

        btnHudReload.setOnClickListener(v -> {
            webView.reload();
            llHud.setVisibility(View.GONE);
        });

        btnHudClose.setOnClickListener(v -> finish());

        // Configure modern WebView for TV
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

        // Block unwanted popups and redirects
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // Modern TV desktop user agent so websites format nicely on large screens
        String defaultUA = settings.getUserAgentString();
        settings.setUserAgentString("Mozilla/5.0 (X11; Linux armv7l) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 SmartTV");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // Load inside this WebView
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

                // Auto-focus username on login pages and add high-visibility focus ring for TV remote navigation
                view.evaluateJavascript(
                    "(function() {" +
                    "   var u = document.getElementById('username');" +
                    "   if (u) { u.focus(); }" +
                    "   var style = document.createElement('style');" +
                    "   style.innerHTML = ':focus { outline: 3px solid #F59E0B !important; outline-offset: 2px !important; }';" +
                    "   document.head.appendChild(style);" +
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
                // Support HTML5 fullscreen video playback
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

        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        } else {
            webView.loadUrl("https://www.google.com");
        }

        webView.requestFocus();
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                // Toggle HUD on Menu key
                if (llHud.getVisibility() == View.VISIBLE) {
                    llHud.setVisibility(View.GONE);
                } else {
                    llHud.setVisibility(View.VISIBLE);
                    llHud.bringToFront();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (customView != null) {
                    // Exit fullscreen video
                    if (webView.getWebChromeClient() != null) {
                        webView.getWebChromeClient().onHideCustomView();
                    }
                    return true;
                }
                if (llHud.getVisibility() == View.VISIBLE) {
                    llHud.setVisibility(View.GONE);
                    return true;
                }
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
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
