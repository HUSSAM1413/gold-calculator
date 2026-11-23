package com.hussam.goldcalc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class WatchService extends Service {
    private final Handler h = new Handler(Looper.getMainLooper());
    private volatile boolean busy = false;

    private final Runnable tick = new Runnable() {
        public void run() {
            if (!Alerts.hasActive(Alerts.load(WatchService.this))) { stopSelf(); return; }
            if (PriceJob.marketOpen() && !busy) {
                busy = true;
                new Thread(new Runnable() { public void run() {
                    try { PriceJob.check(WatchService.this); } catch (Exception e) { }
                    busy = false;
                }}).start();
            }
            h.postDelayed(this, 30000);
        }
    };

    static void start(Context c) {
        Intent i = new Intent(c, WatchService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, WatchService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("watch", "Price monitoring", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, "watch");
        } else {
            b = new Notification.Builder(this);
        }
        boolean ar = "ar".equals(Alerts.lang(this));
        PendingIntent pi = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle(ar ? "مراقبة سعر الذهب" : "Gold price monitoring")
         .setContentText(ar ? "يتم فحص السعر كل 30 ثانية لتنبيهاتك" : "Checking the price every 30 seconds for your alerts")
         .setContentIntent(pi)
         .setOngoing(true);
        startForeground(1, b.build());
        h.removeCallbacks(tick);
        h.post(tick);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        h.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
