package com.hussam.goldcalc;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class PriceJob extends JobService {
    static final int ID = 7;

    static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == ID) return;
        js.schedule(new JobInfo.Builder(ID, new ComponentName(c, PriceJob.class))
            .setPeriodic(15 * 60 * 1000L)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true).build());
    }

    static void cancel(Context c) {
        ((JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(ID);
    }

    static boolean marketOpen() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        int d = c.get(Calendar.DAY_OF_WEEK);
        int mins = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        if (d == Calendar.SATURDAY) return false;
        if (d == Calendar.FRIDAY) return mins < 17 * 60;
        if (d == Calendar.SUNDAY) return mins >= 18 * 60;
        return !(mins >= 17 * 60 && mins < 18 * 60);
    }

    @Override
    public boolean onStartJob(final JobParameters p) {
        new Thread(new Runnable() { public void run() {
            try { check(PriceJob.this); } catch (Exception e) { }
            jobFinished(p, false);
        }}).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters p) { return true; }

    static double fetchOz() throws Exception {
        HttpURLConnection u = (HttpURLConnection) new URL("https://xaus.com/api/v1/spot").openConnection();
        u.setConnectTimeout(15000);
        u.setReadTimeout(15000);
        BufferedReader r = new BufferedReader(new InputStreamReader(u.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) sb.append(l);
        r.close();
        JSONObject j = new JSONObject(sb.toString());
        double oz = j.optDouble("spot_usd_oz", Double.NaN);
        if (Double.isNaN(oz) && j.has("xau")) oz = j.getJSONObject("xau").optDouble("price", Double.NaN);
        if (Double.isNaN(oz) || oz <= 0) throw new Exception("bad");
        return oz;
    }

    static void check(Context c) throws Exception {
        String json = Alerts.load(c);
        if (!Alerts.hasActive(json)) { cancel(c); return; }
        if (!marketOpen()) return;
        double sar24 = fetchOz() / 31.1035 * 3.75;
        JSONArray a = new JSONArray(json);
        boolean ar = "ar".equals(Alerts.lang(c)), changed = false;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (o.optBoolean("done")) continue;
            String k = o.getString("k");
            boolean usd = "USD".equals(o.optString("cur"));
            double price = sar24 * Integer.parseInt(k) / 24.0;
            if (usd) price /= 3.75;
            double target = o.getDouble("t");
            boolean hit = "le".equals(o.optString("dir")) ? price <= target : price >= target;
            if (!hit) continue;
            o.put("done", true);
            o.put("hit", price);
            changed = true;
            String u = usd ? (ar ? "دولار" : "USD") : (ar ? "ريال" : "SAR");
            String pr = String.format(Locale.US, "%.2f", price);
            String tg = String.format(Locale.US, "%.2f", target);
            Notifier.show(c,
                ar ? "تنبيه سعر الذهب" : "Gold price alert",
                ar ? "عيار " + k + " وصل " + pr + " " + u + " للجرام (الهدف " + tg + ")"
                   : "Karat " + k + " reached " + pr + " " + u + " per gram (target " + tg + ")");
        }
        if (changed) {
            String out = a.toString();
            Alerts.save(c, out);
            if (!Alerts.hasActive(out)) cancel(c);
        }
    }
}
