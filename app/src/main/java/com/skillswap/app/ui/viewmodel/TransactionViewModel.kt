package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.CreditTransaction
import com.skillswap.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<CreditTransaction>>(emptyList())
    val transactions: StateFlow<List<CreditTransaction>> = _transactions.asStateFlow()

    fun loadTransactions(userId: String) {
        viewModelScope.launch {
            repository.getTransactionsFlow(userId).collect {
                _transactions.value = it
            }
        }
    }
}
