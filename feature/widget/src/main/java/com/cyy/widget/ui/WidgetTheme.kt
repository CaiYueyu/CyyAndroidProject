package com.cyy.widget.ui

import android.content.Context
import android.content.res.Configuration

/**
 * Widget 主题配置
 * 简化版，后续可扩展
 */
object WidgetTheme {
    
    fun isDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and 
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
