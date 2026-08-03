package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Debt
import com.example.ui.theme.debtColors
import com.example.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    val debtColors = MaterialTheme.debtColors
    val accentColor = if (debt.type == com.example.data.model.DebtType.OWED_TO_ME) debtColors.owedToMe else debtColors.iOwe

    val percentages = listOf(
        "25%" to 0.25,
        "50%" to 0.50,
        "75%" to 0.75,
        "100%" to 1.00
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Внести частичный платеж",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Кому/От кого: ${debt.personName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_payment_sheet")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Remaining balance banner
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Текущий остаток долга:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = { Text("Сумма платежа *") },
                placeholder = { Text("0") },
                leadingIcon = {
                    Text(
                        text = debt.currency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(start = 12.dp, end = 2.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_payment_amount")
            )

            // Quick Percentage Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Быстрый выбор доли:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(percentages) { (label, factor) ->
                        val calculatedAmount = debt.remainingAmount * factor
                        val formattedCalc = if (calculatedAmount % 1.0 == 0.0) {
                            calculatedAmount.toLong().toString()
                        } else {
                            String.format(java.util.Locale.US, "%.2f", calculatedAmount)
                        }

                        SuggestionChip(
                            onClick = {
                                amountText = formattedCalc
                                amountError = null
                            },
                            label = {
                                Text(
                                    text = "$label (${CurrencyUtils.formatAmount(calculatedAmount, debt.currency)})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                }
            }

            // Optional Payment Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Заметка к платежу (опционально)") },
                placeholder = { Text("Перевод на карту, частичный расчет...") },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_payment_note")
            )

            // Submit Button
            Button(
                onClick = {
                    val amount = CurrencyUtils.parseAmount(amountText)
                    if (amount == null || amount <= 0) {
                        amountError = "Сумма должна быть больше 0"
                    } else if (amount > debt.remainingAmount + 0.01) {
                        amountError = "Сумма не может превышать остаток (${CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency)})"
                    } else {
                        onConfirmPayment(amount, noteText)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_confirm_payment"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                )
            ) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Подтвердить платеж",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
