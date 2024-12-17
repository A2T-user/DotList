package com.a2t.myapplication.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.a2t.myapplication.alarm.AlarmHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.e("МОЁ", "AlarmReceiver")
        // Извлечение данных из интента
        val idDir = intent.getLongExtra("IDDIR", 0)
        val alarmText = intent.getStringExtra("ALARM_TEXT" )

        val alarmHelper = AlarmHelper(context)
        alarmHelper.sendNotification(idDir, alarmText!!)
    }
}