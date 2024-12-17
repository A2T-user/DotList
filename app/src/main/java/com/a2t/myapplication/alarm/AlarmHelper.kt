package com.a2t.myapplication.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.a2t.myapplication.R
import com.a2t.myapplication.alarm.receivers.AlarmReceiver
import com.a2t.myapplication.root.ui.RootActivity
import java.util.Calendar
import java.util.Date

class AlarmHelper (private val context: Context) {

    private val channelId = "dot_list_channel_id"
    private val channelName = "DotList"

    init {
        createNotificationChannel()
        Log.e("МОЁ", "AlarmHelper")
    }


    companion object {
        fun startOfCurrentDay(): Long {
            return midnight(0)
        }

        fun endOfCurrentDay(): Long {
            return midnight(24)
        }

        fun midnight(hour: Int): Long {
            val currentDate = Date()
            val calendar = Calendar.getInstance()
            calendar.setTime(currentDate)
            val startOfDayInMillis = calendar.timeInMillis - calendar.timeZone.rawOffset
            calendar.timeInMillis = startOfDayInMillis
            calendar[Calendar.HOUR_OF_DAY] = hour
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0

            return calendar.timeInMillis
        }

        // Конвертация времени в милисекундах в дату или время, если дата - сегодня
        fun convertDateTime(timeInMilliseconds: Long): String {
            if (timeInMilliseconds == 0L) {
                return ""
            } else {
                val date = DateFormat.format("dd.M.yy", timeInMilliseconds).toString()

                return if (date == DateFormat.format("dd.M.yy", System.currentTimeMillis())
                        .toString()
                ) {
                    DateFormat.format("HH:mm", timeInMilliseconds).toString()
                } else {
                    date
                }
            }
        }

        // Получение PendingIntent для alarmManager
        fun getAlarmPendingIntent(сontext: Context, idDir: Long, alarmText: String): PendingIntent {
            val intent = Intent(сontext, AlarmReceiver::class.java).apply {
                putExtra("IDDIR", idDir)
                putExtra("ALARM_TEXT", alarmText)
            }
            return PendingIntent.getBroadcast(сontext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun createNotificationChannel() {
        val uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.bell)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(uri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNotification(idDir: Long, alarmText: String, ) {
        val intent = Intent(context, RootActivity::class.java)                  // Замените на вашу активность
        intent.putExtra("IDDIR", idDir)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifi)                                 // Иконка в строке состояния
            .setContentTitle(context.getString(R.string.notifi_title))          // Заголовок
            .setContentText(alarmText)                                          // Сообщение
            .setAutoCancel(true)                                                // Откл-е после касания
            .setColor(Color.RED)
            .setColorized(true)
            .setContentIntent(pendingIntent)                                    // Интент
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        Log.e("МОЁ", "AlarmHelper notification")
        notificationManager.notify(1, notification)
    }

}