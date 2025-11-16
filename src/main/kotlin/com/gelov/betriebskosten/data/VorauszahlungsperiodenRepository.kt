package com.gelov.betriebskosten.data

import com.gelov.betriebskosten.domain.VorauszahlungsPeriode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object VorauszahlungsperiodenRepository {

    // -------------------------------------------------------------------------
    // Kleines internes Logging – später leicht auf echtes Logging umstellbar
    // -------------------------------------------------------------------------
    private fun warn(message: String) {
        println("[VorauszahlungsperiodenRepository WARNING] $message")
    }

    // -------------------------------------------------------------------------
    // Mapping DB -> Domain
    // -------------------------------------------------------------------------
    private fun rowToDomain(row: ResultRow): VorauszahlungsPeriode =
        VorauszahlungsPeriode(
            wohnungId = row[VorauszahlungsperiodenTable.wohnungId],
            mieterId = row[VorauszahlungsperiodenTable.mieterId],
            betragMonat = row[VorauszahlungsperiodenTable.betragMonat],
            von = row[VorauszahlungsperiodenTable.von],
            bis = row[VorauszahlungsperiodenTable.bis]
        )

    // -------------------------------------------------------------------------
    // Lesen: alle Perioden, die ein bestimmtes Jahr ÜBERLAPPEN
    //
    // Semantik:
    //  - von <= 31.12.jahr  UND  bis >= 01.01.jahr
    //    → jede Periode, die das Jahr irgendwie schneidet (auch über mehrere Jahre).
    // -------------------------------------------------------------------------
    fun getForWohnungUndJahr(wohnungId: Long, jahr: Int): List<VorauszahlungsPeriode> = transaction {
        if (jahr !in 1900..2100) {
            warn("getForWohnungUndJahr: ungewöhnliches Jahr $jahr – liefere leere Liste.")
            return@transaction emptyList()
        }

        val jahrStart: LocalDate = YearMonth.of(jahr, 1).atDay(1)
        val jahrEnde: LocalDate = YearMonth.of(jahr, 12).atEndOfMonth()

        VorauszahlungsperiodenTable
            .selectAll()
            .where {
                (VorauszahlungsperiodenTable.wohnungId eq wohnungId) and
                        (VorauszahlungsperiodenTable.von lessEq jahrEnde) and
                        (VorauszahlungsperiodenTable.bis greaterEq jahrStart)
            }
            .map(::rowToDomain)
    }

    // -------------------------------------------------------------------------
    // Löschen: alle Perioden, die ein Jahr überlappen (z.B. beim "Reset")
    // -------------------------------------------------------------------------
    fun clearForWohnungUndJahr(
        wohnungId: Long,
        jahr: Int
    ) = transaction {
        if (jahr !in 1900..2100) {
            warn("clearForWohnungUndJahr: ungewöhnliches Jahr $jahr – kein Delete ausgeführt.")
            return@transaction
        }

        val jahrStart: LocalDate = YearMonth.of(jahr, 1).atDay(1)
        val jahrEnde: LocalDate = YearMonth.of(jahr, 12).atEndOfMonth()

        VorauszahlungsperiodenTable.deleteWhere {
            (VorauszahlungsperiodenTable.wohnungId eq wohnungId) and
                    (von lessEq jahrEnde) and
                    (bis greaterEq jahrStart)
        }
    }

    // -------------------------------------------------------------------------
    // Ersetzen: alle Perioden für dieses Jahr löschen und durch die neue Liste ersetzen
    //
    // - Delete-Logik deckt alle Perioden ab, die dieses Jahr überlappen
    // - Neue Perioden werden defensiv geprüft (Datum & Betrag), kaputte Einträge
    //   werden ignoriert und geloggt.
    // -------------------------------------------------------------------------
    fun replaceForWohnungUndJahr(
        wohnungId: Long,
        jahr: Int,
        periodList: List<VorauszahlungsPeriode>
    ) = transaction {
        if (jahr !in 1900..2100) {
            warn("replaceForWohnungUndJahr: ungewöhnliches Jahr $jahr – keine Änderungen gespeichert.")
            return@transaction
        }

        val jahrStart: LocalDate = YearMonth.of(jahr, 1).atDay(1)
        val jahrEnde: LocalDate = YearMonth.of(jahr, 12).atEndOfMonth()

        // Alte Perioden für diese Wohnung + Jahr löschen (alle, die das Jahr überlappen)
        VorauszahlungsperiodenTable.deleteWhere {
            (VorauszahlungsperiodenTable.wohnungId eq wohnungId) and
                    (von lessEq jahrEnde) and
                    (bis greaterEq jahrStart)
        }

        if (periodList.isEmpty()) {
            // Nichts mehr einzufügen – "Reset" für dieses Jahr
            return@transaction
        }

        // Neue einfügen – defensiv prüfen
        periodList.forEach { p ->
            // 1. Wohnung-Konsistenz prüfen
            if (p.wohnungId != wohnungId) {
                warn("replaceForWohnungUndJahr: Periode mit abweichender wohnungId (${p.wohnungId} != $wohnungId) – Eintrag übersprungen.")
                return@forEach
            }

            // 2. Datumslogik prüfen
            if (p.bis.isBefore(p.von)) {
                warn("replaceForWohnungUndJahr: Periode mit bis < von (${p.von}–${p.bis}) – Eintrag übersprungen.")
                return@forEach
            }

            // 3. Optional: extrem lange Perioden abfangen (z.B. Tippfehler "2100")
            val months = ChronoUnit.MONTHS.between(
                p.von.withDayOfMonth(1),
                p.bis.withDayOfMonth(1)
            ) + 1
            if (months <= 0L || months > 240L) {
                warn("replaceForWohnungUndJahr: Periode mit $months Monaten (${p.von}–${p.bis}) – Eintrag übersprungen.")
                return@forEach
            }

            // 4. Keine negativen Monatsbeträge
            if (p.betragMonat < BigDecimal.ZERO) {
                warn("replaceForWohnungUndJahr: negativer Monatsbetrag ${p.betragMonat} für Wohnung $wohnungId – Eintrag übersprungen.")
                return@forEach
            }

            // Wenn alles okay → einfügen (benanntes Lambda-Argument,
            // damit wir keinen Konflikt mit dem "it" vom forEach haben)
            VorauszahlungsperiodenTable.insert { row ->
                row[VorauszahlungsperiodenTable.wohnungId] = wohnungId
                row[mieterId] = p.mieterId
                row[betragMonat] = p.betragMonat
                row[von] = p.von
                row[bis] = p.bis
            }
        }
    }
}
