package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gelov.betriebskosten.data.*
import java.time.LocalDate

@Composable
fun DashboardScreen() {
    // Stammdaten holen
    val eigentuemer = remember { EigentuemerRepository.getAll() }
    val mieter = remember { MieterRepository.getAll() }
    val wohnungen = remember { WohnungenRepository.getAll() }
    val kostenarten = remember { KostenartenRepository.getAll() }
    val archivedFiles = remember { PdfService.listArchivedPdfs() }

    val lastYearFromArchive = remember(archivedFiles) {
        archivedFiles
            .mapNotNull { file ->
                val parts = file.nameWithoutExtension.split("_")
                parts.getOrNull(1)?.toIntOrNull()
            }
            .maxOrNull()
    }

    val gesamtFlaeche = remember(wohnungen) {
        wohnungen.sumOf { it.wohnflaecheQm }
    }

    // Datenmodell für alle Boxen
    val cards = listOf(
        DashboardCardData(
            title = "Eigentümer",
            primary = eigentuemer.size.toString(),
            secondary = "Eintrag${if (eigentuemer.size == 1) "" else "e"} im System"
        ),
        DashboardCardData(
            title = "Mieter",
            primary = mieter.size.toString(),
            secondary = "aktive Mieter verwaltet"
        ),
        DashboardCardData(
            title = "Wohnungen",
            primary = wohnungen.size.toString(),
            secondary = "Wohn­einheiten erfasst"
        ),
        DashboardCardData(
            title = "Kostenarten",
            primary = kostenarten.size.toString(),
            secondary = "Positionen für die Abrechnung"
        ),
        DashboardCardData(
            title = "Archivierte Abrechnungen",
            primary = archivedFiles.size.toString(),
            secondary = if (lastYearFromArchive != null)
                "letzte Abrechnung: $lastYearFromArchive"
            else
                "bisher keine Abrechnungen gespeichert"
        ),
        DashboardCardData(
            title = "Gesamtwohnfläche",
            primary = "$gesamtFlaeche m²",
            secondary = "Summe aller Wohnungen"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            ScreenTitle(
                title = "Dashboard",
                trailing = {
                    Text(
                        text = "Stand: ${LocalDate.now()}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )

            Spacer(Modifier.height(24.dp))

            DashboardGrid(cards = cards, columns = 3)
        }
    }
}

private data class DashboardCardData(
    val title: String,
    val primary: String,
    val secondary: String
)

@Composable
private fun DashboardGrid(
    cards: List<DashboardCardData>,
    columns: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        cards.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // echte Karten
                rowItems.forEach { data ->
                    DashboardCard(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 110.dp),
                        title = data.title,
                        primaryValue = data.primary,
                        secondary = data.secondary
                    )
                }
                // leere Spacers, damit auch die letzte Zeile volle Breite hat
                repeat(columns - rowItems.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 110.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    primaryValue: String,
    secondary: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
