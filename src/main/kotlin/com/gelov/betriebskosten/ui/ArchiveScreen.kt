package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gelov.betriebskosten.data.PdfService
import com.gelov.betriebskosten.ui.util.CommonSimpleTable
import java.io.File

@Composable
fun ArchiveScreen() {
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    // Fehler-Dialog
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Bestätigungsdialog fürs Löschen
    var deleteTarget by remember { mutableStateOf<File?>(null) }

    fun reload() {
        try {
            files = PdfService.listArchivedPdfs()
        } catch (e: Exception) {
            errorMessage =
                "Fehler beim Laden der archivierten PDFs: ${e.message ?: "Unbekannter Fehler"}"
            println("[ArchiveScreen] Fehler beim Laden: ${e.stackTraceToString()}")
            files = emptyList()
        }
    }

    // Beim ersten Anzeigen laden
    LaunchedEffect(Unit) {
        reload()
    }

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
                title = "Archiv",
                trailing = {
                    TextButton(onClick = { reload() }) {
                        Text("Aktualisieren")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            if (files.isEmpty()) {
                Text(
                    text = "Noch keine Abrechnungen gespeichert.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ArchiveTable(
                    files = files,
                    onOpen = { file ->
                        try {
                            PdfService.openPdf(file)
                        } catch (e: Exception) {
                            errorMessage =
                                "Fehler beim Öffnen der PDF-Datei: ${e.message ?: "Unbekannter Fehler"}"
                            println("[ArchiveScreen] Fehler beim Öffnen: ${e.stackTraceToString()}")
                        }
                    },
                    onDelete = { file ->
                        // erst bestätigen lassen
                        deleteTarget = file
                    }
                )
            }
        }
    }

    // Löschbestätigung
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("PDF löschen") },
            text = { Text("Möchten Sie die Datei „${target.name}“ wirklich löschen?") },
            confirmButton = {
                Button(onClick = {
                    try {
                        PdfService.deletePdf(target)
                        reload()
                    } catch (e: Exception) {
                        errorMessage =
                            "Fehler beim Löschen der PDF-Datei: ${e.message ?: "Unbekannter Fehler"}"
                        println("[ArchiveScreen] Fehler beim Löschen: ${e.stackTraceToString()}")
                    } finally {
                        deleteTarget = null
                    }
                }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Fehler-Dialog
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Fehler") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ArchiveTable(
    files: List<File>,
    onOpen: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    val columnWeights = listOf(0.70f, 0.30f)
    CommonSimpleTable(
        headers = listOf("Abrechnungsjahr / Datei", "Aktionen"),
        rows = files.map { listOf(it.nameWithoutExtension) },
        columnWeights = columnWeights,
        actions = { index ->
            val f = files[index]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TableActionButton(
                    "Öffnen",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { onOpen(f) }

                TableActionButton(
                    "Löschen",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { onDelete(f) }
            }
        }
    )
}
