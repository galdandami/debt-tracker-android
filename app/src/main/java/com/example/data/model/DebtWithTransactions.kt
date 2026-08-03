package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class DebtWithTransactions(
    @Embedded val debt: Debt,
    @Relation(
        parentColumn = "id",
        entityColumn = "debtId"
    )
    val transactions: List<DebtTransaction>
)
