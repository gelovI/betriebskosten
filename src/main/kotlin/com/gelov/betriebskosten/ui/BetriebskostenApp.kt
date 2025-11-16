package com.gelov.betriebskosten.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import kotlin.system.exitProcess

@Composable
fun BetriebskostenApp() {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)
    var currentScreen by remember { mutableStateOf(MainScreen.Dashboard) }

    Window(
        onCloseRequest = { exitProcess(0) },
        title = "Betriebskosten",
        state = windowState
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = MaterialTheme.colorScheme.primary,
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Row {
                    NavigationPanel(
                        current = currentScreen,
                        onSelect = { currentScreen = it }
                    )
                    HorizontalDivider(modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp))
                    ScreenHost(
                        modifier = Modifier.weight(1f),
                        screen = currentScreen
                    )
                }
            }
        }
    }
}

enum class MainScreen(
    val label: String
) {
    Dashboard("Dashboard"),
    Owners("Eigentümer"),
    Tenants("Mieter"),
    Apartments("Wohnungen"),
    CostTypes("Kostenarten"),
    Settlement("Wohnungskosten"),
    Archive("Archiv")
}

@Composable
private fun NavigationPanel(
    current: MainScreen,
    onSelect: (MainScreen) -> Unit
) {
    val navItems = MainScreen.values().toList()
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Navigation",
            modifier = Modifier
                .padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        navItems.forEach { item ->
            val selected = item == current
            NavigationItem(
                label = item.label,
                selected = selected,
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg =
        if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.primary

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bg)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ScreenHost(
    modifier: Modifier = Modifier,
    screen: MainScreen
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        when (screen) {
            MainScreen.Dashboard  -> DashboardScreen()
            MainScreen.Owners     -> EigentuemerScreen()
            MainScreen.Tenants    -> TenantScreen()
            MainScreen.Apartments -> WohnungenScreen()
            MainScreen.CostTypes  -> CostTypeScreen()
            MainScreen.Settlement -> SettlementScreen()
            MainScreen.Archive    -> ArchiveScreen()
        }
    }
}
