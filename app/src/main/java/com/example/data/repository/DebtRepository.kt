package com.example.data.repository

import com.example.data.db.DebtDao
import com.example.data.model.Debt
import com.example.data.model.DebtTransaction
import com.example.data.model.DebtWithTransactions
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class DebtRepository(private val debtDao: DebtDao) {

    val activeDebts: Flow<List<Debt>> = debtDao.getActiveDebts()
    val archivedDebts: Flow<List<Debt>> = debtDao.getArchivedDebts()
    val allPersonNames: Flow<List<String>> = debtDao.getAllPersonNames()

    fun getDebtById(debtId: Long): Flow<Debt?> = debtDao.getDebtById(debtId)

    fun getDebtWithTransactions(debtId: Long): Flow<DebtWithTransactions?> =
        debtDao.getDebtWithTransactions(debtId)

    suspend fun insertDebt(debt: Debt): Long {
        return debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
    }

    suspend fun deleteDebtById(debtId: Long) {
        debtDao.deleteDebtById(debtId)
    }

    /**
     * Records a partial payment for a debt.
     * Updates the remaining balance and creates a transaction record.
     * If the remaining balance reaches zero or less, the debt is automatically closed/archived.
     */
    suspend fun recordPartialPayment(
        debtId: Long,
        paymentAmount: Double,
        note: String = ""
    ): Boolean {
        val debt = debtDao.getDebtByIdOnce(debtId) ?: return false
        if (debt.isClosed || paymentAmount <= 0) return false

        val newRemaining = max(0.0, debt.remainingAmount - paymentAmount)
        val isNowClosed = newRemaining <= 0.001

        val updatedDebt = debt.copy(
            remainingAmount = newRemaining,
            isClosed = isNowClosed,
            closedDateMillis = if (isNowClosed) System.currentTimeMillis() else debt.closedDateMillis
        )

        debtDao.updateDebt(updatedDebt)

        debtDao.insertTransaction(
            DebtTransaction(
                debtId = debtId,
                amount = paymentAmount,
                dateMillis = System.currentTimeMillis(),
                note = note.ifBlank { "Частичный платеж" }
            )
        )

        return true
    }

    /**
     * Fully settles a debt, recording a final payment transaction for whatever balance was left,
     * setting remainingAmount to 0 and marking the debt as closed/archived.
     */
    suspend fun fullySettleDebt(debtId: Long, note: String = "Полный расчет"): Boolean {
        val debt = debtDao.getDebtByIdOnce(debtId) ?: return false
        if (debt.isClosed) return false

        val remainingToPay = debt.remainingAmount

        val updatedDebt = debt.copy(
            remainingAmount = 0.0,
            isClosed = true,
            closedDateMillis = System.currentTimeMillis()
        )

        debtDao.updateDebt(updatedDebt)

        if (remainingToPay > 0) {
            debtDao.insertTransaction(
                DebtTransaction(
                    debtId = debtId,
                    amount = remainingToPay,
                    dateMillis = System.currentTimeMillis(),
                    note = note
                )
            )
        }

        return true
    }

    /**
     * Restores an archived debt back to active status.
     */
    suspend fun restoreDebt(debtId: Long): Boolean {
        val debt = debtDao.getDebtByIdOnce(debtId) ?: return false
        if (!debt.isClosed) return false

        // If it was closed with 0 remaining, we set remaining back to totalAmount or keep current remaining
        val restoreRemaining = if (debt.remainingAmount <= 0) debt.totalAmount else debt.remainingAmount

        val updatedDebt = debt.copy(
            remainingAmount = restoreRemaining,
            isClosed = false,
            closedDateMillis = null
        )

        debtDao.updateDebt(updatedDebt)
        return true
    }

    /**
     * Exports all database content (debts & transactions) into a JSON string format for backup/sync.
     */
    suspend fun exportDataToJson(): String {
        val debts = debtDao.getAllDebtsOnce()
        val transactions = debtDao.getAllTransactionsOnce()

        val rootJson = org.json.JSONObject()
        rootJson.put("version", 1)
        rootJson.put("exportDate", System.currentTimeMillis())

        val debtsArray = org.json.JSONArray()
        for (debt in debts) {
            val dJson = org.json.JSONObject().apply {
                put("id", debt.id)
                put("personName", debt.personName)
                put("type", debt.type.name)
                put("totalAmount", debt.totalAmount)
                put("remainingAmount", debt.remainingAmount)
                put("currency", debt.currency)
                put("createdDateMillis", debt.createdDateMillis)
                put("dueDateMillis", debt.dueDateMillis ?: org.json.JSONObject.NULL)
                put("closedDateMillis", debt.closedDateMillis ?: org.json.JSONObject.NULL)
                put("isClosed", debt.isClosed)
                put("comment", debt.comment)
            }
            debtsArray.put(dJson)
        }
        rootJson.put("debts", debtsArray)

        val transArray = org.json.JSONArray()
        for (t in transactions) {
            val tJson = org.json.JSONObject().apply {
                put("id", t.id)
                put("debtId", t.debtId)
                put("amount", t.amount)
                put("dateMillis", t.dateMillis)
                put("note", t.note)
            }
            transArray.put(tJson)
        }
        rootJson.put("transactions", transArray)

        return rootJson.toString(2)
    }

    /**
     * Imports and synchronizes data from a JSON backup string into the local Room database.
     */
    suspend fun importDataFromJson(jsonString: String, overwriteExisting: Boolean = true): Boolean {
        return try {
            val rootJson = org.json.JSONObject(jsonString)
            val debtsArray = rootJson.optJSONArray("debts") ?: return false
            val transArray = rootJson.optJSONArray("transactions")

            if (overwriteExisting) {
                debtDao.deleteAllTransactions()
                debtDao.deleteAllDebts()
            }

            for (i in 0 until debtsArray.length()) {
                val d = debtsArray.getJSONObject(i)
                val debt = Debt(
                    id = d.optLong("id", 0L),
                    personName = d.getString("personName"),
                    type = com.example.data.model.DebtType.valueOf(d.getString("type")),
                    totalAmount = d.getDouble("totalAmount"),
                    remainingAmount = d.getDouble("remainingAmount"),
                    currency = d.optString("currency", "₽"),
                    createdDateMillis = d.optLong("createdDateMillis", System.currentTimeMillis()),
                    dueDateMillis = if (d.isNull("dueDateMillis")) null else d.optLong("dueDateMillis"),
                    closedDateMillis = if (d.isNull("closedDateMillis")) null else d.optLong("closedDateMillis"),
                    isClosed = d.optBoolean("isClosed", false),
                    comment = d.optString("comment", "")
                )
                debtDao.insertDebt(debt)
            }

            if (transArray != null) {
                for (i in 0 until transArray.length()) {
                    val t = transArray.getJSONObject(i)
                    val trans = DebtTransaction(
                        id = t.optLong("id", 0L),
                        debtId = t.getLong("debtId"),
                        amount = t.getDouble("amount"),
                        dateMillis = t.optLong("dateMillis", System.currentTimeMillis()),
                        note = t.optString("note", "")
                    )
                    debtDao.insertTransaction(trans)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
