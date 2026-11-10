package com.namhs.cashnote.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 화면 ViewModel의 공통 베이스.
 *
 * @param S 화면 상태(UiState, 불변 data class)
 * @param I 사용자 의도(Intent, sealed interface)
 * @param E 일회성 이벤트(SideEffect, sealed interface)
 */
abstract class MviViewModel<S : Any, I : Any, E : Any>(initialState: S) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)

    // 화면은 이 StateFlow만 구독한다
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    // 버퍼가 있는 Channel이라 구독 전에 보낸 이벤트도 유실되지 않는다
    private val _sideEffect = Channel<E>(Channel.BUFFERED)
    val sideEffect: Flow<E> = _sideEffect.receiveAsFlow()

    protected val currentState: S
        get() = _uiState.value

    // 화면에서 들어오는 모든 Intent의 단일 진입점
    abstract fun onIntent(intent: I)

    // 현재 상태를 받아 새 상태를 만든다. update로 원자적으로 갱신한다
    protected fun setState(reduce: S.() -> S) {
        _uiState.update(reduce)
    }

    protected fun postSideEffect(effect: E) {
        viewModelScope.launch { _sideEffect.send(effect) }
    }
}
