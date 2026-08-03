package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.DebtWithTransactions
import com.example.data.model.UserSession
import com.example.data.repository.AuthRepository
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.DebtRepository
import com.example.data.repository.SyncState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DebtViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DebtRepository
    val authRepository: AuthRepository
    val cloudSyncRepository: CloudSyncRepository

    init {
        val database = AppDatabase.getDatabase(application)
        val debtDao = database.debtDao()
        repository = DebtRepository(debtDao)
        authRepository = AuthRepository(application)
        cloudSyncRepository = CloudSyncRepository(application, debtDao, authRepository)
    }

    val userSession: StateFlow<UserSession> = authRepository.userSession
    val syncState: StateFlow<SyncState> = cloudSyncRepository.syncState

    fun login(email: String, pass: String, onResult: (Result<UserSession>) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.login(email, pass)
            onResult(res)
            if (res.isSuccess && userSession.value.autoSyncEnabled) {
                cloudSyncRepository.performCloudSync()
            }
        }
    }

    fun register(name: String, email: String, pass: String, onResult: (Result<UserSession>) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.register(name, email, pass)
            onResult(res)
            if (res.isSuccess) {
                cloudSyncRepository.performCloudSync()
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            cloudSyncRepository.performCloudSync()
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        authRepository.setAutoSyncEnabled(enabled)
    }

    // Selected Tab on Dashboard ("Мне должны" vs "Я должен")
    val selectedTab = MutableStateFlow(DebtType.OWED_TO_ME)

    // Search Query
    val searchQuery = MutableStateFlow("")

    // All active debts from DB
    val activeDebts: StateFlow<List<Debt>> = repository.activeDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All archived debts from DB
    val archivedDebts: StateFlow<List<Debt>> = repository.archivedDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Distinct contact names for autocomplete picker
    val contactNames: StateFlow<List<String>> = repository.allPersonNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total Owed To Me ("Мне должны") - sum of active remaining amounts
    val totalOwedToMe: StateFlow<Double> = activeDebts.map { list ->
        list.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total I Owe ("Я должен") - sum of active remaining amounts
    val totalIOwe: StateFlow<Double> = activeDebts.map { list ->
        list.filter { it.type == DebtType.I_OWE }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Counts for badges
    val countOwedToMe: StateFlow<Int> = activeDebts.map { list ->
        list.count { it.type == DebtType.OWED_TO_ME }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countIOwe: StateFlow<Int> = activeDebts.map { list ->
        list.count { it.type == DebtType.I_OWE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtered Active Debts based on tab and search query
    val filteredActiveDebts: StateFlow<List<Debt>> = combine(
        activeDebts,
        selectedTab,
        searchQuery
    ) { list, tab, query ->
        list.filter { debt ->
            debt.type == tab &&
            (query.isBlank() ||
             debt.personName.contains(query, ignoreCase = true) ||
             debt.comment.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Archived Debts based on search query
    val filteredArchivedDebts: StateFlow<List<Debt>> = combine(
        archivedDebts,
        searchQuery
    ) { list, query ->
        list.filter { debt ->
            query.isBlank() ||
            debt.personName.contains(query, ignoreCase = true) ||
            debt.comment.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: DebtType) {
        selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun getDebtWithTransactions(debtId: Long): Flow<DebtWithTransactions?> {
        return repository.getDebtWithTransactions(debtId)
    }

    private fun checkAutoSync() {
        val session = userSession.value
        if (session.isLoggedIn && session.autoSyncEnabled) {
            viewModelScope.launch {
                cloudSyncRepository.performCloudSync()
            }
        }
    }

    fun addDebt(
        personName: String,
        type: DebtType,
        amount: Double,
        currency: String = "₽",
        dueDateMillis: Long? = null,
        comment: String = ""
    ) {
        viewModelScope.launch {
            val debt = Debt(
                personName = personName.trim(),
                type = type,
                totalAmount = amount,
                remainingAmount = amount,
                currency = currency.ifBlank { "₽" },
                createdDateMillis = System.currentTimeMillis(),
                dueDateMillis = dueDateMillis,
                comment = comment.trim()
            )
            repository.insertDebt(debt)
            checkAutoSync()
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            repository.updateDebt(debt)
            checkAutoSync()
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            checkAutoSync()
        }
    }

    fun recordPartialPayment(debtId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordPartialPayment(debtId, amount, note)
            checkAutoSync()
        }
    }

    fun fullySettleDebt(debtId: Long, note: String = "Полный расчет") {
        viewModelScope.launch {
            repository.fullySettleDebt(debtId, note)
            checkAutoSync()
        }
    }

    fun restoreDebt(debtId: Long) {
        viewModelScope.launch {
            repository.restoreDebt(debtId)
            checkAutoSync()
        }
    }

    fun exportDataToJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            onResult(json)
        }
    }

    fun importDataFromJson(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importDataFromJson(jsonString)
            onResult(success)
        }
    }
}
