package com.ornek.okulzili

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

class ZilWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        tumWidgetlariGuncelle(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ALARM_GUNCELLE ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == Intent.ACTION_DATE_CHANGED
        ) {
            tumWidgetlariGuncelle(context)
        }
    }

    companion object {
        const val ACTION_ALARM_GUNCELLE = "com.ornek.okulzili.ACTION_ALARM_GUNCELLE"

        fun tumWidgetlariGuncelle(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ZilWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val durum = ProgramYoneticisi.suAnkiDurumuHesapla(context)

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_zil)
                views.setTextViewText(R.id.tvWidgetBaslik, durum.baslik)
                views.setTextViewText(R.id.tvWidgetAltBilgi, durum.altBilgi)

                if (durum.hedefZamanMillis != null) {
                    val kalanMillis = (durum.hedefZamanMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                    val tabanZaman = SystemClock.elapsedRealtime() + kalanMillis

                    views.setViewVisibility(R.id.kronometreWidget, View.VISIBLE)
                    views.setChronometer(R.id.kronometreWidget, tabanZaman, null, true)
                    views.setChronometerCountDown(R.id.kronometreWidget, true)
                } else {
                    views.setViewVisibility(R.id.kronometreWidget, View.GONE)
                }

                val ayarlarIntent = Intent(context, MainActivity::class.java)
                val pendingAyarlar = PendingIntent.getActivity(
                    context, 0, ayarlarIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetKokLayout, pendingAyarlar)

                manager.updateAppWidget(id, views)
            }

            durum.sonrakiAlarmMillis?.let { sonrakiAlarmiKur(context, it) }
        }

        private fun sonrakiAlarmiKur(context: Context, hedefMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ZilWidgetProvider::class.java).apply {
                action = ACTION_ALARM_GUNCELLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val tetiklemeZamani = hedefMillis + 500L

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tetiklemeZamani, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tetiklemeZamani, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tetiklemeZamani, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, tetiklemeZamani, pendingIntent)
            }
        }
    }
}
