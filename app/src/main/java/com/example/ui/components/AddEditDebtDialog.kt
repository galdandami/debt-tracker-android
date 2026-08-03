package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.ui.theme.debtColors
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtSheet(
    existingDebt: Debt? = null,
    initialType: DebtType = DebtType.OWED_TO_ME,
    contactNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        personName: String,
        type: DebtType,
        amount: Double,
        currency: String,
        dueDateMillis: Long?,
        comment: String
    ) -> Unit
) {
    var personName by remember { mutableStateOf(existingDebt?.personName ?: "") }
    var selectedType by remember { mutableStateOf(existingDebt?.type ?: initialType) }
    var amountText by remember { mutableStateOf(existingDebt?.totalAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var selectedCurrency by remember { mutableStateOf(existingDebt?.currency ?: "₽") }
    var dueDateMillis by remember { mutableStateOf<Long?>(existingDebt?.dueDateMillis) }
    var comment by remember { mutableStateOf(existingDebt?.comment ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val debtColors = MaterialTheme.debtColors
    val accentColor = if (selectedType == DebtType.OWED_TO_ME) debtColors.owedToMe else debtColors.iOwe

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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sheet Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (existingDebt == null) "Новый долг" else "Редактировать долг",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_sheet")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Type Toggle: "Мне должны" vs "Я должен"
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedType == DebtType.OWED_TO_ME,
                    onClick = { selectedType = DebtType.OWED_TO_ME },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = debtColors.owedToMeContainer,
                        activeContentColor = debtColors.owedToMe
                    ),
                    modifier = Modifier.testTag("tab_type_owed_to_me")
                ) {
                    Text(
                        text = "Мне должны",
                        fontWeight = FontWeight.Bold
                    )
                }

                SegmentedButton(
                    selected = selectedType == DebtType.I_OWE,
                    onClick = { selectedType = DebtType.I_OWE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = debtColors.iOweContainer,
                        activeContentColor = debtColors.iOwe
                    ),
                    modifier = Modifier.testTag("tab_type_i_owe")
                ) {
                    Text(
                        text = "Я должен",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Person Name Field
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Имя человека *") },
                placeholder = { Text("Иван Петров") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_person_name")
            )

            // Contact Suggestion Chips
            if (contactNames.isNotEmpty()) {
                val filteredContacts = contactNames.filter {
                    it.contains(personName, ignoreCase = true) && it != personName
                }.take(5)

                if (filteredContacts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Быстрый выбор контакта:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredContacts) { name ->
                                SuggestionChip(
                                    onClick = {
                                        personName = name
                                        nameError = null
                                    },
                                    label = { Text(name) },
                                    icon = {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Amount & Currency Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.isNotBlank()) amountError = null
                    },
                    label = { Text("Сумма *") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Text(
                            text = selectedCurrency,
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
                        .weight(1f)
                        .testTag("input_amount")
                )
            }

            // Currency Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Валюта:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CurrencyUtils.popularCurrencies) { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr.symbol,
                            onClick = { selectedCurrency = curr.symbol },
                            label = { Text("${curr.symbol}  ${curr.name}", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor
                            )
                        )
                    }
                }
            }

            // Due Date Picker Field
            OutlinedCard(
                onClick = {
                    val cal = Calendar.getInstance()
                    dueDateMillis?.let { cal.timeInMillis = it }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            dueDateMillis = selectedCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("picker_due_date"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Column {
                            Text(
                                text = "Дата возврата (дедлайн)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dueDateMillis?.let { DateUtils.formatDate(it) } ?: "Не указана (без срока)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (dueDateMillis != null) {
                        IconButton(onClick = { dueDateMillis = null }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Очистить дату",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Comment / Note Field
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий / цель (опционально)") },
                placeholder = { Text("На обед, за билеты, покупка техники...") },
                leadingIcon = {
                    Icon(Icons.Default.Comment, contentDescription = null)
                },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_comment")
            )

            // Save Action Button
            Button(
                onClick = {
                    var isValid = true
                    if (personName.isBlank()) {
                        nameError = "Введите имя человека"
                        isValid = false
                    }
                    val amount = CurrencyUtils.parseAmount(amountText)
                    if (amount == null || amount <= 0) {
                        amountError = "Введите корректную сумму (> 0)"
                        isValid = false
                    }

                    if (isValid && amount != null) {
                        onSave(
                            personName.trim(),
                            selectedType,
                            amount,
                            selectedCurrency,
                            dueDateMillis,
                            comment.trim()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_debt"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingDebt == null) "Сохранить долг" else "Обновить долг",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
