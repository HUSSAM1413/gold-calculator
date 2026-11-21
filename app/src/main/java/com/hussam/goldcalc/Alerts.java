package com.hussam.goldcalc;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

class Alerts {
    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("gold", Context.MODE_PRIVATE); }
    static String load(Context c) { return sp(c).getString("alerts", "[]"); }
    static void save(Context c, String json) { sp(c).edit().putString("alerts", json).apply(); }
    static String pref(Context c, String k) { return sp(c).getString("p_" + k, ""); }
    static void setPref(Context c, String k, String v) { sp(c).edit().putString("p_" + k, v).apply(); }
    static String lang(Context c) { return "en".equals(pref(c, "lang")) ? "en" : "ar"; }

    static boolean hasActive(String json) {
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) if (!a.getJSONObject(i).optBoolean("done")) return true;
        } catch (Exception e) { }
        return false;
    }

    static synchronized void addHistory(Context c, String title, String body) {
        try {
            JSONArray old = new JSONArray(sp(c).getString("hist", "[]"));
            JSONArray n = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("ts", System.currentTimeMillis()); o.put("title", title);
            o.put("body", body); o.put("read", false);
            n.put(o);
            for (int i = 0; i < old.length() && n.length() < 50; i++) n.put(old.get(i));
            sp(c).edit().putString("hist", n.toString()).apply();
        } catch (Exception e) { }
    }
    static String history(Context c) { return sp(c).getString("hist", "[]"); }

    static int unread(Context c) {
        int n = 0;
        try {
            JSONArray a = new JSONArray(history(c));
            for (int i = 0; i < a.length(); i++) if (!a.getJSONObject(i).optBoolean("read")) n++;
        } catch (Exception e) { }
        return n;
    }

    static synchronized void markAllRead(Context c) {
        try {
            JSONArray a = new JSONArray(history(c));
            for (int i = 0; i < a.length(); i++) a.getJSONObject(i).put("read", true);
            sp(c).edit().putString("hist", a.toString()).apply();
        } catch (Exception e) { }
    }
    static void clearHistory(Context c) { sp(c).edit().remove("hist").apply(); }
}
