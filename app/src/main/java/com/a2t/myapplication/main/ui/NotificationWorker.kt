package com.a2t.myapplication.main.ui

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.a2t.myapplication.App
import com.a2t.myapplication.utilities.AlarmHelper

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (App.appSettings.notificationOn) {
            // Получить данные из входных данных
            val idDir = inputData.getLong("IDDIR", 0)
            val alarmText = inputData.getString("ALARM_TEXT") ?: ""

            // Отправка уведомления
            val alarmHelper = AlarmHelper(applicationContext)
            alarmHelper.sendNotification(idDir, alarmText)
        }
        return Result.success()
    }
}