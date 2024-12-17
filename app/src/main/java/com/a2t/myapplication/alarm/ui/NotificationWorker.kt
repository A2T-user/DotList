package com.a2t.myapplication.alarm.ui

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.a2t.myapplication.alarm.AlarmHelper

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.e("МОЁ", "NotificationWorker")
        // Получить данные из входных данных
        val idDir = inputData.getLong("IDDIR", 0)
        val alarmText = inputData.getString("ALARM_TEXT") ?: "Текст уведомления"

        // Отправка уведомления
        val alarmHelper = AlarmHelper(applicationContext)
        alarmHelper.sendNotification(idDir, alarmText)

        return Result.success()
    }
}