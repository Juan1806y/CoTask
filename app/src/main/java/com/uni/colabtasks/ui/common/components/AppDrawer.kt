package com.uni.colabtasks.ui.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uni.colabtasks.R
import com.uni.colabtasks.ui.navigation.DrawerSection

@Composable
fun AppDrawerContent(
    current: DrawerSection,
    onSelect: (DrawerSection) -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp).fillMaxHeight()) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
        )
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            DrawerEntry(
                label = stringResource(R.string.menu_lists),
                icon = Icons.Outlined.Checklist,
                selected = current == DrawerSection.LISTS,
                onClick = { onSelect(DrawerSection.LISTS) }
            )
            DrawerEntry(
                label = stringResource(R.string.menu_calendar),
                icon = Icons.Outlined.CalendarMonth,
                selected = current == DrawerSection.CALENDAR,
                onClick = { onSelect(DrawerSection.CALENDAR) }
            )
            DrawerEntry(
                label = stringResource(R.string.menu_stats),
                icon = Icons.Outlined.BarChart,
                selected = current == DrawerSection.STATS,
                onClick = { onSelect(DrawerSection.STATS) }
            )
            DrawerEntry(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Outlined.Settings,
                selected = current == DrawerSection.SETTINGS,
                onClick = { onSelect(DrawerSection.SETTINGS) }
            )
        }
    }
}

@Composable
private fun DrawerEntry(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
