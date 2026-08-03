package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PersonAvatar
import com.example.ui.theme.debtColors
import com.example.ui.viewmodel.DebtViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: DebtViewModel,
    onNavigateToDetail: (debtId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedDebts by viewModel.filteredArchivedDebts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var debtToRestore by remember { mutableStateOf<Debt?>(null) }
    var debtToDelete by remember { mutableStateOf<Debt?>(null) }

    val debtColors = MaterialTheme.debtColors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Архив закрытых долгов", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_archive_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Поиск по архиву...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("input_search_archive")
            )

            if (archivedDebts.isEmpty()) {
                EmptyStateView(
                    title = "Архив пуст",
                    description = if (searchQuery.isNotBlank()) "Ничего не найдено по запросу \"$searchQuery\"" else "Здесь будут отображаться полностью погашенные долги.",
                    icon = Icons.Default.Archive
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = archivedDebts,
                        key = { it.id }
                    ) { debt ->
                        ArchivedDebtItem(
                            debt = debt,
                            onClick = { onNavigateToDetail(debt.id) },
                            onRestoreClick = { debtToRestore = debt },
                            onDeleteClick = { debtToDelete = debt }
                        )
                    }
                }
            }
        }
    }

    // Restore Dialog
    debtToRestore?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToRestore = null },
            title = { Text("Восстановить долг?") },
            text = { Text("Запись долга \"${debt.personName}\" будет возвращена на главный экран как активная.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDebt(debt.id)
                        debtToRestore = null
                    }
                ) {
                    Text("Восстановить")
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToRestore = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Dialog
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text("Удалить из архива?") },
            text = { Text("Запись о погашенном долге \"${debt.personName}\" будет удалена навсегда.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ArchivedDebtItem(
    debt: Debt,
    onClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val debtColors = MaterialTheme.debtColors
    val accentColor = if (debt.type == DebtType.OWED_TO_ME) debtColors.owedToMe else debtColors.iOwe

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("archived_item_${debt.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PersonAvatar(
                    name = debt.personName,
                    accentColor = accentColor,
                    modifier = Modifier.size(40.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (debt.type == DebtType.OWED_TO_ME) "Мне должны были" else "Я должен был",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatAmount(debt.totalAmount, debt.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = debtColors.owedToMe.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Погашен",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = debtColors.owedToMe,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (debt.closedDateMillis != null) {
                Text(
                    text = "Закрыт: ${DateUtils.formatDate(debt.closedDateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.testTag("btn_restore_archived_${debt.id}")
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = "Восстановить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("btn_delete_archived_${debt.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
