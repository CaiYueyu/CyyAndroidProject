package com.cyy.widget.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WidgetConfig private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: WidgetConfig? = null

        fun getInstance(context: Context): WidgetConfig {
            return instance ?: synchronized(this) {
                instance ?: WidgetConfig(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private const val PREFS_NAME = "widget_config"
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)

        fun themeKey(widgetId: Int) = stringPreferencesKey("widget_${widgetId}_theme")
        fun updateIntervalKey(widgetId: Int) = intPreferencesKey("widget_${widgetId}_update_interval")
        fun isAutoUpdateKey(widgetId: Int) = booleanPreferencesKey("widget_${widgetId}_auto_update")
        fun customDataKey(widgetId: Int) = stringPreferencesKey("widget_${widgetId}_custom_data")
    }

    enum class Theme {
        LIGHT, DARK, SYSTEM, DYNAMIC
    }

    enum class Size(val minWidthDp: Int, val minHeightDp: Int) {
        SMALL(100, 100),
        MEDIUM(200, 100),
        LARGE(200, 200),
        EXTRA_LARGE(300, 200)
    }

    fun getTheme(widgetId: Int): Flow<Theme> {
        return context.dataStore.data.map { prefs ->
            prefs[themeKey(widgetId)]?.let { Theme.valueOf(it) } ?: Theme.SYSTEM
        }
    }

    suspend fun setTheme(widgetId: Int, theme: Theme) {
        context.dataStore.edit { prefs ->
            prefs[themeKey(widgetId)] = theme.name
        }
    }

    fun getUpdateInterval(widgetId: Int): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[updateIntervalKey(widgetId)] ?: 30
        }
    }

    suspend fun setUpdateInterval(widgetId: Int, intervalMinutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[updateIntervalKey(widgetId)] = intervalMinutes
        }
    }

    fun isAutoUpdate(widgetId: Int): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[isAutoUpdateKey(widgetId)] ?: true
        }
    }

    suspend fun setAutoUpdate(widgetId: Int, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[isAutoUpdateKey(widgetId)] = enabled
        }
    }

    fun getCustomData(widgetId: Int): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[customDataKey(widgetId)]
        }
    }

    suspend fun setCustomData(widgetId: Int, data: String) {
        context.dataStore.edit { prefs ->
            prefs[customDataKey(widgetId)] = data
        }
    }

    suspend fun clearConfig(widgetId: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(themeKey(widgetId))
            prefs.remove(updateIntervalKey(widgetId))
            prefs.remove(isAutoUpdateKey(widgetId))
            prefs.remove(customDataKey(widgetId))
        }
    }
}
