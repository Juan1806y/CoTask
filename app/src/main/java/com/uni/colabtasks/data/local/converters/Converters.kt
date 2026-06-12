package com.uni.colabtasks.data.local.converters

import androidx.room.TypeConverter
import com.uni.colabtasks.domain.model.Subtask
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    // Listas de strings (emails de contribuyentes, etc.) — codificadas como JSON array.
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val arr = JSONArray()
        value?.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    // Subtareas — codificadas como JSON array de objetos {id, title, done}.
    @TypeConverter
    fun fromSubtaskList(value: List<Subtask>?): String {
        val arr = JSONArray()
        value?.forEach { st ->
            arr.put(
                JSONObject()
                    .put("id", st.id)
                    .put("title", st.title)
                    .put("done", st.isDone)
            )
        }
        return arr.toString()
    }

    @TypeConverter
    fun toSubtaskList(value: String?): List<Subtask> {
        if (value.isNullOrEmpty()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Subtask(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    isDone = obj.optBoolean("done", false)
                )
            }
        }.getOrDefault(emptyList())
    }
}
