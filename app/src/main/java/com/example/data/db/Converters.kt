package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.DebtType

class Converters {
    @TypeConverter
    fun fromDebtType(type: DebtType): String {
        return type.name
    }

    @TypeConverter
    fun toDebtType(value: String): DebtType {
        return try {
            DebtType.valueOf(value)
        } catch (e: Exception) {
            DebtType.OWED_TO_ME
        }
    }
}
