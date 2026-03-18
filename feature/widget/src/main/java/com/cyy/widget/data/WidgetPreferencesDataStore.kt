package com.cyy.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Widget 数据存储管理类
 * 使用 DataStore 持久化存储 Widget 数据（简化版，使用 String 存储）
 */
class WidgetPreferencesDataStore private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: WidgetPreferencesDataStore? = null

        fun getInstance(context: Context): WidgetPreferencesDataStore {
            return instance ?: synchronized(this) {
                instance ?: WidgetPreferencesDataStore(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private const val DATASTORE_NAME = "widget_data"
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

        fun dataKey(widgetId: Int) = stringPreferencesKey("widget_data_$widgetId")
        fun timestampKey(widgetId: Int) = longPreferencesKey("widget_timestamp_$widgetId")
        fun typeKey(widgetId: Int) = stringPreferencesKey("widget_type_$widgetId")
    }

    suspend fun saveData(widgetId: Int, data: String) {
        context.dataStore.edit { prefs ->
            prefs[dataKey(widgetId)] = data
            prefs[timestampKey(widgetId)] = System.currentTimeMillis()
        }
    }

    fun getData(widgetId: Int): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[dataKey(widgetId)]
        }
    }

    fun getDataWithTimestamp(widgetId: Int): Flow<Pair<String?, Long>> {
        return context.dataStore.data.map { prefs ->
            val data = prefs[dataKey(widgetId)]
            val timestamp = prefs[timestampKey(widgetId)] ?: 0L
            data to timestamp
        }
    }

    suspend fun deleteData(widgetId: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(dataKey(widgetId))
            prefs.remove(timestampKey(widgetId))
            prefs.remove(typeKey(widgetId))
        }
    }

    fun getTimestamp(widgetId: Int): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            prefs[timestampKey(widgetId)] ?: 0L
        }
    }

    fun hasData(widgetId: Int): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs.contains(dataKey(widgetId))
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    fun getAllWidgetIds(): Flow<Set<Int>> {
        return context.dataStore.data.map { prefs ->
            prefs.asMap().keys
                .filterIsInstance<Preferences.Key<String>>()
                .mapNotNull { key ->
                    key.name.removePrefix("widget_data_").toIntOrNull()
                }
                .toSet()
        }
    }
}
