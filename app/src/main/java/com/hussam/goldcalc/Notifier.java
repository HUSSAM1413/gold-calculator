package com.hussam.goldcalc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

class Notifier {
    static void show(Context c, String title, String body) {
        Alerts.addHistory(c, title, body);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("gold", "Gold alerts", NotificationManager.IMPORTANCE_HIGH));
            b = new Notification.Builder(c, "gold");
        } else {
            b = new Notification.Builder(c).setPriority(Notification.PRIORITY_HIGH);
        }
        Intent i = new Intent(c, MainActivity.class);
        i.putExtra("page", "center");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body)
         .setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(pi).setAutoCancel(true);
        nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
    }
}
