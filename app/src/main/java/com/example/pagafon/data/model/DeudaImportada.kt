package com.example.pagafon.data.model

data class DeudaImportada(
    val numeroDocumento: String,
    val tipo: String,
    val empresa: String,
    val monto: Double,
    val fechaLimite: String,
    val horaNotificacion: String,
    var esValido: Boolean = true,
    var mensajeError: String? = null
)
