package com.a2t.myapplication.common.utilities

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date

class AlarmHelper {
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
    }
}