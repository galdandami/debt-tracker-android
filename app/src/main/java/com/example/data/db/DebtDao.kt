package com.example.data.db

import androidx.room.*
import com.example.data.model.Debt
import com.example.data.model.DebtTransaction
import com.example.data.model.DebtWithTransactions
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts WHERE isClosed = 0 ORDER BY createdDateMillis DESC")
    fun getActiveDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE isClosed = 1 ORDER BY closedDateMillis DESC, createdDateMillis DESC")
    fun getArchivedDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :debtId LIMIT 1")
    fun getDebtById(debtId: Long): Flow<Debt?>

    @Query("SELECT * FROM debts WHERE id = :debtId LIMIT 1")
    suspend fun getDebtByIdOnce(debtId: Long): Debt?

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :debtId LIMIT 1")
    fun getDebtWithTransactions(debtId: Long): Flow<DebtWithTransactions?>

    @Query("SELECT DISTINCT personName FROM debts ORDER BY personName ASC")
    fun getAllPersonNames(): Flow<List<String>>

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsOnce(): List<Debt>

    @Query("SELECT * FROM debt_transactions")
    suspend fun getAllTransactionsOnce(): List<DebtTransaction>

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()

    @Query("DELETE FROM debt_transactions")
    suspend fun deleteAllTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun deleteDebtById(debtId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: DebtTransaction): Long

    @Query("SELECT * FROM debt_transactions WHERE debtId = :debtId ORDER BY dateMillis DESC")
    fun getTransactionsForDebt(debtId: Long): Flow<List<DebtTransaction>>

    @Delete
    suspend fun deleteTransaction(transaction: DebtTransaction)
}
