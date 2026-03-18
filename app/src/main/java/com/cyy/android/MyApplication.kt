package com.cyy.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口类
 * 初始化 Hilt 依赖注入
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 应用初始化逻辑
    }
}
