package com.a2t.myapplication.main.ui.utilities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.a2t.myapplication.R
import com.a2t.myapplication.main.ui.activity.MainActivity
import java.time.ZonedDateTime

class AlarmHelper (private val context: Context) {

    private val channelId = "dot_list_bell_channel_id"
    private val channelName = "DotList"

    init {
        createNotificationChannel()
    }

    companion object {

        fun startOfCurrentDay(): Long {
            // Получаем текущее время с учетом часового пояса
            val now = ZonedDateTime.now()
            // Устанавливаем время на 00:00:00
            val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
            // Преобразуем в миллисекунды с начала эпохи (1970-01-01T00:00:00Z)
            return startOfDay.toInstant().toEpochMilli()
        }

        fun endOfCurrentDay(): Long {
            return startOfCurrentDay() + 24 * 60 * 60 * 1000
        }

        // Конвертация времени в милисекундах в дату или время, если дата - сегодня
        fun convertDateTime(timeInMilliseconds: Long): String {
            if (timeInMilliseconds == 0L) return ""

            val date = DateFormat.format("dd.M.yy", timeInMilliseconds).toString()
            val currentDate = DateFormat.format("dd.M.yy", System.currentTimeMillis()).toString()

            return if (date == currentDate) {
                DateFormat.format("HH:mm", timeInMilliseconds).toString()
            } else {
                date
            }
        }
    }

    private fun createNotificationChannel() {
        val uri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.bell)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(uri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNotification(idDir: Long, alarmText: String) {
        val intent = Intent(context, MainActivity::class.java)                  // Замените на вашу активность
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
        notificationManager.notify(1, notification)
    }

}