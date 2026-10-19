package com.aura.tvlauncher;

import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {

    private static final Set<String> AD_DOMAINS = new HashSet<>(Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "google-analytics.com",
            "googleadservices.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "amazon-adsystem.com",
            "adnxs.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "smartadserver.com",
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "exoclick.com",
            "adsterra.com",
            "trafficjunky.com",
            "scorecardresearch.com",
            "moatads.com",
            "applovin.com",
            "inmobi.com",
            "vungle.com",
            "chartbeat.com",
            "quantserve.com",
            "branch.io",
            "adjust.com",
            "appsflyer.com"
    ));

    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isAd(String url) {
        if (!enabled || url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();

        // Check domain matches
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host != null) {
                host = host.toLowerCase();
                for (String adDomain : AD_DOMAINS) {
                    if (host.equals(adDomain) || host.endsWith("." + adDomain)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Check common ad path keywords
        return lowerUrl.contains("/adservice/")
                || lowerUrl.contains("/pagead/")
                || lowerUrl.contains("/doubleclick/")
                || lowerUrl.contains("adsbygoogle")
                || lowerUrl.contains("googlesyndication")
                || lowerUrl.contains("popcash")
                || lowerUrl.contains("popads");
    }

    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }

    public static void injectCosmeticAdBlock(WebView webView) {
        if (!enabled || webView == null) return;
        String js = "javascript:(function() {" +
                "var css = '" +
                ".ads, .ad-banner, .advertisement, [id*=\"google_ads\"], [class*=\"adsbygoogle\"], " +
                "[id*=\"ad-container\"], [class*=\"ad-container\"], [id*=\"taboola\"], [id*=\"outbrain\"], " +
                ".cookie-banner, #cookie-banner, .cookie-consent, [id*=\"cookieConsent\"], .fc-consent-root, " +
                "[aria-label*=\"cookie\" i], [aria-label*=\"consent\" i] " +
                "{ display: none !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; }';" +
                "var head = document.head || document.getElementsByTagName('head')[0];" +
                "if (head) {" +
                "   var style = document.createElement('style');" +
                "   style.type = 'text/css';" +
                "   style.appendChild(document.createTextNode(css));" +
                "   head.appendChild(style);" +
                "}" +
                "})();";
        webView.evaluateJavascript(js, null);
    }
}
