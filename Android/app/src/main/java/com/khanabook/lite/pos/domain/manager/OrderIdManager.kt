package com.khanabook.lite.pos.domain.manager

import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import java.text.SimpleDateFormat
import java.util.*

object OrderIdManager {
    
    fun getDailyOrderDisplay(date: String, counter: Long): String {
        return "$date-${String.format("%03d", counter)}"
    }

    fun isResetNeeded(profile: RestaurantProfileEntity, today: String): Boolean {
        return profile.lastResetDate != today
    }

    fun getNextDailyCounter(profile: RestaurantProfileEntity, today: String): Long {
        return if (profile.lastResetDate != today) 1L else profile.dailyOrderCounter + 1L
    }

    fun getNextLifetimeId(profile: RestaurantProfileEntity): Long {
        return profile.lifetimeOrderCounter + 1L
    }

    fun getTodayString(timezone: String = "Asia/Kolkata"): String {
        return java.time.LocalDate.now(java.time.ZoneId.of(timezone)).toString()
    }
}


