package com.example.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class DeadlineInfo(
    val text: String,
    val daysLeft: Long,
    val isOverdue: Boolean,
    val isUrgent: Boolean, // <= 3 days
    val isToday: Boolean
)

object DateUtils {

    private val russianLocale = Locale("ru", "RU")

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("d MMMM yyyy г.", russianLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatShortDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", russianLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatDateTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", russianLocale)
        return sdf.format(Date(timeMillis))
    }

    fun calculateDeadlineInfo(dueDateMillis: Long?): DeadlineInfo? {
        if (dueDateMillis == null || dueDateMillis == 0L) return null

        val nowCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dueCalendar = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = dueCalendar.timeInMillis - nowCalendar.timeInMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffDays < 0 -> {
                val absDays = kotlin.math.abs(diffDays)
                DeadlineInfo(
                    text = "Просрочено на $absDays ${pluralizeDays(absDays)}",
                    daysLeft = diffDays,
                    isOverdue = true,
                    isUrgent = true,
                    isToday = false
                )
            }
            diffDays == 0L -> {
                DeadlineInfo(
                    text = "Срок сегодня",
                    daysLeft = 0,
                    isOverdue = false,
                    isUrgent = true,
                    isToday = true
                )
            }
            else -> {
                DeadlineInfo(
                    text = "Осталось $diffDays ${pluralizeDays(diffDays)}",
                    daysLeft = diffDays,
                    isOverdue = false,
                    isUrgent = diffDays <= 3,
                    isToday = false
                )
            }
        }
    }

    private fun pluralizeDays(count: Long): String {
        val n = kotlin.math.abs(count) % 100
        val n1 = n % 10
        if (n in 11..19) return "дней"
        if (n1 == 1L) return "день"
        if (n1 in 2..4) return "дня"
        return "дней"
    }
}
