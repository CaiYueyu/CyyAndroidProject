package com.cyy.widget.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cyy.widget.SampleAppWidgetReceiver
import com.cyy.widget.core.WidgetGlobalState
import com.cyy.widget.data.WidgetPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Widget 更新 Worker
 * 用于在后台定期或一次性更新 Widget 数据
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME_PERIODIC = "widget_periodic_update"
        const val WORK_NAME_ONE_TIME = "widget_one_time_update"
        const val KEY_WIDGET_ID = "widget_id"
        const val KEY_FORCE_REFRESH = "force_refresh"

        fun schedulePeriodicUpdate(
            context: Context,
            widgetId: Int,
            intervalMinutes: Long = 30
        ) {
            val workName = "${WORK_NAME_PERIODIC}_$widgetId"
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_WIDGET_ID to widgetId))
                .addTag("widget_update")
                .addTag("widget_$widgetId")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun executeOneTimeUpdate(
            context: Context,
            widgetId: Int,
            forceRefresh: Boolean = false
        ) {
            val workName = "${WORK_NAME_ONE_TIME}_$widgetId"

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    KEY_WIDGET_ID to widgetId,
                    KEY_FORCE_REFRESH to forceRefresh
                ))
                .addTag("widget_update")
                .addTag("widget_$widgetId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelUpdates(context: Context, widgetId: Int) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork("${WORK_NAME_PERIODIC}_$widgetId")
                cancelUniqueWork("${WORK_NAME_ONE_TIME}_$widgetId")
                cancelAllWorkByTag("widget_$widgetId")
            }
        }

        fun cancelAllUpdates(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("widget_update")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val widgetId = inputData.getInt(KEY_WIDGET_ID, -1)
            val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)

            if (widgetId == -1) {
                updateAllWidgets(forceRefresh)
            } else {
                updateSingleWidget(widgetId, forceRefresh)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }

    private suspend fun updateAllWidgets(forceRefresh: Boolean) {
        val activeWidgets = WidgetGlobalState.activeWidgets.value
        activeWidgets.forEach { widgetId ->
            updateSingleWidget(widgetId, forceRefresh)
        }
    }

    private suspend fun updateSingleWidget(widgetId: Int, forceRefresh: Boolean) {
        if (!WidgetGlobalState.isWidgetActive(widgetId)) {
            return
        }

        // 触发 Widget 更新
        val intent = android.content.Intent("${applicationContext.packageName}.WIDGET_DATA_UPDATED").apply {
            putExtra("widget_id", widgetId)
            putExtra("force_refresh", forceRefresh)
        }
        applicationContext.sendBroadcast(intent)
    }
}

/**
 * Widget 数据同步 Worker
 */
class WidgetSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "widget_data_sync"

        fun scheduleSync(context: Context, delayMinutes: Long = 0) {
            val workRequest = OneTimeWorkRequestBuilder<WidgetSyncWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag("widget_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val activeWidgets = WidgetGlobalState.activeWidgets.value
            activeWidgets.forEach { widgetId ->
                syncWidgetData(widgetId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncWidgetData(widgetId: Int) {
        // 实现数据同步逻辑
    }
}

/**
 * Widget 清理 Worker
 */
class WidgetCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "widget_cleanup"

        fun scheduleCleanup(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetCleanupWorker>(
                24, TimeUnit.HOURS
            )
                .addTag("widget_cleanup")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val dataStore = WidgetPreferencesDataStore.getInstance(context)

            val allWidgetIds = dataStore.getAllWidgetIds()

            val activeWidgets = WidgetGlobalState.activeWidgets.value
            allWidgetIds.collect { ids ->
                ids.forEach { widgetId ->
                    if (!activeWidgets.contains(widgetId)) {
                        dataStore.deleteData(widgetId)
                        WidgetUpdateWorker.cancelUpdates(context, widgetId)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
