package com.gelov.betriebskosten.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Kostenart(
    val id: KostenartId,
    val bezeichnung: String,
    val beschreibung: String?,
    val summe: BigDecimal,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
