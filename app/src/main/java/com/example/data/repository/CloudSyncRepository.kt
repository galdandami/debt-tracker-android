package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.DebtDao
import com.example.data.model.Debt
import com.example.data.model.DebtTransaction
import com.example.data.model.DebtType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    data class Success(val timestampMillis: Long, val syncedDebtsCount: Int) : SyncState
    data class Error(val message: String) : SyncState
}

class CloudSyncRepository(
    private val context: Context,
    private val debtDao: DebtDao,
    private val authRepository: AuthRepository
) {
    private val cloudPrefs: SharedPreferences =
        context.getSharedPreferences("debt_tracker_cloud_db_store", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Performs cloud database synchronization for the logged-in user.
     * Pulls remote cloud records, merges with local Room database, and pushes back the unified state.
     */
    suspend fun performCloudSync(): SyncState = withContext(Dispatchers.IO) {
        val userSession = authRepository.userSession.value
        if (!userSession.isLoggedIn) {
            val errorState = SyncState.Error("Требуется авторизация для синхронизации с облаком")
            _syncState.value = errorState
            return@withContext errorState
        }

        _syncState.value = SyncState.Syncing

        try {
            // Simulate network cloud request delay
            delay(1200)

            val userId = userSession.userId
            val cloudDataKey = "cloud_snapshot_$userId"

            // 1. Fetch current local Room database items
            val localDebts = debtDao.getAllDebtsOnce()
            val localTransactions = debtDao.getAllTransactionsOnce()

            // 2. Fetch remote cloud database snapshot
            val rawCloudSnapshot = cloudPrefs.getString(cloudDataKey, null)

            val mergedDebtsMap = mutableMapOf<Long, Debt>()
            val mergedTransactionsMap = mutableMapOf<Long, DebtTransaction>()

            // Populate from remote cloud first if exists
            if (!rawCloudSnapshot.isNullOrBlank()) {
                val cloudJson = JSONObject(rawCloudSnapshot)
                val cloudDebtsArr = cloudJson.optJSONArray("debts") ?: JSONArray()
                val cloudTransArr = cloudJson.optJSONArray("transactions") ?: JSONArray()

                for (i in 0 until cloudDebtsArr.length()) {
                    val d = cloudDebtsArr.getJSONObject(i)
                    val debt = parseDebtFromJson(d)
                    mergedDebtsMap[debt.id] = debt
                }

                for (i in 0 until cloudTransArr.length()) {
                    val t = cloudTransArr.getJSONObject(i)
                    val trans = parseTransactionFromJson(t)
                    mergedTransactionsMap[trans.id] = trans
                }
            }

            // Merge local Room database entries (local updates take precedence or add new)
            for (debt in localDebts) {
                mergedDebtsMap[debt.id] = debt
            }
            for (trans in localTransactions) {
                mergedTransactionsMap[trans.id] = trans
            }

            // 3. Write merged dataset back to local Room database
            debtDao.deleteAllTransactions()
            debtDao.deleteAllDebts()

            for (debt in mergedDebtsMap.values) {
                debtDao.insertDebt(debt)
            }
            for (trans in mergedTransactionsMap.values) {
                debtDao.insertTransaction(trans)
            }

            // 4. Update cloud database store snapshot
            val newCloudJson = JSONObject().apply {
                put("userId", userId)
                put("updatedAt", System.currentTimeMillis())

                val debtsArr = JSONArray()
                for (debt in mergedDebtsMap.values) {
                    debtsArr.put(serializeDebtToJson(debt))
                }
                put("debts", debtsArr)

                val transArr = JSONArray()
                for (trans in mergedTransactionsMap.values) {
                    transArr.put(serializeTransactionToJson(trans))
                }
                put("transactions", transArr)
            }

            cloudPrefs.edit().putString(cloudDataKey, newCloudJson.toString()).apply()

            val now = System.currentTimeMillis()
            authRepository.updateLastSyncTime(now)

            val successState = SyncState.Success(now, mergedDebtsMap.size)
            _syncState.value = successState
            return@withContext successState
        } catch (e: Exception) {
            e.printStackTrace()
            val errorState = SyncState.Error("Ошибка облачной синхронизации: ${e.localizedMessage ?: "Ошибка сети"}")
            _syncState.value = errorState
            return@withContext errorState
        }
    }

    private fun parseDebtFromJson(d: JSONObject): Debt {
        return Debt(
            id = d.optLong("id", 0L),
            personName = d.getString("personName"),
            type = DebtType.valueOf(d.getString("type")),
            totalAmount = d.getDouble("totalAmount"),
            remainingAmount = d.getDouble("remainingAmount"),
            currency = d.optString("currency", "₽"),
            createdDateMillis = d.optLong("createdDateMillis", System.currentTimeMillis()),
            dueDateMillis = if (d.isNull("dueDateMillis")) null else d.optLong("dueDateMillis"),
            closedDateMillis = if (d.isNull("closedDateMillis")) null else d.optLong("closedDateMillis"),
            isClosed = d.optBoolean("isClosed", false),
            comment = d.optString("comment", "")
        )
    }

    private fun parseTransactionFromJson(t: JSONObject): DebtTransaction {
        return DebtTransaction(
            id = t.optLong("id", 0L),
            debtId = t.getLong("debtId"),
            amount = t.getDouble("amount"),
            dateMillis = t.optLong("dateMillis", System.currentTimeMillis()),
            note = t.optString("note", "")
        )
    }

    private fun serializeDebtToJson(debt: Debt): JSONObject {
        return JSONObject().apply {
            put("id", debt.id)
            put("personName", debt.personName)
            put("type", debt.type.name)
            put("totalAmount", debt.totalAmount)
            put("remainingAmount", debt.remainingAmount)
            put("currency", debt.currency)
            put("createdDateMillis", debt.createdDateMillis)
            put("dueDateMillis", debt.dueDateMillis ?: JSONObject.NULL)
            put("closedDateMillis", debt.closedDateMillis ?: JSONObject.NULL)
            put("isClosed", debt.isClosed)
            put("comment", debt.comment)
        }
    }

    private fun serializeTransactionToJson(t: DebtTransaction): JSONObject {
        return JSONObject().apply {
            put("id", t.id)
            put("debtId", t.debtId)
            put("amount", t.amount)
            put("dateMillis", t.dateMillis)
            put("note", t.note)
        }
    }
}
