package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.ui.theme.debtColors
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

@Composable
fun DebtItemCard(
    debt: Debt,
    onClick: () -> Unit,
    onQuickPaymentClick: () -> Unit,
    onQuickSettleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtColors = MaterialTheme.debtColors
    val accentColor = if (debt.type == DebtType.OWED_TO_ME) debtColors.owedToMe else debtColors.iOwe
    val deadlineInfo = DateUtils.calculateDeadlineInfo(debt.dueDateMillis)

    val progress = if (debt.totalAmount > 0) {
        ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isPartiallyPaid = progress > 0f && progress < 1f

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("debt_item_${debt.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Avatar, Name & Amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initial Avatar
                PersonAvatar(
                    name = debt.personName,
                    accentColor = accentColor,
                    modifier = Modifier.size(44.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Создано: ${DateUtils.formatShortDate(debt.createdDateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount Section
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )

                    if (isPartiallyPaid) {
                        Text(
                            text = "из ${CurrencyUtils.formatAmount(debt.totalAmount, debt.currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Progress bar if partially paid
            if (isPartiallyPaid) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Оплачено ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Осталось: ${CurrencyUtils.formatAmount(debt.remainingAmount, debt.currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f)
                    )
                }
            }

            // Comment / Note if provided
            if (debt.comment.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = debt.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Info & Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Deadline Chip (if specified)
                if (deadlineInfo != null) {
                    DeadlineChip(deadlineInfo)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onQuickPaymentClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_quick_pay_${debt.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Платеж", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onQuickSettleClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accentColor.copy(alpha = 0.15f),
                            contentColor = accentColor
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_quick_settle_${debt.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Закрыть", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Подробнее",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PersonAvatar(
    name: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
fun DeadlineChip(info: com.example.util.DeadlineInfo) {
    val chipBg = when {
        info.isOverdue -> MaterialTheme.colorScheme.errorContainer
        info.isUrgent -> Color(0xFFFEF3C7) // Amber/Yellow
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val chipFg = when {
        info.isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        info.isUrgent -> Color(0xFF92400E) // Dark amber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = chipBg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = chipFg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = info.text,
                style = MaterialTheme.typography.labelSmall,
                color = chipFg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
