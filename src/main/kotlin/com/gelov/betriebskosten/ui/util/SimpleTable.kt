package com.gelov.betriebskosten.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CommonSimpleTable(
    headers: List<String>,
    rows: List<List<String>>,
    columnWeights: List<Float>,
    actions: (@Composable (rowIndex: Int) -> Unit)? = null
) {
    val hasActions = actions != null

    if (rows.isNotEmpty()) {
        val baseCols = rows.first().size
        val expected = baseCols + if (hasActions) 1 else 0

        require(headers.size == expected && columnWeights.size == expected) {
            "Bei actions muss es eine zusätzliche Spalte für Aktionen geben. " +
                    "rows=$baseCols, headers=${headers.size}, columnWeights=${columnWeights.size}"
        }
    } else {
        // keine Zeilen → nur prüfen, dass headers und columnWeights zueinander passen
        require(headers.size == columnWeights.size) {
            "Headers und columnWeights müssen gleich lang sein, wenn keine Zeilen vorhanden sind. " +
                    "headers=${headers.size}, columnWeights=${columnWeights.size}"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                headers.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier.weight(columnWeights[index]),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            // --- Zeilen ---
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // alle Daten-Spalten (ohne Aktionen)
                    row.forEachIndexed { colIndex, cell ->
                        Box(
                            modifier = Modifier.weight(columnWeights[colIndex])
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Aktionen in der letzten Spalte (falls vorhanden)
                    actions?.let {
                        Box(
                            modifier = Modifier.weight(columnWeights.last()),
                        ) {
                            it(rowIndex)
                        }
                    }
                }

                HorizontalDivider()
            }
        }
    }
}
