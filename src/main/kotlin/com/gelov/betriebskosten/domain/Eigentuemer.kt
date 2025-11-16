package com.gelov.betriebskosten.domain

import java.time.LocalDateTime

data class Eigentuemer(
    val id: EigentuemerId,
    val grundstueck: String?,
    val name: String,
    val abrechnungsperiode: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
