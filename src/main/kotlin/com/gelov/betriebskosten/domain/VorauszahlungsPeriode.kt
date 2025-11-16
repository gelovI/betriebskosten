package com.gelov.betriebskosten.domain

import java.math.BigDecimal
import java.time.LocalDate

data class VorauszahlungsPeriode(
    val id: Long? = null,
    val wohnungId: Long,
    val mieterId: Long?,
    val betragMonat: BigDecimal,
    val von: LocalDate,
    val bis: LocalDate
)
