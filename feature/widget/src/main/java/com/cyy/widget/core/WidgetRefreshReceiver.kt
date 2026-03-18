package com.cyy.widget.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetRefreshReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.cyy.widget.ACTION_REFRESH"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        const val EXTRA_ACTION_TYPE = "extra_action_type"

        fun sendRefreshBroadcast(context: Context, widgetId: Int? = null, actionType: String = "refresh") {
            val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
                action = ACTION_WIDGET_REFRESH
                widgetId?.let { putExtra(EXTRA_WIDGET_ID, it) }
                putExtra(EXTRA_ACTION_TYPE, actionType)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_REFRESH) return

        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: "refresh"

        CoroutineScope(Dispatchers.Default).launch {
            when (actionType) {
                "refresh" -> handleRefresh(context, widgetId)
                "force_refresh" -> handleForceRefresh(context, widgetId)
                "sync" -> handleSync(context, widgetId)
            }
        }
    }

    private suspend fun handleRefresh(context: Context, widgetId: Int) {
        if (widgetId == -1) {
            WidgetGlobalState.activeWidgets.value.forEach { id ->
                notifyWidgetRefresh(context, id)
            }
        } else {
            notifyWidgetRefresh(context, widgetId)
        }
    }

    private suspend fun handleForceRefresh(context: Context, widgetId: Int) {
        if (widgetId == -1) {
            WidgetGlobalState.activeWidgets.value.forEach { id ->
                notifyWidgetForceRefresh(context, id)
            }
        } else {
            notifyWidgetForceRefresh(context, widgetId)
        }
    }

    private suspend fun handleSync(context: Context, widgetId: Int) {
    }

    private suspend fun notifyWidgetRefresh(context: Context, widgetId: Int) {
        val intent = Intent("${context.packageName}.WIDGET_DATA_UPDATED").apply {
            putExtra("widget_id", widgetId)
            putExtra("refresh_type", "normal")
        }
        context.sendBroadcast(intent)
    }

    private suspend fun notifyWidgetForceRefresh(context: Context, widgetId: Int) {
        val intent = Intent("${context.packageName}.WIDGET_DATA_UPDATED").apply {
            putExtra("widget_id", widgetId)
            putExtra("refresh_type", "force")
        }
        context.sendBroadcast(intent)
    }
}
