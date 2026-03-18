# AppWidget 框架使用指南

本项目实现了一个基于 **Glance** 的现代化通用 AppWidget 框架，使用 Android 最新和稳定的技术栈。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Glance | 1.1.1 | 声明式 Widget UI |
| Compose | BOM 2025.02.00 | UI 构建 |
| Hilt | 2.55 | 依赖注入 |
| WorkManager | 2.10.0 | 后台更新 |
| Coroutines | 1.10.1 | 异步处理 |
| DataStore | 内置 | 数据持久化 |
| Kotlin Serialization | 1.8.0 | JSON 序列化 |

## 框架结构

```
com.cyy.android.widget/
├── core/                       # 核心框架类
│   ├── WidgetConfig.kt         # Widget 配置管理
│   ├── WidgetState.kt          # Widget 状态管理
│   ├── WidgetAction.kt         # Widget 动作处理
│   └── WidgetRefreshReceiver.kt # 刷新广播接收器
├── ui/                         # UI 组件
│   ├── WidgetTheme.kt          # 主题管理
│   └── WidgetComponents.kt     # 通用 UI 组件
├── data/                       # 数据层
│   ├── WidgetDataRepository.kt # 数据仓库接口
│   └── WidgetPreferencesDataStore.kt # 数据存储
├── worker/                     # 后台任务
│   └── WidgetUpdateWorker.kt   # 更新 Worker
├── SampleAppWidget.kt          # 示例 Widget
└── WidgetConfigureActivity.kt  # 配置界面
```

## 快速开始

### 1. 创建新的 Widget

继承 `GlanceAppWidget` 并实现你的 Widget：

```kotlin
class MyAppWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 初始化配置
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
```

### 2. 创建 Widget Receiver

```kotlin
class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyAppWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 安排定期更新
        WidgetUpdateWorker.schedulePeriodicUpdate(context, -1, 30)
    }
}
```

### 3. 注册到 AndroidManifest.xml

```xml
<receiver
    android:name=".widget.MyAppWidgetReceiver"
    android:exported="true"
    android:label="我的 Widget">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/my_app_widget_info" />
</receiver>
```

### 4. 创建 Widget 配置文件

在 `res/xml/my_app_widget_info.xml`：

```xml
<appwidget-provider 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/widget_initial"
    android:minWidth="180dp"
    android:minHeight="180dp"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
```

## 核心功能

### 配置管理

```kotlin
val config = WidgetConfig.getInstance(context)

// 设置主题
config.setTheme(widgetId, WidgetConfig.Theme.DARK)

// 设置更新间隔
config.setUpdateInterval(widgetId, 60) // 60分钟

// 保存自定义数据
config.setCustomData(widgetId, "{\"key\":\"value\"}")
```

### 状态管理

```kotlin
class MyWidgetViewModel {
    private val stateManager = WidgetStateManager<MyData>()
    val state: StateFlow<WidgetUiState<MyData>> = stateManager.state

    fun loadData() {
        stateManager.setLoading()
        // 加载数据...
        stateManager.setSuccess(data)
    }
}
```

### 使用预置 UI 组件

```kotlin
@Composable
fun MyWidgetContent() {
    Column {
        // 标题栏
        WidgetHeader(
            title = "今日概览",
            actionIcon = R.drawable.ic_refresh,
            onActionClick = action { /* 刷新 */ }
        )

        // 卡片容器
        WidgetCard {
            // 内容
        }

        // 列表项
        WidgetListItem(
            title = "项目 A",
            subtitle = "进行中",
            trailing = "85%",
            onClick = action { /* 点击 */ }
        )

        // 信息块
        WidgetInfoBlock(
            label = "总计",
            value = "100"
        )

        // 按钮
        WidgetButton(
            text = "查看更多",
            onClick = action { /* 点击 */ }
        )

        // 状态组件
        when (uiState) {
            is WidgetUiState.Loading -> WidgetLoadingState()
            is WidgetUiState.Error -> WidgetErrorState(
                message = "加载失败",
                onRetry = action { /* 重试 */ }
            )
            is WidgetUiState.Empty -> WidgetEmptyState()
            is WidgetUiState.Success -> Content(data)
        }
    }
}
```

