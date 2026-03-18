package com.cyy.widget.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Widget 状态管理类
 * 用于管理 Widget 的 UI 状态和数据状态
 */
sealed class WidgetUiState<out T> {
    data object Loading : WidgetUiState<Nothing>()
    data class Success<T>(val data: T) : WidgetUiState<T>()
    data class Error(val message: String) : WidgetUiState<Nothing>()
    data object Empty : WidgetUiState<Nothing>()
}

/**
 * Widget 状态持有者
 * 每个 Widget 实例拥有一个状态管理器
 */
class WidgetStateManager<T>(initialState: WidgetUiState<T> = WidgetUiState.Loading) {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<WidgetUiState<T>> = _state.asStateFlow()

    fun updateState(newState: WidgetUiState<T>) {
        _state.value = newState
    }

    fun setSuccess(data: T) {
        _state.value = WidgetUiState.Success(data)
    }

    fun setError(message: String) {
        _state.value = WidgetUiState.Error(message)
    }

    fun setLoading() {
        _state.value = WidgetUiState.Loading
    }

    fun setEmpty() {
        _state.value = WidgetUiState.Empty
    }

    fun getCurrentData(): T? {
        return (_state.value as? WidgetUiState.Success<T>)?.data
    }
}

/**
 * Widget 全局状态管理器
 * 用于管理所有 Widget 的注册状态和全局配置
 */
object WidgetGlobalState {
    
    private val _activeWidgets = MutableStateFlow<Set<Int>>(emptySet())
    val activeWidgets: StateFlow<Set<Int>> = _activeWidgets.asStateFlow()

    fun registerWidget(widgetId: Int) {
        _activeWidgets.update { it + widgetId }
    }

    fun unregisterWidget(widgetId: Int) {
        _activeWidgets.update { it - widgetId }
    }

    fun isWidgetActive(widgetId: Int): Boolean {
        return _activeWidgets.value.contains(widgetId)
    }

    fun clear() {
        _activeWidgets.value = emptySet()
    }
}
