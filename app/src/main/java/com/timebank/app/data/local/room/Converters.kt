package com.timebank.app.data.local.room

import androidx.room.TypeConverter
import com.timebank.app.domain.model.AppCategory

/**
 * Room类型转换器
 */
class Converters {

    @TypeConverter
    fun fromAppCategory(category: AppCategory): String {
        return category.name
    }

    @TypeConverter
    fun toAppCategory(value: String): AppCategory {
        return try {
            AppCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AppCategory.NONE
        }
    }
}
