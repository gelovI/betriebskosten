package com.gelov.betriebskosten.domain

import java.time.LocalDateTime

data class Mieter(
    val id: MieterId,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
