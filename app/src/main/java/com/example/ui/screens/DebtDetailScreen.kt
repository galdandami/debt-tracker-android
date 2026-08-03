package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Debt
import com.example.data.model.DebtTransaction
import com.example.data.model.DebtType
import com.example.ui.components.AddEditDebtSheet
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.DeadlineChip
import com.example.ui.components.PersonAvatar
import com.example.ui.theme.debtColors
import com.example.ui.viewmodel.DebtViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    debtId: Long,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtWithTransactionsState by viewModel.getDebtWithTransactions(debtId).collectAsState(initial = null)
    val contactNames by viewModel.contactNames.collectAsState()

    var showPaymentSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }

    val debtWithTrans = debtWithTransactionsState
    val debt = debtWithTrans?.debt
    val transactions = debtWithTrans?.transactions ?: emptyList()

    val debtColors = MaterialTheme.debtColors
    val accentColor = debt?.let {
        if (it.type == DebtType.OWED_TO_ME) debtColors.owedToMe else debtColors.iOwe
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(debt?.personName ?: "Детали долга", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (debt != null) {
                        IconButton(
                            onClick = { showEditSheet = true },
                            modifier = Modifier.testTag("btn_detail_edit")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("btn_detail_delete")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (debt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val progress = if (debt.totalAmount > 0) {
                ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat().coerceIn(0f, 1f)
            } else 0f

            val paidAmount = debt.totalAmount - debt.remainingAmount
            val deadlineInfo = DateUtils.calculateDeadlineInfo(debt.dueDateMillis)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: Status, Amount, Details
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PersonAvatar(
                                        name = debt.personName,
                                        accentColor = accentColor,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Column {
                                        Text(
                                            text = debt.personName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (debt.type == DebtType.OWED_TO_ME) "Мне должны" else "Я должен",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = accentColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    color = if (debt.isClosed) MaterialTheme.colorScheme.surfaceVariant else accentColor.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (debt.isClosed) "Закрыт" else "Активен",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (debt.isClosed) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Main Balances
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Осталось выплатить",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Исходная сумма",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyUtils.formatAmount(debt.totalAmount, debt.currency),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Payment Progress bar
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Выплачено: ${CurrencyUtils.formatAmount(paidAmount, debt.currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.2f)
                                )
                            }

                            // Dates & Comment Section
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Дата создания: ${DateUtils.formatDate(debt.createdDateMillis)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (deadlineInfo != null && !debt.isClosed) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Дедлайн: ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        DeadlineChip(info = deadlineInfo)
                                    }
                                }

                                if (debt.comment.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Цель/Комментарий: ${debt.comment}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons for active debts
                if (!debt.isClosed) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showPaymentSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_detail_partial_payment"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Внести частичный платеж", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showSettleDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_detail_full_settle"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Погасить полностью", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    item {
                        OutlinedButton(
                            onClick = {
                                viewModel.restoreDebt(debt.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_detail_restore"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Восстановить из архива")
                        }
                    }
                }

                // Payment History Section Header
                item {
                    Text(
                        text = "История платежей (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Payment History List
                if (transactions.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Платежей пока не было. Нажмите 'Внести частичный платеж', чтобы записать погашение.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->
                        PaymentTransactionItem(
                            transaction = transaction,
                            currency = debt.currency,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }

    // Partial Payment Bottom Sheet
    if (showPaymentSheet && debt != null) {
        AddTransactionSheet(
            debt = debt,
            onDismiss = { showPaymentSheet = false },
            onConfirmPayment = { amount, note ->
                viewModel.recordPartialPayment(debt.id, amount, note)
                showPaymentSheet = false
            }
        )
    }

    // Edit Debt Bottom Sheet
    if (showEditSheet && debt != null) {
        AddEditDebtSheet(
            existingDebt = debt,
            contactNames = contactNames,
            onDismiss = { showEditSheet = false },
            onSave = { name, type, amount, currency, dueDate, comment ->
                val updated = debt.copy(
                    personName = name,
                    type = type,
                    totalAmount = amount,
                    remainingAmount = if (amount < debt.remainingAmount) amount else debt.remainingAmount,
                    currency = currency,
                    dueDateMillis = dueDate,
                    comment = comment
                )
                viewModel.updateDebt(updated)
                showEditSheet = false
            }
        )
    }

    // Full Settle Dialog
    if (showSettleDialog && debt != null) {
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("Погасить полностью?") },
            text = { Text("Долг на сумму ${CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency)} будет помечен как выплаченный и перенесен в архив.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fullySettleDebt(debt.id, "Полное погашение")
                        showSettleDialog = false
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && debt != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Вы уверены, что хотите безвозвратно удалить этот долг и всю историю платежей?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PaymentTransactionItem(
    transaction: DebtTransaction,
    currency: String,
    accentColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.note.ifBlank { "Частичный платеж" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = DateUtils.formatDateTime(transaction.dateMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "- ${CurrencyUtils.formatAmount(transaction.amount, currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
