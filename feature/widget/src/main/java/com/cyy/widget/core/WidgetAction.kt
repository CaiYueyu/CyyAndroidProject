package com.cyy.widget.core

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget 动作定义
 * 简化版，移除了 Glance 依赖
 */
sealed class WidgetAction {
    data object Refresh : WidgetAction()
    data class OpenApp(val extras: Map<String, String> = emptyMap()) : WidgetAction()
    data class OpenPage(val page: String, val extras: Map<String, String> = emptyMap()) : WidgetAction()
    data class PerformAction(val action: String, val params: Map<String, String> = emptyMap()) : WidgetAction()
    data class ToggleSetting(val settingKey: String) : WidgetAction()
    data class ShowDetail(val itemId: String) : WidgetAction()
}

/**
 * Widget 动作处理器接口
 * 简化版，使用传统 Widget ID (Int)
 */
interface WidgetActionHandler {
    fun handleAction(context: Context, widgetId: Int, action: WidgetAction)
}

/**
 * 默认动作处理器
 */
class DefaultWidgetActionHandler : WidgetActionHandler {
    
    override fun handleAction(context: Context, widgetId: Int, action: WidgetAction) {
        when (action) {
            is WidgetAction.Refresh -> handleRefresh(context, widgetId)
            is WidgetAction.OpenApp -> handleOpenApp(context, action.extras)
            is WidgetAction.OpenPage -> handleOpenPage(context, action.page, action.extras)
            is WidgetAction.PerformAction -> handlePerformAction(context, action.action, action.params)
            is WidgetAction.ToggleSetting -> handleToggleSetting(context, widgetId, action.settingKey)
            is WidgetAction.ShowDetail -> handleShowDetail(context, action.itemId)
        }
    }

    private fun handleRefresh(context: Context, widgetId: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
                putExtra("widget_id", widgetId)
                putExtra("action", "refresh")
            }
            context.sendBroadcast(intent)
        }
    }

    private fun handleOpenApp(context: Context, extras: Map<String, String>) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        intent?.let { context.startActivity(it) }
    }

    private fun handleOpenPage(context: Context, page: String, extras: Map<String, String>) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", page)
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        intent?.let { context.startActivity(it) }
    }

    private fun handlePerformAction(context: Context, action: String, params: Map<String, String>) {
        val intent = Intent("${context.packageName}.WIDGET_ACTION").apply {
            putExtra("action", action)
            params.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        context.sendBroadcast(intent)
    }

    private fun handleToggleSetting(context: Context, widgetId: Int, settingKey: String) {
        // 简化实现
    }

    private fun handleShowDetail(context: Context, itemId: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "show_detail")
            putExtra("item_id", itemId)
        }
        intent?.let { context.startActivity(it) }
    }
}
