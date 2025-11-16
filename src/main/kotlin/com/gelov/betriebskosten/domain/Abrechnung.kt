package com.gelov.betriebskosten.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AbrechnungErgebnis(
    val wohnungId: WohnungId,
    val mieterId: MieterId?,
    val mieterName: String,
    val wohnflaeche: Int,
    val monate: Int,
    val einheiten: Int,
    val umlageBetrag: BigDecimal,
    val vorauszahlung: BigDecimal,
    val ergebnis: BigDecimal   // >0 = Guthaben, <0 = Nachzahlung
)

object Abrechnung {

    /**
     * Berechnet die Betriebskostenabrechnung:
     *  - Einheiten = Wohnfläche * Monate (Monate werden defensiv auf [0,12] begrenzt)
     *  - Kosten je Einheit = Summe(Kostenarten.summe) / Summe(Einheiten)
     *  - Umlage je Wohnung = Kosten je Einheit * Einheiten
     *  - Ergebnis = Vorauszahlung - Umlage (positiv = Guthaben, negativ = Nachzahlung)
     *
     * Resilience-Aspekte:
     *  - Gibt bei offensichtlich inkonsistenten Eingaben (keine Einheiten, komisches Jahr) eine leere Liste
     *    zurück, loggt aber eine Warnung.
     *  - Extremwerte (z.B. falsche Perioden-Definitionen) werden abgefangen.
     */
    fun berechnen(
        wohnungen: List<Wohnung>,
        mieter: List<Mieter>,
        kostenarten: List<Kostenart>,
        monateByWohnungId: Map<WohnungId, Int>,
        vorauszahlungen: List<VorauszahlungsPeriode>,
        jahr: Int
    ): List<AbrechnungErgebnis> {

        if (wohnungen.isEmpty()) {
            warn("Abrechnung.berechnen: keine Wohnungen übergeben – Ergebnis bleibt leer.")
            return emptyList()
        }

        if (kostenarten.isEmpty()) {
            warn("Abrechnung.berechnen: keine Kostenarten übergeben – Ergebnis bleibt leer.")
            return emptyList()
        }

        // Jahr defensiv prüfen, um offensichtliche Tippfehler früh zu erkennen
        if (jahr !in 1900..2100) {
            warn("Abrechnung.berechnen: ungewöhnliches Jahr $jahr – Ergebnis bleibt leer.")
            return emptyList()
        }

        // Gesamtkosten = Summe aller Kostenarten
        val gesamtKosten = kostenarten.fold(BigDecimal.ZERO) { acc, k -> acc + k.summe }

        // Einheiten je Wohnung (Umlageschlüssel: Wohnfläche * Monate)
        val einheitenMap: Map<WohnungId, Int> = wohnungen.associate { w ->
            val monateRaw = monateByWohnungId[w.id] ?: 12
            // Monate werden defensiv auf 0..12 geklemmt, um UI-Fehleingaben abzufangen
            val monate = monateRaw.coerceIn(0, 12)

            val wohnflaeche = if (w.wohnflaecheQm < 0) {
                warn("Wohnung ${w.id}: negative Wohnfläche (${w.wohnflaecheQm}) – wird als 0 behandelt.")
                0
            } else {
                w.wohnflaecheQm
            }

            val einheiten = wohnflaeche * monate
            w.id to einheiten
        }

        val gesamtEinheiten = einheitenMap.values.sum()
        if (gesamtEinheiten <= 0) {
            warn("Abrechnung.berechnen: gesamtEinheiten == $gesamtEinheiten – vermutlich Monate oder Wohnflächen falsch. Ergebnis bleibt leer.")
            return emptyList()
        }

        // Kosten je Einheit
        val kostenProEinheit = gesamtKosten.divide(
            BigDecimal(gesamtEinheiten),
            6,
            RoundingMode.HALF_UP
        )

        // Vorauszahlungen nach Wohnung gruppieren
        val vorausByWohnung: Map<WohnungId, List<VorauszahlungsPeriode>> =
            vorauszahlungen.groupBy { it.wohnungId }

        // Mieter-Map für performante Auflösung (O(1) statt jedes Mal .find)
        val mieterById: Map<MieterId, Mieter> = mieter.associateBy { it.id }

        // Ergebnis je Wohnung
        return wohnungen.map { w ->
            val monateRaw = monateByWohnungId[w.id] ?: 12
            val monate = monateRaw.coerceIn(0, 12)

            val einheiten = einheitenMap[w.id] ?: 0

            // exakte Umlage aus Einheiten
            val umlageRaw = kostenProEinheit.multiply(BigDecimal(einheiten))

            // auf volle Euro runden (für Anzeige / Speicherung)
            val umlageGerundet = umlageRaw.roundToFullEuro()

            val perioden = vorausByWohnung[w.id]
            val voraus = berechneVorauszahlungFuerJahr(
                perioden = perioden,
                jahr = jahr,
                fallbackJahresbetrag = w.vorauszahlung,
                monateImJahr = monate
            )

            // Ergebnis = Vorauszahlung - Umla­geBetrag (auf ganze Euro gerundet),
            // damit das Ergebnis zu den angezeigten Werten passt.
            val ergebnisRaw = voraus.subtract(umlageGerundet)
            val ergebnisGerundet = ergebnisRaw.roundToFullEuro()

            val mieterName = w.aktuellerMieterId
                ?.let { id -> mieterById[id]?.name }
                ?: "Leerstand"

            AbrechnungErgebnis(
                wohnungId = w.id,
                mieterId = w.aktuellerMieterId,
                mieterName = mieterName,
                wohnflaeche = w.wohnflaecheQm,
                monate = monate,
                einheiten = einheiten,
                umlageBetrag = umlageGerundet,
                vorauszahlung = voraus,
                ergebnis = ergebnisGerundet
            )
        }
    }

