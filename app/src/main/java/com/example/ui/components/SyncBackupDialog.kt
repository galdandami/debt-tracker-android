package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SyncState
import com.example.ui.viewmodel.DebtViewModel
import com.example.util.DateUtils

@Composable
fun SyncBackupDialog(
    viewModel: DebtViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val userSession by viewModel.userSession.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showLocalBackupSection by remember { mutableStateOf(false) }

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var jsonText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = {
                showAuthDialog = false
                Toast.makeText(context, "Успешный вход! Выполняется синхронизация...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Облачная синхронизация",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ACCOUNT PROFILE CARD
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userSession.isLoggedIn) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userSession.isLoggedIn) Icons.Default.Person else Icons.Default.PersonOutline,
                                contentDescription = null,
                                tint = if (userSession.isLoggedIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (userSession.isLoggedIn) {
                                Text(
                                    text = userSession.displayName.ifBlank { "Пользователь" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userSession.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Гостевой режим",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Войдите для облачной синхронизации",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (userSession.isLoggedIn) {
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.testTag("btn_logout")
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = "Выйти из аккаунта",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Button(
                                onClick = { showAuthDialog = true },
                                modifier = Modifier.testTag("btn_open_auth_dialog"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Войти", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // CLOUD SYNC CONTROL SECTION
                if (userSession.isLoggedIn) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Status Indicator Box
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (syncState) {
                                    is SyncState.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    is SyncState.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (val state = syncState) {
                                    is SyncState.Syncing -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Синхронизация с облаком...",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    is SyncState.Success -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Облачная база синхронизирована",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Время: ${DateUtils.formatDateTime(state.timestampMillis)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    is SyncState.Error -> {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    SyncState.Idle -> {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val lastSync = userSession.lastSyncTimeMillis
                                        Text(
                                            text = if (lastSync != null) "Последняя синхронизация: ${DateUtils.formatDateTime(lastSync)}"
                                                   else "Облачная база готова к первой синхронизации",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Trigger Cloud Sync Button
                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            enabled = syncState !is SyncState.Syncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_trigger_cloud_sync"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Синхронизировать с облаком")
                        }

                        // Auto-sync switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Авто-синхронизация при изменениях",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = userSession.autoSyncEnabled,
                                onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                                modifier = Modifier.testTag("switch_auto_sync")
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 2.dp))

                // OPTIONAL LOCAL JSON BACKUP EXPANDER
                TextButton(
                    onClick = { showLocalBackupSection = !showLocalBackupSection },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_toggle_local_backup")
                ) {
                    Icon(
                        imageVector = if (showLocalBackupSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Локальный импорт/экспорт (JSON file)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                AnimatedVisibility(visible = showLocalBackupSection) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isExporting && !isImporting) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.exportDataToJson { exportedJson ->
                                            jsonText = exportedJson
                                            isExporting = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_export_backup"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Экспорт JSON", style = MaterialTheme.typography.bodySmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        jsonText = ""
                                        isImporting = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_import_backup"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Импорт JSON", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (isExporting) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("JSON Бэкапа:", style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = jsonText,
                                    onValueChange = {},
                                    readOnly = true,
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(jsonText))
                                        Toast.makeText(context, "Скопировано в буфер обмена!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_copy_backup_json"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Скопировать JSON")
                                }
                            }
                        }

                        if (isImporting) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Вставьте JSON данные:", style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = jsonText,
                                    onValueChange = {
                                        jsonText = it
                                        importStatusMessage = null
                                    },
                                    placeholder = { Text("Вставьте JSON...") },
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .testTag("input_import_json")
                                )

                                importStatusMessage?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }

                                Button(
                                    onClick = {
                                        if (jsonText.isBlank()) {
                                            importStatusMessage = "Вставьте данные JSON"
                                            return@Button
                                        }
                                        viewModel.importDataFromJson(jsonText) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Импортировано успешно!", Toast.LENGTH_SHORT).show()
                                                isImporting = false
                                            } else {
                                                importStatusMessage = "Неверный формат JSON"
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_confirm_import"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Загрузить в локальную базу")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_sync_dialog")
            ) {
                Text("Закрыть")
            }
        }
    )
}
