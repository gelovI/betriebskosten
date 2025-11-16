package com.gelov.betriebskosten.data

import com.gelov.betriebskosten.domain.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

// ---------- Eigentümer ----------

object EigentuemerRepository {

    fun getAll(): List<Eigentuemer> = transaction {
        EigentuemerTable
            .selectAll()
            .map { it.toEigentuemer() }
    }

    fun insert(
        grundstueck: String?,
        name: String,
        abrechnungsperiode: String
    ): Eigentuemer = transaction {
        val insertedId = EigentuemerTable.insert { row ->
            row[EigentuemerTable.grundstueck] = grundstueck
            row[EigentuemerTable.name] = name
            row[EigentuemerTable.abrechnungsperiode] = abrechnungsperiode
        }[EigentuemerTable.id]

        EigentuemerTable
            .selectAll()
            .where { EigentuemerTable.id eq insertedId }
            .single()
            .toEigentuemer()
    }

    fun update(
        id: Long,
        grundstueck: String?,
        name: String,
        abrechnungsperiode: String
    ) = transaction {
        EigentuemerTable.update({ EigentuemerTable.id eq id }) { row ->
            row[EigentuemerTable.grundstueck] = grundstueck
            row[EigentuemerTable.name] = name
            row[EigentuemerTable.abrechnungsperiode] = abrechnungsperiode
        }
    }

    fun delete(id: Long) = transaction {
        EigentuemerTable.deleteWhere { EigentuemerTable.id eq id }
    }
}

private fun ResultRow.toEigentuemer(): Eigentuemer =
    Eigentuemer(
        id = this[EigentuemerTable.id],
        grundstueck = this[EigentuemerTable.grundstueck],
        name = this[EigentuemerTable.name],
        abrechnungsperiode = this[EigentuemerTable.abrechnungsperiode],
        createdAt = null,
        updatedAt = null
    )

// ---------- Mieter ----------

object MieterRepository {

    fun getAll(): List<Mieter> = transaction {
        MieterTable
            .selectAll()
            .map { it.toMieter() }
    }

    fun insert(
        name: String,
        email: String?,
        phone: String?
    ): Mieter = transaction {
        val insertedId = MieterTable.insert { row ->
            row[MieterTable.name] = name
            row[MieterTable.email] = email
            row[MieterTable.phone] = phone
        }[MieterTable.id]

        MieterTable
            .selectAll()
            .where { MieterTable.id eq insertedId }
            .single()
            .toMieter()
    }

    fun update(
        id: Long,
        name: String,
        email: String?,
        phone: String?
    ) = transaction {
        MieterTable.update({ MieterTable.id eq id }) { row ->
            row[MieterTable.name] = name
            row[MieterTable.email] = email
            row[MieterTable.phone] = phone
        }
    }

    fun delete(id: Long) = transaction {
        MieterTable.deleteWhere { MieterTable.id eq id }
    }
}

private fun ResultRow.toMieter(): Mieter =
    Mieter(
        id = this[MieterTable.id],
        name = this[MieterTable.name],
        email = this[MieterTable.email],
        phone = this[MieterTable.phone],
        createdAt = null,
        updatedAt = null
    )

// ---------- Wohnungen ----------

object WohnungenRepository {

    fun getAll(): List<Wohnung> = transaction {
        WohnungenTable
            .selectAll()
            .map { it.toWohnung() }
    }

    fun insert(
        adresse: String,
        wohnflaeche: Int,
        vorauszahlung: java.math.BigDecimal?,
        aktuellerMieterId: Long?
    ): Wohnung = transaction {
        val insertedId = WohnungenTable.insert { row ->
            row[WohnungenTable.adresse] = adresse
            row[WohnungenTable.wohnflaeche] = wohnflaeche
            row[WohnungenTable.vorauszahlung] = vorauszahlung
            row[WohnungenTable.aktuellerMieterId] = aktuellerMieterId
        }[WohnungenTable.id]

        WohnungenTable
            .selectAll()
            .where { WohnungenTable.id eq insertedId }
            .single()
            .toWohnung()
    }

    fun update(
        id: Long,
        adresse: String,
        wohnflaeche: Int,
        vorauszahlung: java.math.BigDecimal?,
        aktuellerMieterId: Long?
    ) = transaction {
        WohnungenTable.update({ WohnungenTable.id eq id }) { row ->
            row[WohnungenTable.adresse] = adresse
            row[WohnungenTable.wohnflaeche] = wohnflaeche
            row[WohnungenTable.vorauszahlung] = vorauszahlung
            row[WohnungenTable.aktuellerMieterId] = aktuellerMieterId
        }
    }

