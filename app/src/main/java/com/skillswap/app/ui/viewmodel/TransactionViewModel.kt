package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.CreditTransaction
import com.skillswap.app.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<CreditTransaction>>(emptyList())
    val transactions: StateFlow<List<CreditTransaction>> = _transactions.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    fun loadTransactions(userId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getTransactionsFlow(userId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { _transactions.value = it }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
