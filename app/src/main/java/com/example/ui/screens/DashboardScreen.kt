package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.ui.components.AddEditDebtSheet
import com.example.ui.components.AddTransactionSheet
import com.example.ui.components.DebtItemCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SummaryCardsHeader
import com.example.ui.components.SyncBackupDialog
import com.example.ui.viewmodel.DebtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DebtViewModel,
    onNavigateToDetail: (debtId: Long) -> Unit,
    onNavigateToArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalOwedToMe by viewModel.totalOwedToMe.collectAsState()
    val totalIOwe by viewModel.totalIOwe.collectAsState()
    val countOwedToMe by viewModel.countOwedToMe.collectAsState()
    val countIOwe by viewModel.countIOwe.collectAsState()
    val activeDebts by viewModel.filteredActiveDebts.collectAsState()
    val contactNames by viewModel.contactNames.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<Debt?>(null) }
    var selectedDebtForSettle by remember { mutableStateOf<Debt?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Поиск по имени или комментарию...") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Закрыть поиск")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_dashboard")
                        )
                    } else {
                        Text(
                            text = "Учет долгов",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("btn_search_toggle")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    }
                    IconButton(
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.testTag("btn_sync_backup")
                    ) {
                        BadgedBox(
                            badge = {
                                if (userSession.isLoggedIn) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("✓")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (userSession.isLoggedIn) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Облачная синхронизация"
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToArchive,
                        modifier = Modifier.testTag("btn_go_archive")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Архив долгов")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_debt")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Cards Header ("Мне должны" vs "Я должен") - acts as the tab switcher
            SummaryCardsHeader(
                totalOwedToMe = totalOwedToMe,
                totalIOwe = totalIOwe,
                countOwedToMe = countOwedToMe,
                countIOwe = countIOwe,
                selectedTab = selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )

            // Debt List or Empty State
            if (activeDebts.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    EmptyStateView(
                        title = "Ничего не найдено",
                        description = "По запросу \"$searchQuery\" долгов не найдено.",
                        icon = Icons.Default.SearchOff
                    )
                } else if (selectedTab == DebtType.OWED_TO_ME) {
                    EmptyStateView(
                        title = "Вам никто не должен",
                        description = "Все расчеты проведены! Нажмите '+', чтобы записать кому вы дали в долг.",
                        actionText = "Дать в долг",
                        onActionClick = { showAddSheet = true }
                    )
                } else {
                    EmptyStateView(
                        title = "Вы никому не должны",
                        description = "У вас нет активных задолженностей! Отличный результат.",
                        actionText = "Взять в долг",
                        onActionClick = { showAddSheet = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = activeDebts,
                        key = { it.id }
                    ) { debt ->
                        DebtItemCard(
                            debt = debt,
                            onClick = { onNavigateToDetail(debt.id) },
                            onQuickPaymentClick = { selectedDebtForPayment = debt },
                            onQuickSettleClick = { selectedDebtForSettle = debt }
                        )
                    }
                }
            }
        }
    }

    // Add Debt Bottom Sheet
    if (showAddSheet) {
        AddEditDebtSheet(
            initialType = selectedTab,
            contactNames = contactNames,
            onDismiss = { showAddSheet = false },
            onSave = { name, type, amount, currency, dueDate, comment ->
                viewModel.addDebt(name, type, amount, currency, dueDate, comment)
                showAddSheet = false
            }
        )
    }

    // Sync & Backup Dialog
    if (showSyncDialog) {
        SyncBackupDialog(
            viewModel = viewModel,
            onDismiss = { showSyncDialog = false }
        )
    }

    // Quick Partial Payment Sheet
    selectedDebtForPayment?.let { debt ->
        AddTransactionSheet(
            debt = debt,
            onDismiss = { selectedDebtForPayment = null },
            onConfirmPayment = { amount, note ->
                viewModel.recordPartialPayment(debt.id, amount, note)
                selectedDebtForPayment = null
            }
        )
    }

    // Quick Settle Confirmation Dialog
    selectedDebtForSettle?.let { debt ->
        AlertDialog(
            onDismissRequest = { selectedDebtForSettle = null },
            title = { Text("Погасить полностью?") },
            text = {
                Text(
                    "Долг человека \"${debt.personName}\" на сумму " +
                    "${com.example.util.CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency)} " +
                    "будет полностью закрыт и перенесен в Архив."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fullySettleDebt(debt.id, "Полный расчет")
                        selectedDebtForSettle = null
                    },
                    modifier = Modifier.testTag("btn_confirm_settle_dialog")
                ) {
                    Text("Погасить")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtForSettle = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
