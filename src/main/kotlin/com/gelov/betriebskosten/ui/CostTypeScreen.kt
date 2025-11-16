package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gelov.betriebskosten.data.KostenartenRepository
import com.gelov.betriebskosten.domain.Kostenart
import com.gelov.betriebskosten.ui.util.CommonSimpleTable
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun CostTypeScreen() {
    var kostenarten by remember { mutableStateOf<List<Kostenart>>(emptyList()) }

    var editingItem by remember { mutableStateOf<Kostenart?>(null) }
    var isNew by remember { mutableStateOf(false) }

    // Bestätigungsdialog fürs Löschen
    var deleteTarget by remember { mutableStateOf<Kostenart?>(null) }

    // generische Fehleranzeige
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun reloadKostenarten() {
        try {
            kostenarten = KostenartenRepository.getAll()
        } catch (e: Exception) {
            errorMessage =
                "Fehler beim Laden der Kostenarten: ${e.message ?: "Unbekannter Fehler"}"
            println("[CostTypeScreen] Fehler beim Laden: ${e.stackTraceToString()}")
            kostenarten = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        reloadKostenarten()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // zentrierter Inhalt wie bei EigentuemerScreen
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            ScreenTitle(
                title = "Kostenarten",
                trailing = {
                    Button(onClick = {
                        isNew = true
                        editingItem = Kostenart(
                            id = 0,
                            bezeichnung = "",
                            beschreibung = "",
                            summe = BigDecimal.ZERO,
                            createdAt = null,
                            updatedAt = null
                        )
                    }) {
                        Text("Neue Kostenart hinzufügen")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            KostenartenTable(
                kostenarten = kostenarten,
                onEdit = { k ->
                    isNew = false
                    editingItem = k
                },
                onDelete = { k ->
                    deleteTarget = k
                }
            )
        }
    }

    editingItem?.let { item ->
        KostenartEditDialog(
            initial = item,
            isNew = isNew,
            onDismiss = { editingItem = null },
            onConfirm = { bezeichnung, beschreibung, summe ->
                try {
                    if (isNew) {
                        KostenartenRepository.insert(bezeichnung, beschreibung, summe)
                    } else {
                        KostenartenRepository.update(item.id, bezeichnung, beschreibung, summe)
                    }
                    reloadKostenarten()
                    editingItem = null
                } catch (e: Exception) {
                    errorMessage =
                        "Fehler beim Speichern der Kostenart: ${e.message ?: "Unbekannter Fehler"}"
                    println("[CostTypeScreen] Fehler beim Speichern: ${e.stackTraceToString()}")
                }
            }
        )
    }

    // Lösch-Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Kostenart löschen") },
            text = {
                Text("Möchten Sie die Kostenart „${target.bezeichnung}“ wirklich löschen?")
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        KostenartenRepository.delete(target.id)
                        reloadKostenarten()
                    } catch (e: Exception) {
                        errorMessage =
                            "Fehler beim Löschen der Kostenart: ${e.message ?: "Unbekannter Fehler"}"
                        println("[CostTypeScreen] Fehler beim Löschen: ${e.stackTraceToString()}")
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
private fun KostenartenTable(
    kostenarten: List<Kostenart>,
    onEdit: (Kostenart) -> Unit,
    onDelete: (Kostenart) -> Unit
) {
    // Spaltenbreiten: Bezeichnung / Beschreibung / Summe / Aktionen
    val columnWeights = listOf(
        0.25f, // Bezeichnung
        0.40f, // Beschreibung
        0.15f, // Summe
        0.20f  // Aktionen → genug Platz für beide Buttons
    )

    CommonSimpleTable(
        headers = listOf("Bezeichnung", "Beschreibung", "Summe (€)", "Aktionen"),
        rows = kostenarten.map {
            listOf(
                it.bezeichnung,
                it.beschreibung.orEmpty(),
                it.summe.toEuro()
            )
        },
        columnWeights = columnWeights,
        actions = { rowIndex ->
            val k = kostenarten[rowIndex]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TableActionButton(
                    "Bearbeiten",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) { onEdit(k) }

                TableActionButton(
                    "Löschen",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { onDelete(k) }
            }
        }
    )
}

@Composable
private fun KostenartEditDialog(
    initial: Kostenart,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, BigDecimal) -> Unit
) {
    var bezeichnung by remember(initial) { mutableStateOf(initial.bezeichnung) }
    var beschreibung by remember(initial) { mutableStateOf(initial.beschreibung ?: "") }
    var summeText by remember(initial) {
        mutableStateOf(
            initial.summe
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
        )
    }

    // Validierung
    var bezeichnungError by remember { mutableStateOf<String?>(null) }
    var summeError by remember { mutableStateOf<String?>(null) }

    fun validate(): BigDecimal? {
        var ok = true

        bezeichnungError = if (bezeichnung.isBlank()) {
            ok = false
            "Bezeichnung darf nicht leer sein."
        } else null

        val parsedSum = summeText
            .trim()
            .replace(',', '.')
            .toBigDecimalOrNull()

        summeError = when {
            parsedSum == null -> {
                ok = false
                "Ungültiger Betrag."
            }
            parsedSum < BigDecimal.ZERO -> {
                ok = false
                "Betrag darf nicht negativ sein."
            }
            else -> null
        }

        return if (ok) parsedSum else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Neue Kostenart" else "Kostenart bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bezeichnung,
                    onValueChange = {
                        bezeichnung = it
                        if (bezeichnungError != null) bezeichnungError = null
                    },
                    label = { Text("Bezeichnung") },
                    singleLine = true,
                    isError = bezeichnungError != null,
                    supportingText = {
                        bezeichnungError?.let { err -> Text(err) }
                    }
                )
                OutlinedTextField(
                    value = beschreibung,
                    onValueChange = { beschreibung = it },
                    label = { Text("Beschreibung (optional)") }
                )
                OutlinedTextField(
                    value = summeText,
                    onValueChange = {
                        summeText = it
                        if (summeError != null) summeError = null
                    },
                    label = { Text("Summe (€)") },
                    singleLine = true,
                    isError = summeError != null,
                    supportingText = {
                        summeError?.let { err -> Text(err) }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = validate()
                if (parsed != null) {
                    onConfirm(
                        bezeichnung.trim(),
                        beschreibung.ifBlank { null },
                        parsed
                    )
                }
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/**
 * Kleiner Helfer wie in PdfService: BigDecimal -> "123,45 €"
 */
private fun BigDecimal.toEuro(): String =
    this.setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
        .replace('.', ',') + " €"
