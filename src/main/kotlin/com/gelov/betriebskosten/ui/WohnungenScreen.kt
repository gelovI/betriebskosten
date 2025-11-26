package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gelov.betriebskosten.data.WohnungenRepository
import com.gelov.betriebskosten.data.MieterRepository
import com.gelov.betriebskosten.domain.Wohnung
import com.gelov.betriebskosten.domain.Mieter
import com.gelov.betriebskosten.ui.util.CommonSimpleTable
import java.math.BigDecimal

@Composable
fun WohnungenScreen() {
    var wohnungen by remember { mutableStateOf<List<Wohnung>>(emptyList()) }
    var mieterListe by remember { mutableStateOf<List<Mieter>>(emptyList()) }

    // Aktuell bearbeitete Wohnung (Dialog)
    var editing by remember { mutableStateOf<Wohnung?>(null) }
    var isNew by remember { mutableStateOf(false) }

    // Bestätigungsdialog für Löschen
    var deleteTarget by remember { mutableStateOf<Wohnung?>(null) }

    // Einfache Fehleranzeige (z.B. DB-Fehler)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun reloadWohnungen() {
        try {
            wohnungen = WohnungenRepository.getAll()
        } catch (e: Exception) {
            errorMessage = "Fehler beim Laden der Wohnungen: ${e.message ?: "Unbekannter Fehler"}"
            println("[WohnungenScreen] Fehler beim Laden: ${e.stackTraceToString()}")
            wohnungen = emptyList()
        }
    }

    fun reloadMieter() {
        try {
            mieterListe = MieterRepository.getAll()
        } catch (e: Exception) {
            errorMessage = "Fehler beim Laden der Mieter: ${e.message ?: "Unbekannter Fehler"}"
            println("[WohnungenScreen] Fehler beim Laden der Mieter: ${e.stackTraceToString()}")
            mieterListe = emptyList()
        }
    }

    // Beim ersten Anzeigen Daten laden
    LaunchedEffect(Unit) {
        reloadWohnungen()
        reloadMieter()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // zentrierter Content-Block wie bei den anderen Screens
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            ScreenTitle(
                title = "Wohnungen",
                trailing = {
                    Button(onClick = {
                        isNew = true
                        editing = Wohnung(
                            id = 0L,
                            adresse = "",
                            wohnflaecheQm = 0,
                            vorauszahlung = null,
                            aktuellerMieterId = null
                        )
                    }) {
                        Text("Neue Wohnung hinzufügen")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            WohnungenTable(
                wohnungen = wohnungen,
                mieterById = mieterListe.associateBy { it.id },
                onEdit = { w ->
                    isNew = false
                    editing = w
                },
                onDelete = { w ->
                    // Nicht direkt löschen, sondern zuerst bestätigen lassen
                    deleteTarget = w
                }
            )
        }
    }

    // Dialog zum Bearbeiten / Anlegen
    editing?.let { current ->
        WohnungenEditDialog(
            initial = current,
            isNew = isNew,
            mieterListe = mieterListe,
            onDismiss = { editing = null },
            onConfirm = { adresse, qm, voraus, mieterId ->
                try {
                    val qmInt = qm.toIntOrNull() ?: 0
                    val qmClamped = qmInt.coerceAtLeast(0)

                    val vorausDecimal: BigDecimal? = if (voraus.isNotBlank()) {
                        val normalized = voraus.replace(',', '.')
                        normalized.toBigDecimalOrNull()?.let {
                            if (it < BigDecimal.ZERO) BigDecimal.ZERO else it
                        }
                    } else {
                        null
                    }

                    val mieterIdLong = mieterId
                        .takeIf { it.isNotBlank() }
                        ?.toLongOrNull()

                    if (isNew) {
                        WohnungenRepository.insert(
                            adresse = adresse,
                            wohnflaeche = qmClamped,
                            vorauszahlung = vorausDecimal,
                            aktuellerMieterId = mieterIdLong
                        )
                    } else {
                        WohnungenRepository.update(
                            id = current.id,
                            adresse = adresse,
                            wohnflaeche = qmClamped,
                            vorauszahlung = vorausDecimal,
                            aktuellerMieterId = mieterIdLong
                        )
                    }
                    reloadWohnungen()
                    editing = null
                } catch (e: Exception) {
                    errorMessage = "Fehler beim Speichern der Wohnung: ${e.message ?: "Unbekannter Fehler"}"
                    println("[WohnungenScreen] Fehler beim Speichern: ${e.stackTraceToString()}")
                }
            }
        )
    }

    // Bestätigungsdialog fürs Löschen
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Wohnung löschen") },
            text = {
                Text(
                    "Möchten Sie die Wohnung „${target.adresse}“ wirklich löschen?\n" +
                            "Achtung: Abhängige Daten (z. B. Abrechnungen) könnten betroffen sein."
                )
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        WohnungenRepository.delete(target.id)
                        reloadWohnungen()
                    } catch (e: Exception) {
                        errorMessage = "Fehler beim Löschen der Wohnung: ${e.message ?: "Unbekannter Fehler"}"
                        println("[WohnungenScreen] Fehler beim Löschen: ${e.stackTraceToString()}")
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

    // Generischer Fehlerdialog
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
private fun WohnungenTable(
    wohnungen: List<Wohnung>,
    mieterById: Map<Long, Mieter>,
    onEdit: (Wohnung) -> Unit,
    onDelete: (Wohnung) -> Unit
) {
    // Spaltenbreiten: Adresse / Fläche / Vorauszahlung / Mieter / Aktionen
    val columnWeights = listOf(
        0.15f, // Adresse
        0.15f, // Fläche
        0.20f, // Vorauszahlung
        0.30f, // Mieter (Name)
        0.20f  // Aktionen
    )

    CommonSimpleTable(
        headers = listOf(
            "Adresse",
            "Fläche (m²)",
            "Vorauszahlung (€)",
            "Mieter",
            "Aktionen"
        ),
        rows = wohnungen.map { w ->
            val mieterName = w.aktuellerMieterId?.let { id ->
                mieterById[id]?.let { mieterLabel(it) } ?: "ID $id"
            }.orEmpty()

            listOf(
                w.adresse,
                w.wohnflaecheQm.toString(),
                w.vorauszahlung?.toEuro().orEmpty(),
                mieterName
            )
        },
        columnWeights = columnWeights,
        actions = { index ->
            val w = wohnungen[index]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TableActionButton(
                    "Bearbeiten",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) { onEdit(w) }

                TableActionButton(
                    "Löschen",
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { onDelete(w) }
            }
        }
    )
}

private fun mieterLabel(m: Mieter): String = m.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WohnungenEditDialog(
    initial: Wohnung,
    isNew: Boolean,
    mieterListe: List<Mieter>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var adresse by remember(initial) { mutableStateOf(initial.adresse) }
    var qm by remember(initial) { mutableStateOf(initial.wohnflaecheQm.toString()) }
    var voraus by remember(initial) { mutableStateOf(initial.vorauszahlung?.toPlainString() ?: "") }
    var mieterId by remember(initial) { mutableStateOf(initial.aktuellerMieterId?.toString() ?: "") }

    // Validierungs-Fehler
    var adresseError by remember { mutableStateOf<String?>(null) }
    var qmError by remember { mutableStateOf<String?>(null) }
    var vorausError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var ok = true

        adresseError = if (adresse.isBlank()) {
            ok = false
            "Adresse darf nicht leer sein."
        } else null

        qmError = when {
            qm.isBlank() -> {
                ok = false
                "Wohnfläche angeben."
            }
            qm.toIntOrNull() == null -> {
                ok = false
                "Ungültige Zahl."
            }
            qm.toInt() < 0 -> {
                ok = false
                "Wohnfläche darf nicht negativ sein."
            }
            else -> null
        }

        vorausError = if (voraus.isNotBlank()) {
            val normalized = voraus.replace(',', '.')
            val dec = normalized.toBigDecimalOrNull()
            when {
                dec == null -> {
                    ok = false
                    "Ungültiger Betrag."
                }
                dec < BigDecimal.ZERO -> {
                    ok = false
                    "Betrag darf nicht negativ sein."
                }
                else -> null
            }
        } else null

        // mieterId stammt aus Dropdown → immer gültig (oder leer)
        return ok
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Neue Wohnung" else "Wohnung bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = adresse,
                    onValueChange = {
                        adresse = it
                        if (adresseError != null) adresseError = null
                    },
                    isError = adresseError != null,
                    label = { Text("Adresse") },
                    supportingText = {
                        adresseError?.let { err -> Text(err) }
                    }
                )
                OutlinedTextField(
                    value = qm,
                    onValueChange = {
                        qm = it
                        if (qmError != null) qmError = null
                    },
                    isError = qmError != null,
                    label = { Text("Wohnfläche (m²)") },
                    singleLine = true,
                    supportingText = {
                        qmError?.let { err -> Text(err) }
                    }
                )
                OutlinedTextField(
                    value = voraus,
                    onValueChange = {
                        voraus = it
                        if (vorausError != null) vorausError = null
                    },
                    isError = vorausError != null,
                    label = { Text("Vorauszahlung (€)") },
                    singleLine = true,
                    supportingText = {
                        vorausError?.let { err -> Text(err) }
                    }
                )

                // --- Mieter-Auswahl (Dropdown) ---
                var expanded by remember { mutableStateOf(false) }
                val selectedMieter = mieterListe.find { it.id.toString() == mieterId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedMieter?.let { "${mieterLabel(it)} (ID ${it.id})" }
                            ?: "Kein Mieter zugewiesen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mieter auswählen") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Kein Mieter zugewiesen") },
                            onClick = {
                                mieterId = ""
                                expanded = false
                            }
                        )

                        mieterListe.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(mieterLabel(m)) },
                                onClick = {
                                    mieterId = m.id.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validate()) {
                    onConfirm(
                        adresse.trim(),
                        qm.trim(),
                        voraus.trim(),
                        mieterId.trim()
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}