    fun delete(id: Long) = transaction {
        WohnungenTable.deleteWhere { WohnungenTable.id eq id }
    }
}

private fun ResultRow.toWohnung(): Wohnung =
    Wohnung(
        id = this[WohnungenTable.id],
        adresse = this[WohnungenTable.adresse],
        wohnflaecheQm = this[WohnungenTable.wohnflaeche],
        vorauszahlung = this[WohnungenTable.vorauszahlung],
        aktuellerMieterId = this[WohnungenTable.aktuellerMieterId],
        createdAt = null,
        updatedAt = null
    )

// ---------- Kostenarten ----------

object KostenartenRepository {

    fun getAll(): List<Kostenart> = transaction {
        KostenartenTable
            .selectAll()
            .map { it.toKostenart() }
    }

    fun insert(
        bezeichnung: String,
        beschreibung: String?,
        summe: java.math.BigDecimal
    ): Kostenart = transaction {
        // Einfügen
        val insertedId: Long = KostenartenTable.insert { row ->
            row[KostenartenTable.bezeichnung] = bezeichnung
            row[KostenartenTable.beschreibung] = beschreibung
            row[KostenartenTable.summe] = summe
        }[KostenartenTable.id]

        // Den frisch eingefügten Datensatz wieder lesen
        KostenartenTable
            .selectAll()
            .where { KostenartenTable.id eq insertedId }
            .single()
            .toKostenart()
    }

    fun update(
        id: KostenartId,
        bezeichnung: String,
        beschreibung: String?,
        summe: java.math.BigDecimal
    ) = transaction {
        KostenartenTable.update({ KostenartenTable.id eq id }) { row ->
            row[KostenartenTable.bezeichnung] = bezeichnung
            row[KostenartenTable.beschreibung] = beschreibung
            row[KostenartenTable.summe] = summe
        }
    }

    fun delete(id: KostenartId) = transaction {
        KostenartenTable.deleteWhere { KostenartenTable.id eq id }
    }
}

private fun ResultRow.toKostenart(): Kostenart =
    Kostenart(
        id = this[KostenartenTable.id],
        bezeichnung = this[KostenartenTable.bezeichnung],
        beschreibung = this[KostenartenTable.beschreibung],
        summe = this[KostenartenTable.summe],
        createdAt = null,
        updatedAt = null
    )



// ---------- Wohnungskosten ----------

object WohnungskostenRepository {

    fun getAll(): List<Wohnungskosten> = transaction {
        WohnungskostenTable
            .selectAll()
            .map { it.toWohnungskosten() }
    }

    fun getForYear(jahr: Int): List<Wohnungskosten> = transaction {
        WohnungskostenTable
            .selectAll()
            .where { WohnungskostenTable.jahr eq jahr }   // neue DSL: selectAll().where { ... }
            .map { it.toWohnungskosten() }
    }

    /**
     * Speichert die Abrechnungsergebnisse für ein bestimmtes Jahr.
     * Vorher werden alte Einträge für dieses Jahr gelöscht.
     */
    fun saveForYear(jahr: Int, ergebnisse: List<AbrechnungErgebnis>) = transaction {
        // alte Datensätze für dieses Jahr löschen
        WohnungskostenTable.deleteWhere { WohnungskostenTable.jahr eq jahr }

        // neue Datensätze einfügen
        ergebnisse.forEach { e ->
            WohnungskostenTable.insert { row ->
                row[wohnungId] = e.wohnungId
                row[mieterId] = e.mieterId
                row[betrag] = e.umlageBetrag
                row[anteil] = e.umlageBetrag
                row[ergebnis] = e.ergebnis
                row[WohnungskostenTable.jahr] = jahr
                row[monate] = e.monate
                row[einheiten] = e.einheiten
                row[umlage] = e.umlageBetrag
            }
        }
    }
}

private fun ResultRow.toWohnungskosten(): Wohnungskosten =
    Wohnungskosten(
        id = this[WohnungskostenTable.id],
        wohnungId = this[WohnungskostenTable.wohnungId],
        mieterId = this[WohnungskostenTable.mieterId],
        betrag = this[WohnungskostenTable.betrag],
        anteil = this[WohnungskostenTable.anteil],
        ergebnis = this[WohnungskostenTable.ergebnis],
        jahr = this[WohnungskostenTable.jahr],
        monate = this[WohnungskostenTable.monate],
        einheiten = this[WohnungskostenTable.einheiten],
        umlage = this[WohnungskostenTable.umlage],
        createdAt = null,
        updatedAt = null
    )

