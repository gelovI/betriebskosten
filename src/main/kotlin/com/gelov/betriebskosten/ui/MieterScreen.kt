package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gelov.betriebskosten.data.MieterRepository
import com.gelov.betriebskosten.domain.Mieter
import com.gelov.betriebskosten.ui.util.CommonSimpleTable

@Composable
fun MieterScreen() {
    var mieter by remember { mutableStateOf<List<Mieter>>(emptyList()) }

    var editing by remember { mutableStateOf<Mieter?>(null) }
    var isNew by remember { mutableStateOf(false) }

    // Bestätigungsdialog für Löschen
    var deleteTarget by remember { mutableStateOf<Mieter?>(null) }

    // generische Fehleranzeige
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun reloadMieter() {
        try {
            mieter = MieterRepository.getAll()
        } catch (e: Exception) {
            errorMessage = "Fehler beim Laden der Mieter: ${e.message ?: "Unbekannter Fehler"}"
            println("[MieterScreen] Fehler beim Laden: ${e.stackTraceToString()}")
            mieter = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        reloadMieter()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // zentrierter Content-Block
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            ScreenTitle(
                title = "Mieter",
                trailing = {
                    Button(onClick = {
                        isNew = true
                        editing = Mieter(
                            id = 0L,
                            name = "",
                            email = "",
                            phone = ""
                        )
                    }) {
                        Text("Neuen Mieter hinzufügen")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            MieterTable(
                mieter = mieter,
                onEdit = { m ->
                    isNew = false
                    editing = m
                },
                onDelete = { m ->
                    // zuerst bestätigen lassen
                    deleteTarget = m
                }
            )
        }
    }

    // Dialog zum Anlegen / Bearbeiten
    editing?.let { current ->
        MieterEditDialog(
            initial = current,
            isNew = isNew,
            onDismiss = { editing = null },
            onConfirm = { name, email, phone ->
                try {
                    if (isNew) {
                        MieterRepository.insert(
                            name = name,
                            email = email,
                            phone = phone
                        )
                    } else {
                        MieterRepository.update(
                            id = current.id,
                            name = name,
                            email = email,
                            phone = phone
                        )
                    }
                    reloadMieter()
                    editing = null
                } catch (e: Exception) {
                    errorMessage =
                        "Fehler beim Speichern des Mieters: ${e.message ?: "Unbekannter Fehler"}"
                    println("[MieterScreen] Fehler beim Speichern: ${e.stackTraceToString()}")
                }
            }
        )
    }

    // Bestätigungsdialog fürs Löschen
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Mieter löschen") },
            text = {
                Text("Möchten Sie den Mieter „${target.name}“ wirklich löschen?")
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        MieterRepository.delete(target.id)
                        reloadMieter()
                    } catch (e: Exception) {
                        errorMessage =
                            "Fehler beim Löschen des Mieters: ${e.message ?: "Unbekannter Fehler"}"
                        println("[MieterScreen] Fehler beim Löschen: ${e.stackTraceToString()}")
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
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun MieterTable(
    mieter: List<Mieter>,
    onEdit: (Mieter) -> Unit,
    onDelete: (Mieter) -> Unit
) {
    // Spaltenbreiten: Name / Email / Telefon / Aktionen
    val columnWeights = listOf(
        0.35f, // Name
        0.20f, // Email
        0.25f, // Telefon
        0.20f  // Aktionen
    )

    CommonSimpleTable(
        headers = listOf("Name", "E-Mail", "Telefon", "Aktionen"),
        rows = mieter.map {
            listOf(
                it.name,
                it.email.orEmpty(),
                it.phone.orEmpty()
            )
        },
        columnWeights = columnWeights,
        actions = { index ->
            val m = mieter[index]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TableActionButton(
                    "Bearbeiten",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) { onEdit(m) }

                TableActionButton(
                    "Löschen",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { onDelete(m) }
            }
        }
    )
}

@Composable
private fun MieterEditDialog(
    initial: Mieter,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var email by remember(initial) { mutableStateOf(initial.email.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial.phone.orEmpty()) }

    // einfache Validierung
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var ok = true

        nameError = if (name.isBlank()) {
            ok = false
            "Name darf nicht leer sein."
        } else null

        // sehr einfache Email-Prüfung, nur damit offensichtlicher Mist abgefangen wird
        emailError = if (email.isNotBlank() && !email.contains("@")) {
            ok = false
            "Ungültige E-Mail-Adresse."
        } else null

        return ok
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Neuer Mieter" else "Mieter bearbeiten") },
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
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = { Text("E-Mail (optional)") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let { err -> Text(err) }
                    }
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validate()) {
                    onConfirm(
                        name.trim(),
                        email.trim().ifBlank { null },
                        phone.trim().ifBlank { null }
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
fun TenantScreen() = MieterScreen()