    /**
     * Berechnet die Jahresvorauszahlung für ein gegebenes Jahr.
     *
     * - Wenn perioden == null/leer:
     *      Es wird angenommen, dass [fallbackJahresbetrag] eine JAHRES-vorauszahlung ist,
     *      die proportional auf [monateImJahr] verteilt wird.
     *
     * - Wenn perioden != null:
     *      Es wird über alle Perioden iteriert, die das Jahr [jahr] schneiden, und
     *      pro Monat p.betragMonat aufsummiert. Extremwerte werden defensiv gefiltert.
     */
    private fun berechneVorauszahlungFuerJahr(
        perioden: List<VorauszahlungsPeriode>?,
        jahr: Int,
        fallbackJahresbetrag: BigDecimal?,
        monateImJahr: Int
    ): BigDecimal {
        if (jahr !in 1900..2100) {
            warn("berechneVorauszahlungFuerJahr: ungewöhnliches Jahr $jahr – 0 als Vorauszahlung.")
            return BigDecimal.ZERO
        }

        if (perioden.isNullOrEmpty()) {
            val jahresbetrag = fallbackJahresbetrag ?: BigDecimal.ZERO
            if (monateImJahr <= 0) {
                return BigDecimal.ZERO
            }

            // Annahme: fallbackJahresbetrag ist eine JAHRESvorauszahlung,
            // die proportional auf die Monate verteilt wird.
            return jahresbetrag
                .multiply(BigDecimal(monateImJahr))
                .divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
        }

        var summe = BigDecimal.ZERO
        val jahrStart = LocalDate.of(jahr, 1, 1)
        val jahrEnde = LocalDate.of(jahr, 12, 31)

        for (p in perioden) {
            val start = maxOf(p.von, jahrStart)
            val end = minOf(p.bis, jahrEnde)
            if (end.isBefore(start)) {
                // Periode liegt komplett außerhalb des Jahres
                continue
            }

            val months = ChronoUnit.MONTHS
                .between(start.withDayOfMonth(1), end.withDayOfMonth(1)) + 1

            // Defensive Schranke gegen Tippfehler (z.B. 200 Jahre)
            if (months <= 0L || months > 240L) {
                warn("berechneVorauszahlungFuerJahr: ignorierte Periode (${p.von}–${p.bis}) mit $months Monaten.")
                continue
            }

            if (p.betragMonat < BigDecimal.ZERO) {
                warn("berechneVorauszahlungFuerJahr: negativer Monatsbetrag ${p.betragMonat} für Wohnung ${p.wohnungId} – Periode ignoriert.")
                continue
            }

            val periodSum = p.betragMonat.multiply(BigDecimal(months))
            summe = summe.add(periodSum)
        }

        return summe
    }

    /**
     * Rundet einen Betrag auf den nächsten vollen Euro.
     *
     * - 0,49 -> 0,00
     * - 0,50 -> 1,00
     * - -0,49 -> -0,00 (also 0,00)
     * - -0,50 -> -1,00
     *
     * Das Vorzeichen bleibt erhalten.
     */
    private fun BigDecimal.roundToFullEuro(): BigDecimal {
        val scaled = this.setScale(2, RoundingMode.HALF_UP)

        val sign = scaled.signum()
        val abs = scaled.abs()

        val integer = abs.setScale(0, RoundingMode.DOWN)
        val cents = abs.subtract(integer)

        val roundedAbs =
            if (cents >= BigDecimal("0.50")) integer + BigDecimal.ONE
            else integer

        val result = roundedAbs.setScale(2)
        return if (sign < 0) result.negate() else result
    }

    /**
     * Kleiner zentraler Logger für Domain-Warnungen.
     * Später kann das problemlos auf ein echtes Logging-Framework umgebogen werden.
     */
    private fun warn(message: String) {
        println("[Abrechnung WARNING] $message")
    }
}