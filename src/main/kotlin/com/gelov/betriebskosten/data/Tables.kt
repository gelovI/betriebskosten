package com.gelov.betriebskosten.data

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object EigentuemerTable : Table("eigentuemer") {
    val id = long("id").autoIncrement()
    val grundstueck = varchar("grundstueck", 255).nullable()
    val name = varchar("name", 255)
    val abrechnungsperiode = varchar("abrechnungsperiode", 255)
    override val primaryKey = PrimaryKey(id)
}

object MieterTable : Table("mieter") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}

object WohnungenTable : Table("wohnungen") {
    val id = long("id").autoIncrement()
    val adresse = varchar("adresse", 255)
    val wohnflaeche = integer("wohnflaeche")
    val vorauszahlung = decimal("vorauszahlung", 10, 2).nullable()
    val aktuellerMieterId = long("aktueller_mieter_id").nullable()
    override val primaryKey = PrimaryKey(id)
}

object KostenartenTable : Table("kostenarten") {
    val id = long("id").autoIncrement()
    val bezeichnung = varchar("bezeichnung", 255)
    val beschreibung = varchar("beschreibung", 255).nullable()
    val summe = decimal("summe", 10, 2)
    override val primaryKey = PrimaryKey(id)
}

object WohnungskostenTable : Table("wohnungskosten") {
    val id = long("id").autoIncrement()
    val wohnungId = long("wohnung_id")
    val mieterId = long("mieter_id").nullable()
    val betrag = decimal("betrag", 10, 2)
    val anteil = decimal("anteil", 10, 4).nullable()
    val ergebnis = decimal("ergebnis", 10, 2).nullable()
    val jahr = integer("jahr")
    val monate = integer("monate").nullable()
    val einheiten = integer("einheiten").nullable()
    val umlage = decimal("umlage", 10, 2).nullable()
    override val primaryKey = PrimaryKey(id)
}


object VorauszahlungsperiodenTable : LongIdTable("vorauszahlungsperioden") {
    val wohnungId = long("wohnung_id")
    val mieterId = long("mieter_id").nullable()
    val betragMonat = decimal("betrag_monat", precision = 12, scale = 2)
    val von = date("von")
    val bis = date("bis")
}

