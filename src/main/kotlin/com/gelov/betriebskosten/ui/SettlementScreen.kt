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
import com.gelov.betriebskosten.domain.Abrechnung
import com.gelov.betriebskosten.domain.AbrechnungErgebnis
import com.gelov.betriebskosten.domain.VorauszahlungsPeriode
import com.gelov.betriebskosten.domain.Wohnung
import java.math.BigDecimal
import java.time.YearMonth

private enum class VorauszahlungsModus { STANDARD, PERIODEN }

@Composable
fun SettlementScreen() {
    var year by remember { mutableIntStateOf(2024) }

    // einfache Fehleranzeige für alles im Screen
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Stammdaten einmal laden (hier synchron – Desktop + kleine DB → ok)
    // Wenn du willst, kann man das später auch mit LaunchedEffect asynchron machen.
    val wohnungen = remember {
        try {
            WohnungenRepository.getAll()
        } catch (e: Exception) {
            println("[SettlementScreen] Fehler beim Laden der Wohnungen: ${e.stackTraceToString()}")
            emptyList()
        }
    }
    val mieter = remember {
        try {
            MieterRepository.getAll()
        } catch (e: Exception) {
            println("[SettlementScreen] Fehler beim Laden der Mieter: ${e.stackTraceToString()}")
            emptyList()
        }
    }
    val kostenarten = remember {
        try {
            KostenartenRepository.getAll()
        } catch (e: Exception) {
            println("[SettlementScreen] Fehler beim Laden der Kostenarten: ${e.stackTraceToString()}")
            emptyList()
        }
    }
    val eigentuemer = remember {
        try {
            EigentuemerRepository.getAll().firstOrNull()
        } catch (e: Exception) {
            println("[SettlementScreen] Fehler beim Laden des Eigentümers: ${e.stackTraceToString()}")
            null
        }
    }

    // Monate je Wohnung
    var monateMap by remember {
        mutableStateOf(
            wohnungen.associate { it.id to 12 }.toMutableMap()
        )
    }

    // Vorauszahlungs-Modus je Wohnung
    var modusByWohnungId by remember {
        mutableStateOf(
            wohnungen.associate { it.id to VorauszahlungsModus.STANDARD }.toMutableMap()
        )
    }

    // Perioden je Wohnung (nur relevant, wenn Modus = PERIODEN)
    var periodenByWohnungId by remember {
        mutableStateOf(
            wohnungen.associate { w ->
                w.id to try {
                    VorauszahlungsperiodenRepository.getForWohnungUndJahr(w.id, year)
                } catch (e: Exception) {
                    println("[SettlementScreen] Fehler beim Laden der Vorauszahlungen für Wohnung ${w.id}: ${e.stackTraceToString()}")
                    emptyList()
                }
            }.toMutableMap()
        )
    }

    // Helper, um Perioden bei Jahrwechsel neu zu laden
    fun reloadPeriodenForYear(targetYear: Int) {
        try {
            periodenByWohnungId = wohnungen.associate { w ->
                w.id to VorauszahlungsperiodenRepository.getForWohnungUndJahr(w.id, targetYear)
            }.toMutableMap()
        } catch (e: Exception) {
            errorMessage =
                "Fehler beim Laden der Vorauszahlungen für das Jahr $targetYear: ${e.message ?: "Unbekannter Fehler"}"
            println("[SettlementScreen] Fehler beim Reload der Perioden: ${e.stackTraceToString()}")
            periodenByWohnungId = wohnungen.associate { it.id to emptyList<VorauszahlungsPeriode>() }
                .toMutableMap()
        }
    }

    // Welche Wohnung wird gerade im Dialog bearbeitet?
    var dialogWohnungId by remember { mutableStateOf<Long?>(null) }

    fun aktivePerioden(): List<VorauszahlungsPeriode> =
        periodenByWohnungId
            .filter { (wohnungId, _) ->
                modusByWohnungId[wohnungId] == VorauszahlungsModus.PERIODEN
            }
            .flatMap { it.value }

    fun aktivePeriodenMap(): Map<Long, List<VorauszahlungsPeriode>> =
        periodenByWohnungId
            .filter { (wohnungId, _) ->
                modusByWohnungId[wohnungId] == VorauszahlungsModus.PERIODEN
            }

    fun compute(): List<AbrechnungErgebnis> =
        try {
            Abrechnung.berechnen(
                wohnungen = wohnungen,
                mieter = mieter,
                kostenarten = kostenarten,
                monateByWohnungId = monateMap,
                vorauszahlungen = aktivePerioden(),
                jahr = year
            )
        } catch (e: Exception) {
            errorMessage =
                "Fehler bei der Berechnung der Abrechnung: ${e.message ?: "Unbekannter Fehler"}"
            println("[SettlementScreen] Fehler in Abrechnung.berechnen: ${e.stackTraceToString()}")
            emptyList()
        }

    var ergebnisse by remember { mutableStateOf(compute()) }

    val gesamtKosten = kostenarten.fold(BigDecimal.ZERO) { acc, k -> acc + k.summe }

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
                title = "Wohnungskostenabrechnung",
                trailing = {
                    YearSelector(
                        year = year,
                        onChange = { newYear ->
                            year = newYear
                            reloadPeriodenForYear(newYear)
                            ergebnisse = compute()
                        }
                    )
                }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Summe der umlagefähigen Kosten: ${gesamtKosten.toEuro()} €",
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            // Tabelle in einer Card, wie bei den anderen Übersichten
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    AbrechnungHeader()
                    HorizontalDivider()

                    ergebnisse.forEach { erg ->
                        val wohnungId = erg.wohnungId
                        val mode = modusByWohnungId[wohnungId] ?: VorauszahlungsModus.STANDARD
                        val monateValue = monateMap[wohnungId] ?: erg.monate

                        AbrechnungRow(
                            erg = erg,
                            monateValue = monateValue,
                            vorausMode = mode,
                            onMonateChange = { newMonate ->
                                // defensives Clamping optional: newMonate.coerceIn(0, 12)
                                monateMap = monateMap.toMutableMap().also {
                                    it[wohnungId] = newMonate
                                }
                                ergebnisse = compute()
                            },
                            onModeChange = { newMode ->
                                modusByWohnungId = modusByWohnungId.toMutableMap().also {
                                    it[wohnungId] = newMode
                                }
                                if (newMode == VorauszahlungsModus.PERIODEN) {
                                    dialogWohnungId = wohnungId
                                } else {
                                    // Wechsel zurück auf Standard → Perioden für dieses Jahr löschen
                                    try {
                                        VorauszahlungsperiodenRepository.replaceForWohnungUndJahr(
                                            wohnungId = wohnungId,
                                            jahr = year,
                                            periodList = emptyList()
                                        )
                                        periodenByWohnungId = periodenByWohnungId.toMutableMap().also {
                                            it[wohnungId] = emptyList()
                                        }
                                    } catch (e: Exception) {
                                        errorMessage =
                                            "Fehler beim Zurücksetzen der zeitlichen Vorauszahlungen: ${e.message ?: "Unbekannter Fehler"}"
                                        println("[SettlementScreen] Fehler beim Reset der Perioden: ${e.stackTraceToString()}")
                                    }

                                    ergebnisse = compute()
                                }
                            },
                            onEditPerioden = {
                                dialogWohnungId = wohnungId
                            }
                        )

                        HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { ergebnisse = compute() }) {
                    Text("Neu berechnen")
                }
                Button(
                    onClick = {
                        ergebnisse = compute()
                        try {
                            WohnungskostenRepository.saveForYear(year, ergebnisse)
                            eigentuemer?.let { owner ->
                                PdfService.generateAbrechnungPdf(
                                    jahr = year,
                                    eigentuemer = owner,
                                    kostenarten = kostenarten,
                                    ergebnisse = ergebnisse,
                                    periodenByWohnungId = aktivePeriodenMap()
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage =
                                "Fehler beim Speichern oder PDF-Erzeugen: ${e.message ?: "Unbekannter Fehler"}"
                            println("[SettlementScreen] Fehler beim Speichern/PDF: ${e.stackTraceToString()}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Speichern")
                }
            }
        }
    }

    // ---------- Dialog für Vorauszahlungs-Perioden ----------

    val currentWohnung = dialogWohnungId?.let { id ->
        wohnungen.find { it.id == id }
    }
    if (currentWohnung != null) {
        val currentPerioden = periodenByWohnungId[currentWohnung.id].orEmpty()
        VorauszahlungenDialog(
            jahr = year,
            wohnung = currentWohnung,
            initialPerioden = currentPerioden,
            onDismiss = { dialogWohnungId = null },
            onConfirm = { neuePerioden ->
                try {
                    VorauszahlungsperiodenRepository.replaceForWohnungUndJahr(
                        wohnungId = currentWohnung.id,
                        jahr = year,
                        periodList = neuePerioden
                    )

                    periodenByWohnungId = periodenByWohnungId.toMutableMap().also {
                        it[currentWohnung.id] = neuePerioden
                    }
                    modusByWohnungId = modusByWohnungId.toMutableMap().also {
                        it[currentWohnung.id] = VorauszahlungsModus.PERIODEN
                    }

                    dialogWohnungId = null
                    ergebnisse = compute()
                } catch (e: Exception) {
                    errorMessage =
                        "Fehler beim Speichern der zeitlichen Vorauszahlungen: ${e.message ?: "Unbekannter Fehler"}"
                    println("[SettlementScreen] Fehler im VorauszahlungenDialog: ${e.stackTraceToString()}")
                }
            }
        )
    }

    // ---------- Fehler-Dialog ----------
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

/* -------------------------------------------------------------------------- */
/*  Tabellen-Header + Zeilenlayout                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun AbrechnungHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCellWeighted("Mieter")
        HeaderCellWeighted("Wohnfläche")
        HeaderCellWeighted("Monate")
        HeaderCellWeighted("Einheiten")
        HeaderCellWeighted("Umlage (€)")
        HeaderCellWeighted("Vorauszahlung")
        HeaderCellWeighted("Nachzahlung / Guthaben")
    }
}

@Composable
private fun RowScope.HeaderCellWeighted(text: String) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AbrechnungRow(
    erg: AbrechnungErgebnis,
    monateValue: Int,
    vorausMode: VorauszahlungsModus,
    onMonateChange: (Int) -> Unit,
    onModeChange: (VorauszahlungsModus) -> Unit,
    onEditPerioden: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spalte 1: Mieter
        Cell {
            Column {
                Text(erg.mieterName, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Wohnung #${erg.wohnungId}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Spalte 2: Wohnfläche
        Cell {
            Text("${erg.wohnflaeche} m²")
        }

        // Spalte 3: Monate (editierbar)
        Cell {
            OutlinedTextField(
                value = monateValue.toString(),
                onValueChange = { t -> t.toIntOrNull()?.let(onMonateChange) },
                singleLine = true,
                modifier = Modifier.width(70.dp)
            )
        }

        // Spalte 4: Einheiten
        Cell {
            Text(erg.einheiten.toString())
        }

        // Spalte 5: Umlage
        Cell {
            Text(erg.umlageBetrag.toEuro())
        }

        // Spalte 6: Vorauszahlung (Modus + Details)
        Cell {
            val smallTextStyle = MaterialTheme.typography.labelSmall

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = vorausMode == VorauszahlungsModus.STANDARD,
                        onClick = { onModeChange(VorauszahlungsModus.STANDARD) },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Standardbetrag",
                        style = smallTextStyle
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = vorausMode == VorauszahlungsModus.PERIODEN,
                        onClick = { onModeChange(VorauszahlungsModus.PERIODEN) },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Zeitliche Beträge",
                        style = smallTextStyle
                    )
                }

                if (vorausMode == VorauszahlungsModus.PERIODEN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TextButton(
                            onClick = onEditPerioden,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Perioden bearbeiten",
                                style = smallTextStyle
                            )
                        }
                    }
                }

                Text(
                    text = "Jahresvorauszahlung: ${erg.vorauszahlung.toEuro()} €",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Spalte 7: Ergebnis
        Cell {
            val isGuthaben = erg.ergebnis > BigDecimal.ZERO
            Text(
                text = erg.ergebnis.toEuro(withSign = true),
                color = if (isGuthaben)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Eine Tabellenzelle mit gleicher Breite für jede Spalte. */
@Composable
private fun RowScope.Cell(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

/* -------------------------------------------------------------------------- */
/*  Dialog zur Bearbeitung der Vorauszahlungs-Perioden                        */
/* -------------------------------------------------------------------------- */

@Composable
private fun VorauszahlungenDialog(
    jahr: Int,
    wohnung: Wohnung,
    initialPerioden: List<VorauszahlungsPeriode>,
    onDismiss: () -> Unit,
    onConfirm: (List<VorauszahlungsPeriode>) -> Unit
) {
    data class PeriodUi(
        val von: String,
        val bis: String,
        val betrag: String
    )

    var periodsUi by remember(initialPerioden, jahr) {
        mutableStateOf(
            if (initialPerioden.isEmpty()) {
                listOf(
                    PeriodUi(
                        von = "01.$jahr",
                        bis = "12.$jahr",
                        betrag = wohnung.vorauszahlung?.toPlainString() ?: ""
                    )
                )
            } else {
                initialPerioden.map {
                    PeriodUi(
                        von = it.von.format(java.time.format.DateTimeFormatter.ofPattern("MM.yyyy")),
                        bis = it.bis.format(java.time.format.DateTimeFormatter.ofPattern("MM.yyyy")),
                        betrag = it.betragMonat.toPlainString()
                    )
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vorauszahlungen für ${wohnung.adresse}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Monat/Jahr im Format MM.JJJJ angeben (z.B. 01.$jahr). " +
                            "Betrag = Monatsvorauszahlung in €."
                )

                periodsUi.forEachIndexed { index, ui ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Periode ${index + 1}", fontWeight = FontWeight.SemiBold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ui.von,
                                onValueChange = { value ->
                                    periodsUi = periodsUi.toMutableList().also { list ->
                                        list[index] = list[index].copy(von = value)
                                    }
                                },
                                label = { Text("Von (MM.JJJJ)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ui.bis,
                                onValueChange = { value ->
                                    periodsUi = periodsUi.toMutableList().also { list ->
                                        list[index] = list[index].copy(bis = value)
                                    }
                                },
                                label = { Text("Bis (MM.JJJJ)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = ui.betrag,
                            onValueChange = { value ->
                                periodsUi = periodsUi.toMutableList().also { list ->
                                    list[index] = list[index].copy(betrag = value)
                                }
                            },
                            label = { Text("Betrag pro Monat (€)") },
                            singleLine = true
                        )

                        Row {
                            Spacer(Modifier.weight(1f))
                            if (periodsUi.size > 1) {
                                TextButton(onClick = {
                                    periodsUi = periodsUi.toMutableList().also { list ->
                                        list.removeAt(index)
                                    }
                                }) {
                                    Text("Periode entfernen")
                                }
                            }
                        }

                        HorizontalDivider()
                    }
                }

                TextButton(onClick = {
                    periodsUi = periodsUi + PeriodUi(
                        von = "01.$jahr",
                        bis = "12.$jahr",
                        betrag = wohnung.vorauszahlung?.toPlainString() ?: ""
                    )
                }) {
                    Text("Periode hinzufügen")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val result = periodsUi.mapNotNull { ui ->
                    val vonYm = parseYearMonth(ui.von)
                    val bisYm = parseYearMonth(ui.bis)
                    val betrag = ui.betrag.replace(',', '.').toBigDecimalOrNull()

                    if (vonYm != null && bisYm != null && betrag != null && betrag >= BigDecimal.ZERO) {
                        VorauszahlungsPeriode(
                            wohnungId = wohnung.id,
                            mieterId = wohnung.aktuellerMieterId,
                            betragMonat = betrag,
                            von = vonYm.atDay(1),
                            bis = bisYm.atEndOfMonth()
                        )
                    } else {
                        null
                    }
                }
                onConfirm(result)
            }) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun parseYearMonth(text: String): YearMonth? =
    try {
        val parts = text.trim().split('.', '-', '/')
        if (parts.size == 2) {
            val month = parts[0].toInt()
            val year = parts[1].toInt()
            YearMonth.of(year, month)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
