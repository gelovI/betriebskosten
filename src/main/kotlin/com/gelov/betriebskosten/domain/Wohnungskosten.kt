package com.gelov.betriebskosten.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Wohnungskosten(
    val id: WohnungskostenId,
    val wohnungId: WohnungId,
    val mieterId: MieterId?,
    val betrag: BigDecimal,
    val anteil: BigDecimal?,
    val ergebnis: BigDecimal?,
    val jahr: Int,
    val monate: Int?,
    val einheiten: Int?,
    val umlage: BigDecimal?,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
