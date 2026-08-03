package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType {
    OWED_TO_ME, // Мне должны (Receivable)
    I_OWE       // Я должен (Payable)
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val type: DebtType,
    val totalAmount: Double,
    val remainingAmount: Double,
    val currency: String = "₽",
    val createdDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long? = null,
    val comment: String = "",
    val isClosed: Boolean = false,
    val closedDateMillis: Long? = null
)
