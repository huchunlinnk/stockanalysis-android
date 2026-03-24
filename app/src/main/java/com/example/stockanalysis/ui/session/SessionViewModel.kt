package com.example.stockanalysis.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.ChatSession
import com.example.stockanalysis.data.repository.ChatSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: ChatSessionRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private var currentQuery: String = ""

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            sessionRepository.getAllSessions().collect { sessions ->
                _sessions.value = sessions
            }
        }
    }

    fun searchSessions(query: String) {
        currentQuery = query
        viewModelScope.launch {
            if (query.isBlank()) {
                sessionRepository.getAllSessions().collect { sessions ->
                    _sessions.value = sessions
                }
            } else {
                sessionRepository.searchSessions(query).collect { sessions ->
                    _sessions.value = sessions
                }
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    suspend fun exportSessionAsMarkdown(sessionId: Long): String {
        return sessionRepository.exportSessionAsMarkdown(sessionId)
    }
}