### 后台更新

```kotlin
// 安排定期更新（每30分钟）
WidgetUpdateWorker.schedulePeriodicUpdate(context, widgetId, 30)

// 执行一次性更新
WidgetUpdateWorker.executeOneTimeUpdate(context, widgetId, forceRefresh = true)

// 取消更新
WidgetUpdateWorker.cancelUpdates(context, widgetId)
```

### 数据仓库

```kotlin
// 创建自定义数据仓库
class MyWidgetRepository @Inject constructor(
    context: Context
) : BaseWidgetDataRepository<MyData>(context) {

    override suspend fun fetchFromSource(widgetId: Int): Result<MyData> {
        return runCatching {
            // 从网络/数据库获取数据
            api.fetchData()
        }
    }

    override suspend fun saveToLocal(widgetId: Int, data: MyData) {
        dataStore.saveData(widgetId, data)
    }

    override suspend fun clearLocalData(widgetId: Int) {
        dataStore.deleteData(widgetId)
    }

    override suspend fun shouldUpdate(widgetId: Int): Boolean {
        val data = cache[widgetId]
        return data?.isExpired(5 * 60 * 1000) ?: true
    }
}
```

## 主题配置

框架支持多种主题模式：

| 主题 | 说明 |
|------|------|
| `LIGHT` | 固定浅色主题 |
| `DARK` | 固定深色主题 |
| `SYSTEM` | 跟随系统设置（默认） |
| `DYNAMIC` | 动态取色（Android 12+） |

```kotlin
// 在 Widget 中使用主题
val config = WidgetConfig.getInstance(context)
val theme = config.getTheme(widgetId).first()

GlanceTheme(
    colors = WidgetTheme.getColorProviders(theme, context)
) {
    // Widget 内容
}
```

## 最佳实践

### 1. 性能优化

- 使用 `StateFlow` 管理状态，避免不必要的重组
- 数据使用内存缓存 + DataStore 持久化
- 合理使用 `WorkManager` 避免频繁更新
- 图片使用适当尺寸，避免内存浪费

### 2. 用户体验

- 提供加载、错误、空状态处理
- 支持主题跟随系统
- 响应式布局，适配不同尺寸
- 提供配置界面让用户自定义

### 3. 代码组织

```kotlin
// 数据模型
data class MyWidgetData(
    override val id: String,
    val items: List<Item>,
    override val timestamp: Long
) : WidgetDataModel()

// Widget 类
class MyAppWidget : GlanceAppWidget() { ... }

// Receiver 类
class MyAppWidgetReceiver : GlanceAppWidgetReceiver() { ... }

// 配置 Activity
class MyWidgetConfigureActivity : ComponentActivity() { ... }
```

## 扩展功能

### 添加自定义动作

```kotlin
// 自定义动作处理器
class CustomActionHandler : WidgetActionHandler {
    override fun handleAction(
        context: Context, 
        glanceId: GlanceId, 
        action: WidgetAction
    ) {
        when (action) {
            is MyCustomAction -> { /* 处理 */ }
            else -> DefaultWidgetActionHandler().handleAction(context, glanceId, action)
        }
    }
}
```

### 添加自定义主题

```kotlin
object MyCustomTheme {
    val CustomColors = ColorProviders(
        primary = ColorProvider(Color(0xFFYourColor)),
        // ... 其他颜色
    )
}
```

## 注意事项

1. **内存限制**：Widget 进程有内存限制，避免加载大图
2. **更新频率**：最小更新间隔为 30 分钟，避免频繁刷新
3. **后台限制**：Android 12+ 对后台启动 Activity 有限制
4. **权限**：需要声明 `BIND_APPWIDGET` 权限

## 参考资源

- [Glance 官方文档](https://developer.android.com/jetpack/compose/glance)
- [AppWidget 开发指南](https://developer.android.com/guide/topics/appwidgets)
- [WorkManager 文档](https://developer.android.com/topic/libraries/architecture/workmanager)
