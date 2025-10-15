package com.example.pagafon.data.database

import androidx.room.*

@Dao
interface DeudaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDeuda(deuda: DeudaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDeudaEntitys(deudas: List<DeudaEntity>)

    @Update
    suspend fun actualizarDeudaEntity(deuda: DeudaEntity)

    @Delete
    suspend fun eliminarDeudaEntity(deuda: DeudaEntity)

    @Query("SELECT * FROM deudas ORDER BY id DESC")
    suspend fun obtenerTodasLasDeudaEntitys(): List<DeudaEntity>

    @Query("SELECT * FROM deudas ORDER BY id DESC")
    fun obtenerTodasLasDeudaEntitysSync(): List<DeudaEntity>

    @Query("SELECT * FROM deudas WHERE id = :id")
    suspend fun obtenerDeudaEntityPorId(id: Int): DeudaEntity?

    @Query("SELECT * FROM deudas WHERE estado = :estado")
    suspend fun obtenerDeudaEntitysPorEstado(estado: String): List<DeudaEntity>

    @Query("DELETE FROM deudas")
    suspend fun eliminarTodasLasDeudaEntitys()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarDeudaEntitySync(deuda: DeudaEntity): Long

    @Update
    fun actualizarDeudaEntitySync(deuda: DeudaEntity)

    @Delete
    fun eliminarDeudaEntitySync(deuda: DeudaEntity)

    @Query("SELECT * FROM deudas WHERE id = :id")
    fun obtenerDeudaEntityPorIdSync(id: Int): DeudaEntity?

    @Query("SELECT COUNT(*) FROM deudas WHERE numeroDocumento = :numeroDocumento")
    fun countByNumeroDocumento(numeroDocumento: String): Int
}