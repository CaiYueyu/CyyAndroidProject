package com.cyy.widget.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface WidgetDataRepository<T> {
    fun getDataStream(widgetId: Int): Flow<T>
    suspend fun getLatestData(widgetId: Int): Result<T>
    suspend fun refreshData(widgetId: Int): Result<T>
    suspend fun saveData(widgetId: Int, data: T): Result<Unit>
    suspend fun clearData(widgetId: Int)
    suspend fun shouldUpdate(widgetId: Int): Boolean
}

abstract class BaseWidgetDataRepository<T>(
    protected val context: Context,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WidgetDataRepository<T> {
    
    protected val cache = mutableMapOf<Int, T>()
    protected val _dataUpdates = MutableSharedFlow<Pair<Int, T>>(extraBufferCapacity = 1)
    val dataUpdates: SharedFlow<Pair<Int, T>> = _dataUpdates.asSharedFlow()
    protected val _loadingState = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val loadingState: StateFlow<Map<Int, Boolean>> = _loadingState.asStateFlow()
    
    override fun getDataStream(widgetId: Int): Flow<T> = flow {
        cache[widgetId]?.let { emit(it) }
        val result = fetchFromSource(widgetId)
        result.onSuccess { data ->
            cache[widgetId] = data
            emit(data)
        }
    }.flowOn(ioDispatcher)
    
    override suspend fun getLatestData(widgetId: Int): Result<T> = withContext(ioDispatcher) {
        cache[widgetId]?.let {
            return@withContext Result.success(it)
        }
        fetchFromSource(widgetId).onSuccess { data ->
            cache[widgetId] = data
        }
    }
    
    override suspend fun refreshData(widgetId: Int): Result<T> = withContext(ioDispatcher) {
        setLoading(widgetId, true)
        try {
            fetchFromSource(widgetId).also { result ->
                result.onSuccess { data ->
                    cache[widgetId] = data
                    saveToLocal(widgetId, data)
                    _dataUpdates.tryEmit(widgetId to data)
                }
            }
        } finally {
            setLoading(widgetId, false)
        }
    }
    
    override suspend fun saveData(widgetId: Int, data: T): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            cache[widgetId] = data
            saveToLocal(widgetId, data)
            _dataUpdates.tryEmit(widgetId to data)
            Unit
        }
    }
    
    override suspend fun clearData(widgetId: Int) = withContext(ioDispatcher) {
        cache.remove(widgetId)
        clearLocalData(widgetId)
    }
    
    protected fun setLoading(widgetId: Int, isLoading: Boolean) {
        _loadingState.value = _loadingState.value.toMutableMap().apply {
            this[widgetId] = isLoading
        }
    }
    
    protected abstract suspend fun fetchFromSource(widgetId: Int): Result<T>
    protected abstract suspend fun saveToLocal(widgetId: Int, data: T)
    protected abstract suspend fun clearLocalData(widgetId: Int)
}

abstract class WidgetDataModel {
    abstract val id: String
    abstract val timestamp: Long
    
    fun isExpired(maxAgeMillis: Long): Boolean {
        return System.currentTimeMillis() - timestamp > maxAgeMillis
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val timestamp: Long) : SyncState()
    data class Error(val message: String, val retryCount: Int = 0) : SyncState()
}

class InMemoryWidgetDataRepository<T : WidgetDataModel>(
    context: Context,
    private val fetcher: suspend (Int) -> T
) : BaseWidgetDataRepository<T>(context) {
    
    override suspend fun fetchFromSource(widgetId: Int): Result<T> {
        return runCatching {
            fetcher(widgetId)
        }
    }
    
    override suspend fun saveToLocal(widgetId: Int, data: T) {
    }
    
    override suspend fun clearLocalData(widgetId: Int) {
    }
    
    override suspend fun shouldUpdate(widgetId: Int): Boolean {
        val data = cache[widgetId]
        return data == null || data.isExpired(5 * 60 * 1000)
    }
}
