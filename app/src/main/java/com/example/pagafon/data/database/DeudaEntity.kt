package com.example.pagafon.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deudas")
data class DeudaEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var numeroDocumento: String = "",
    var tipo: String = "",
    var empresa: String = "",
    var monto: Double = 0.0,
    var fechaLimite: String = "",
    var horaNotificacion: String = "",
    var estado: String = "Pendiente"
)