package com.notifmanager.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun deliveryModeToString(value: DeliveryMode): String = value.name

    @TypeConverter
    fun deliveryModeFromString(value: String): DeliveryMode = DeliveryMode.valueOf(value)

    @TypeConverter
    fun ruleSourceToString(value: RuleSource): String = value.name

    @TypeConverter
    fun ruleSourceFromString(value: String): RuleSource = RuleSource.valueOf(value)
}
