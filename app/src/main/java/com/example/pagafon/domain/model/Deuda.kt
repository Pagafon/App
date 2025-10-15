package com.example.pagafon.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Deuda(
    val id: String,
    val numeroDocumento: String,
    val empresa: String,
    val tipo: String,
    val monto: Float,
    val fecha: LocalDate,
    val hora: LocalTime,
    val estado: String
)
