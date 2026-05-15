package com.uni.colabtasks.data.local.converters

import androidx.room.TypeConverter

class Converters {
    private val sep = "" // separador improbable en emails

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString(separator = sep).orEmpty()

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(sep)
}
