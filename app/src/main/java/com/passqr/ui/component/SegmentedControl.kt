package com.passqr.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.passqr.ui.theme.CoralPrimary
import com.passqr.ui.theme.OnPrimary

/**
 * DESIGN.md: "Top segmented control (Active: Coral background, Inactive: Transparent)."
 * Renders a pill-shaped segmented toggle with smooth colour transitions.
 */
@Composable
fun SegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) CoralPrimary else Color.Transparent,
                animationSpec = tween(250),
                label = "tabBgColor"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(250),
                label = "tabTextColor"
            )

            TextButton(
                onClick    = { onTabSelected(index) },
                modifier   = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text      = label,
                    color     = textColor,
                    style     = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
