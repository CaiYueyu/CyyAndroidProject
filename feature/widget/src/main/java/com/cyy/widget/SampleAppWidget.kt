package com.cyy.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.cyy.widget.worker.WidgetUpdateWorker

/**
 * 示例 Widget 实现
 * 使用传统 AppWidgetProvider 方式
 */
class SampleAppWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.schedulePeriodicUpdate(context, -1, 30)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateWorker.cancelAllUpdates(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            WidgetUpdateWorker.cancelUpdates(context, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Widget 更新逻辑
        }
    }
}
