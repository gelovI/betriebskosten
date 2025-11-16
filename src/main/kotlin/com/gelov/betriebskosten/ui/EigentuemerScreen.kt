package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gelov.betriebskosten.data.EigentuemerRepository
import com.gelov.betriebskosten.domain.Eigentuemer
import com.gelov.betriebskosten.ui.util.CommonSimpleTable

@Composable
fun EigentuemerScreen() {
    var eigentuemer by remember { mutableStateOf<List<Eigentuemer>>(emptyList()) }

    var editing by remember { mutableStateOf<Eigentuemer?>(null) }
    var isNew by remember { mutableStateOf(false) }

    // Bestätigungsdialog für Löschen
    var deleteTarget by remember { mutableStateOf<Eigentuemer?>(null) }

    // generischer Fehlertext
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun reloadEigentuemer() {
        try {
            eigentuemer = EigentuemerRepository.getAll()
        } catch (e: Exception) {
            errorMessage =
                "Fehler beim Laden der Eigentümer: ${e.message ?: "Unbekannter Fehler"}"
            println("[EigentuemerScreen] Fehler beim Laden: ${e.stackTraceToString()}")
            eigentuemer = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        reloadEigentuemer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Innerer Content-Block, der zentriert wird
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            ScreenTitle(
                title = "Eigentümer",
                trailing = {
                    Button(onClick = {
                        isNew = true
                        editing = Eigentuemer(
                            id = 0L,
                            grundstueck = null,
                            name = "",
                            abrechnungsperiode = ""
                        )
                    }) {
                        Text("Neuen Eigentümer hinzufügen")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            EigentumerTable(
                eigentuemer = eigentuemer,
                onEdit = { e ->
                    isNew = false
                    editing = e
                },
                onDelete = { e ->
                    // erst bestätigen
                    deleteTarget = e
                }
            )
        }
    }

    // Dialog zum Anlegen / Bearbeiten
    editing?.let { current ->
        EigentuemerEditDialog(
            initial = current,
            isNew = isNew,
            onDismiss = { editing = null },
            onConfirm = { grundstueck, name, periode ->
                try {
                    if (isNew) {
                        EigentuemerRepository.insert(grundstueck, name, periode)
                    } else {
                        EigentuemerRepository.update(
                            id = current.id,
                            grundstueck = grundstueck,
                            name = name,
                            abrechnungsperiode = periode
                        )
                    }
                    reloadEigentuemer()
                    editing = null
                } catch (e: Exception) {
                    errorMessage =
                        "Fehler beim Speichern des Eigentümers: ${e.message ?: "Unbekannter Fehler"}"
                    println("[EigentuemerScreen] Fehler beim Speichern: ${e.stackTraceToString()}")
                }
            }
        )
    }

    // Lösch-Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eigentümer löschen") },
            text = {
                Text(
                    "Möchten Sie den Eigentümer „${target.name}“ wirklich löschen?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        EigentuemerRepository.delete(target.id)
                        reloadEigentuemer()
                    } catch (e: Exception) {
                        errorMessage =
                            "Fehler beim Löschen des Eigentümers: ${e.message ?: "Unbekannter Fehler"}"
                        println("[EigentuemerScreen] Fehler beim Löschen: ${e.stackTraceToString()}")
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
private fun EigentumerTable(
    eigentuemer: List<Eigentuemer>,
    onEdit: (Eigentuemer) -> Unit,
    onDelete: (Eigentuemer) -> Unit
) {
    // Spalten-Weights: Name / Grundstück / Periode / Aktionen
    val columnWeights = listOf(0.30f, 0.35f, 0.20f, 0.20f)

    CommonSimpleTable(
        headers = listOf("Name", "Grundstück", "Abrechnungsperiode", "Aktionen"),
        rows = eigentuemer.map {
            listOf(
                it.name,
                it.grundstueck.orEmpty(),
                it.abrechnungsperiode
            )
        },
        columnWeights = columnWeights,
        actions = { index ->
            val e = eigentuemer[index]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TableActionButton(
                    "Bearbeiten",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) { onEdit(e) }

                TableActionButton(
                    "Löschen",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { onDelete(e) }
            }
        }
    )
}

@Composable
private fun EigentuemerEditDialog(
    initial: Eigentuemer,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?, String, String) -> Unit
) {
    var grundstueck by remember(initial) { mutableStateOf(initial.grundstueck.orEmpty()) }
    var name by remember(initial) { mutableStateOf(initial.name) }
    var periode by remember(initial) { mutableStateOf(initial.abrechnungsperiode) }

    // einfache Validierung
    var nameError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var ok = true

        nameError = if (name.isBlank()) {
            ok = false
            "Name darf nicht leer sein."
        } else null

        // Abrechnungsperiode darf leer sein (PdfService hat Fallback),
        // deshalb hier keine harte Validierung.
        return ok
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Neuer Eigentümer" else "Eigentümer bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError != null) nameError = null
                    },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = {
                        nameError?.let { err -> Text(err) }
                    }
                )
                OutlinedTextField(
                    value = grundstueck,
                    onValueChange = { grundstueck = it },
                    label = { Text("Grundstück (optional)") }
                )
                OutlinedTextField(
                    value = periode,
                    onValueChange = { periode = it },
                    label = { Text("Abrechnungsperiode (optional)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validate()) {
                    onConfirm(
                        grundstueck.ifBlank { null },
                        name.trim(),
                        periode.trim()
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
