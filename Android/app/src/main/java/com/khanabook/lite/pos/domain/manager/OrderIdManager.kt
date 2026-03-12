package com.khanabook.lite.pos.domain.manager

import com.khanabook.lite.pos.data.local.entity.RestaurantProfileEntity
import java.text.SimpleDateFormat
import java.util.*

object OrderIdManager {
    
    fun getDailyOrderDisplay(date: String, counter: Int): String {
        return "$date-${String.format("%03d", counter)}"
    }

    fun isResetNeeded(profile: RestaurantProfileEntity, today: String): Boolean {
        return profile.lastResetDate != today
    }

    fun getNextDailyCounter(profile: RestaurantProfileEntity, today: String): Int {
        return if (profile.lastResetDate != today) 1 else profile.dailyOrderCounter + 1
    }

    fun getNextLifetimeId(profile: RestaurantProfileEntity): Int {
        return profile.lifetimeOrderCounter + 1
    }

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}


