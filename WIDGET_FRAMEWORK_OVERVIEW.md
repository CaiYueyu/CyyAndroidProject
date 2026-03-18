# AppWidget 框架概览

## 框架架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              UI 层 (Glance)                              │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │ WidgetTheme │  │ WidgetCard  │  │ WidgetHeader│  │ WidgetListItem  │ │
│  │  主题管理   │  │  卡片容器   │  │   标题栏    │  │    列表项       │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │WidgetButton │  │ WidgetInfo  │  │WidgetEmpty  │  │  WidgetError    │ │
│  │   按钮      │  │  信息块     │  │   空状态    │  │    错误状态     │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Widget 核心层                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │  WidgetConfig   │  │  WidgetState    │  │      WidgetAction       │  │
│  │   配置管理      │  │   状态管理      │  │      动作处理           │  │
│  │  - DataStore    │  │  - StateFlow    │  │  - 刷新                 │  │
│  │  - 主题设置     │  │  - UI状态       │  │  - 打开应用             │  │
│  │  - 更新间隔     │  │  - 加载/错误    │  │  - 页面跳转             │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    WidgetGlobalState                              │  │
│  │                      全局状态管理                                  │  │
│  │            - 活跃 Widget 追踪                                      │  │
│  │            - 全局主题设置                                          │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            数据层                                        │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐   │
│  │   WidgetDataRepository  │  │     WidgetPreferencesDataStore      │   │
│  │      数据仓库接口        │  │          数据持久化                  │   │
│  │  ├─ fetchFromSource()   │  │  ├─ saveData() / getData()          │   │
│  │  ├─ saveData()          │  │  ├─ deleteData()                    │   │
│  │  ├─ refreshData()       │  │  ├─ DataStore 封装                   │   │
│  │  └─ clearData()         │  │  └─ 自动序列化/反序列化              │   │
│  └─────────────────────────┘  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         后台任务层 (WorkManager)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────┐  │
│  │  WidgetUpdateWorker │  │   WidgetSyncWorker  │  │WidgetCleanupWork│  │
│  │    更新 Worker      │  │    同步 Worker      │  │   清理 Worker   │  │
│  │  - 定期更新         │  │  - 批量数据同步     │  │  - 过期数据清理 │  │
│  │  - 一次性更新       │  │  - 网络状态感知     │  │  - 资源释放     │  │
│  │  - 约束条件检查     │  │                     │  │                 │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## 核心类职责

### 1. WidgetConfig
**职责**：管理每个 Widget 实例的配置
- 主题设置（LIGHT/DARK/SYSTEM/DYNAMIC）
- 更新间隔
- 自动更新开关
- 自定义数据存储
- 使用 DataStore 持久化

### 2. WidgetState / WidgetStateManager
**职责**：管理 Widget 的 UI 状态
- Loading / Success / Error / Empty 四种状态
- StateFlow 驱动 UI 更新
- 支持状态转换和数据缓存

### 3. WidgetAction / DefaultWidgetActionHandler
**职责**：处理用户交互
- 刷新数据
- 打开应用/页面
- 执行自定义操作
- 切换设置
- 显示详情

### 4. WidgetDataRepository
**职责**：数据获取和管理
- 定义标准数据操作接口
- 内存缓存 + 本地持久化
- 支持离线优先策略

### 5. WidgetUpdateWorker
**职责**：后台定时更新
- 定期更新 Widget 数据
- 网络状态感知
- 电池优化
- 可配置约束条件

## 创建新 Widget 的步骤

```kotlin
// Step 1: 定义数据模型
data class MyWidgetData(
    override val id: String,
    val content: String,
    override val timestamp: Long = System.currentTimeMillis()
) : WidgetDataModel()

// Step 2: 实现 Widget
class MyAppWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = getWidgetId(context, id)
        WidgetGlobalState.registerWidget(widgetId)
        
        provideContent {
            GlanceTheme(
                colors = WidgetTheme.getColorProviders(WidgetConfig.Theme.SYSTEM, context)
            ) {
                MyWidgetContent()
            }
        }
    }
}

// Step 3: 创建 Receiver
class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyAppWidget()
}

// Step 4: 配置 AndroidManifest.xml
<receiver android:name=".widget.MyAppWidgetReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/my_app_widget_info" />
</receiver>

// Step 5: 创建配置 XML (res/xml/my_app_widget_info.xml)
```

## 技术特性

| 特性 | 实现方式 | 说明 |
|------|---------|------|
| 声明式 UI | Glance | Jetpack Compose 风格 API |
| 响应式编程 | Kotlin Flow | StateFlow/SharedFlow |
| 依赖注入 | Hilt | 构造函数注入 |
| 后台任务 | WorkManager | 约束感知、省电优化 |
| 数据存储 | DataStore | 类型安全、异步 |
| 序列化 | Kotlinx Serialization | 零反射、高性能 |
| 主题适配 | Material 3 | 动态取色支持 |

## 文件清单

```
app/src/main/java/com/cyy/android/
├── MyApplication.kt
└── widget/
    ├── SampleAppWidget.kt                    # 示例 Widget
    ├── WidgetConfigureActivity.kt            # 配置界面
    ├── core/
    │   ├── WidgetAction.kt                   # 动作定义和处理器
    │   ├── WidgetConfig.kt                   # 配置管理
    │   ├── WidgetRefreshReceiver.kt          # 刷新广播接收器
    │   └── WidgetState.kt                    # 状态管理
    ├── data/
    │   ├── WidgetDataRepository.kt           # 数据仓库
    │   └── WidgetPreferencesDataStore.kt     # 数据存储
    ├── ui/
    │   ├── WidgetComponents.kt               # UI 组件
    │   └── WidgetTheme.kt                    # 主题管理
    └── worker/
        └── WidgetUpdateWorker.kt             # 后台更新 Worker

app/src/main/res/
├── layout/
│   └── widget_initial.xml                    # 初始布局
├── drawable/
│   └── widget_background.xml                 # 背景样式
├── xml/
│   └── sample_app_widget_info.xml            # Widget 配置
└── values/
    ├── colors.xml                            # 颜色定义
    └── strings.xml                           # 字符串定义

gradle/
└── libs.versions.toml                        # 依赖版本管理
```

## 依赖项

```toml
[versions]
glance = "1.1.1"
composeBom = "2025.02.00"
hilt = "2.55"
workManager = "2.10.0"
coroutines = "1.10.1"
kotlinxSerialization = "1.8.0"

[libraries]
androidx-glance = { group = "androidx.glance", name = "glance", version.ref = "glance" }
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```

## 后续扩展建议

1. **添加更多预置组件**
   - 图表组件
   - 图片轮播
   - 时间线组件

2. **增强数据同步**
   - 离线队列
   - 增量更新
   - 冲突解决

3. **完善配置界面**
   - 预览功能
   - 更多主题选项
   - 布局选择器

4. **性能优化**
   - 图片缓存
   - 数据压缩
   - 懒加载

5. **测试覆盖**
   - 单元测试
   - 集成测试
   - UI 测试
