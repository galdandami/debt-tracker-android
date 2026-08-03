package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DebtType
import com.example.ui.theme.debtColors
import com.example.util.CurrencyUtils

@Composable
fun SummaryCardsHeader(
    totalOwedToMe: Double,
    totalIOwe: Double,
    countOwedToMe: Int,
    countIOwe: Int,
    selectedTab: DebtType,
    onTabSelected: (DebtType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: "Мне должны" (Green)
        SummaryCard(
            title = "Мне должны",
            amount = totalOwedToMe,
            count = countOwedToMe,
            type = DebtType.OWED_TO_ME,
            isSelected = selectedTab == DebtType.OWED_TO_ME,
            onClick = { onTabSelected(DebtType.OWED_TO_ME) },
            modifier = Modifier.weight(1f)
        )

        // Card 2: "Я должен" (Red/Orange)
        SummaryCard(
            title = "Я должен",
            amount = totalIOwe,
            count = countIOwe,
            type = DebtType.I_OWE,
            isSelected = selectedTab == DebtType.I_OWE,
            onClick = { onTabSelected(DebtType.I_OWE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    count: Int,
    type: DebtType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtColors = MaterialTheme.debtColors
    val baseAccentColor = if (type == DebtType.OWED_TO_ME) debtColors.owedToMe else debtColors.iOwe
    val baseContainerColor = if (type == DebtType.OWED_TO_ME) debtColors.owedToMeContainer else debtColors.iOweContainer
    val onContainerColor = if (type == DebtType.OWED_TO_ME) debtColors.onOwedToMeContainer else debtColors.onIOweContainer

    val borderStrokeColor by animateColorAsState(
        targetValue = if (isSelected) baseAccentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "summaryCardBorder"
    )

    val containerBgColor by animateColorAsState(
        targetValue = if (isSelected) baseContainerColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "summaryCardBg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag(if (type == DebtType.OWED_TO_ME) "summary_card_owed_to_me" else "summary_card_i_owe"),
        color = containerBgColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(baseAccentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (type == DebtType.OWED_TO_ME) {
                            Icons.AutoMirrored.Filled.TrendingDown
                        } else {
                            Icons.AutoMirrored.Filled.TrendingUp
                        },
                        contentDescription = null,
                        tint = baseAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = baseAccentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$count ${getDebtWord(count)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = baseAccentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = CurrencyUtils.formatAmount(amount),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = baseAccentColor
            )
        }
    }
}

private fun getDebtWord(count: Int): String {
    val n = count % 100
    val n1 = n % 10
    if (n in 11..19) return "записей"
    if (n1 == 1) return "долг"
    if (n1 in 2..4) return "долга"
    return "долгов"
}
