package com.gelov.betriebskosten.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Wohnung(
    val id: WohnungId,
    val adresse: String,
    val wohnflaecheQm: Int,
    val vorauszahlung: BigDecimal?,
    val aktuellerMieterId: MieterId?,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
