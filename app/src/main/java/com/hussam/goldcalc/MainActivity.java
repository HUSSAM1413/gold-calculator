package com.hussam.goldcalc;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private static final String PERM = "android.permission.POST_NOTIFICATIONS";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "GoldBridge");
        web.setWebViewClient(new WebViewClient());
        String page = getIntent() != null ? getIntent().getStringExtra("page") : null;
        web.loadUrl("center".equals(page)
            ? "file:///android_asset/alerts.html#center"
            : "file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private void askPermission() {
        runOnUiThread(new Runnable() { public void run() {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{PERM}, 1);
        }});
    }

    class Bridge {
        @JavascriptInterface public String getAlerts() { return Alerts.load(MainActivity.this); }
        @JavascriptInterface public void setAlerts(String json, String lang) {
            Alerts.save(MainActivity.this, json);
            if (lang != null && lang.length() > 0) Alerts.setPref(MainActivity.this, "lang", lang);
            if (Alerts.hasActive(json)) { PriceJob.schedule(MainActivity.this); askPermission(); }
            else PriceJob.cancel(MainActivity.this);
        }
        @JavascriptInterface public String getPref(String k) { return Alerts.pref(MainActivity.this, k); }
        @JavascriptInterface public void setPref(String k, String v) { Alerts.setPref(MainActivity.this, k, v); }
        @JavascriptInterface public String getHistory() { return Alerts.history(MainActivity.this); }
        @JavascriptInterface public int getUnread() { return Alerts.unread(MainActivity.this); }
        @JavascriptInterface public void markAllRead() { Alerts.markAllRead(MainActivity.this); }
        @JavascriptInterface public void clearHistory() { Alerts.clearHistory(MainActivity.this); }
        @JavascriptInterface public void askNotify() { askPermission(); }
        @JavascriptInterface public void notifyNow(String title, String body) { Notifier.show(MainActivity.this, title, body); }
    }
}
