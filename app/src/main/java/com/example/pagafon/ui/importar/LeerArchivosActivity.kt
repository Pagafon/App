package com.example.pagafon.ui.importar

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pagafon.R
import com.example.pagafon.data.database.AppDatabase
import com.example.pagafon.data.database.DeudaEntity
import com.example.pagafon.data.model.DeudaImportada
import com.example.pagafon.utils.DeudaNotificationWorker
import com.example.pagafon.utils.FileReaderUtil
import java.text.SimpleDateFormat
import java.util.*

class LeerArchivosActivity : AppCompatActivity() {

    private lateinit var btnSeleccionarExcel: MaterialButton
    private lateinit var btnSeleccionarPdf: MaterialButton
    private lateinit var btnImportar: MaterialButton
    private lateinit var tvArchivoSeleccionado: TextView
    private lateinit var tvCantidadRegistros: TextView
    private lateinit var cardVistaPrevia: MaterialCardView
    private lateinit var recyclerViewPrevia: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: DeudaImportadaAdapter
    private var deudasImportadas: List<DeudaImportada> = emptyList()
    private var uriSeleccionado: Uri? = null
    private var tipoArchivo: TipoArchivo? = null

    companion object {
        private const val TAG = "LeerArchivosActivity"
    }

    enum class TipoArchivo {
        EXCEL, PDF
    }

    private val seleccionarExcelLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            tipoArchivo = TipoArchivo.EXCEL
            procesarArchivo(it, TipoArchivo.EXCEL)
        }
    }

    private val seleccionarPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            tipoArchivo = TipoArchivo.PDF
            procesarArchivo(it, TipoArchivo.PDF)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso denegado. No se pueden leer archivos.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leer_archivos)

        inicializarVistas()
        configurarToolbar()
        configurarRecyclerView()
        configurarListeners()
        verificarPermisos()
    }

    private fun inicializarVistas() {
        btnSeleccionarExcel = findViewById(R.id.btnSeleccionarExcel)
        btnSeleccionarPdf = findViewById(R.id.btnSeleccionarPdf)
        btnImportar = findViewById(R.id.btnImportar)
        tvArchivoSeleccionado = findViewById(R.id.tvArchivoSeleccionado)
        tvCantidadRegistros = findViewById(R.id.tvCantidadRegistros)
        cardVistaPrevia = findViewById(R.id.cardVistaPrevia)
        recyclerViewPrevia = findViewById(R.id.recyclerViewPrevia)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun configurarToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configurarRecyclerView() {
        adapter = DeudaImportadaAdapter(emptyList())
        recyclerViewPrevia.layoutManager = LinearLayoutManager(this)
        recyclerViewPrevia.adapter = adapter
    }

    private fun configurarListeners() {
        btnSeleccionarExcel.setOnClickListener {
            seleccionarExcelLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        btnSeleccionarPdf.setOnClickListener {
            seleccionarPdfLauncher.launch("application/pdf")
        }

        btnImportar.setOnClickListener {
            mostrarDialogoConfirmacion()
        }
    }

    private fun verificarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun procesarArchivo(uri: Uri, tipo: TipoArchivo) {
        uriSeleccionado = uri
        val nombreArchivo = obtenerNombreArchivo(uri)
        tvArchivoSeleccionado.text = "📎 $nombreArchivo"
        progressBar.visibility = View.VISIBLE
        cardVistaPrevia.visibility = View.VISIBLE
        btnImportar.visibility = View.GONE

        lifecycleScope.launch {
            try {
                deudasImportadas = withContext(Dispatchers.IO) {
                    when (tipo) {
                        TipoArchivo.EXCEL -> FileReaderUtil.leerExcel(this@LeerArchivosActivity, uri)
                        TipoArchivo.PDF -> FileReaderUtil.leerPDF(this@LeerArchivosActivity, uri)
                    }
                }

                progressBar.visibility = View.GONE

                if (deudasImportadas.isNotEmpty()) {
                    adapter.actualizarDatos(deudasImportadas)
                    val validos = deudasImportadas.count { it.esValido }
                    val total = deudasImportadas.size
                    tvCantidadRegistros.text = "$total registros encontrados ($validos válidos)"
                    if (validos > 0) {
                        btnImportar.visibility = View.VISIBLE
                    }
                    Toast.makeText(this@LeerArchivosActivity, "✓ Archivo procesado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    tvCantidadRegistros.text = "No se encontraron datos en el archivo"
                    Toast.makeText(this@LeerArchivosActivity, "️ No se encontraron datos válidos", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@LeerArchivosActivity, "❌ Error al procesar archivo: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "Archivo desconocido"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            nombre = cursor.getString(nameIndex)
        }
        return nombre
    }

    private fun mostrarDialogoConfirmacion() {
        val deudasValidas = deudasImportadas.filter { it.esValido }
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar Importación")
            .setMessage("¿Deseas importar ${deudasValidas.size} deudas a la base de datos?\n\nSe programarán notificaciones automáticas para cada deuda.")
            .setPositiveButton("Importar") { _, _ ->
                importarDeudas(deudasValidas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Convierte fechaLimite (dd/MM/yyyy) y horaNotificacion (HH:mm) a milisegundos
     */
    private fun convertirFechaHoraAMillis(fechaLimite: String, horaNotificacion: String): Long {
        return try {
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fecha = sdfFecha.parse(fechaLimite) ?: return -1

            val calendar = Calendar.getInstance()
            calendar.time = fecha

            // Parsear hora (HH:mm)
            val partes = horaNotificacion.split(":")
            if (partes.size == 2) {
                val hora = partes[0].toInt()
                val minuto = partes[1].toInt()

                calendar.set(Calendar.HOUR_OF_DAY, hora)
                calendar.set(Calendar.MINUTE, minuto)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir fecha/hora: ${e.message}")
            -1
        }
    }

    /**
     * Programa una notificación para una deuda importada
     */
    private fun programarNotificacionDeuda(deuda: DeudaEntity): Boolean {
        val fechaRecordatorioMillis = convertirFechaHoraAMillis(deuda.fechaLimite, deuda.horaNotificacion)

        if (fechaRecordatorioMillis == -1L) {
            Log.e(TAG, "Error al convertir fecha/hora para deuda ID: ${deuda.id}")
            return false
        }

        val tiempoActual = System.currentTimeMillis()
        val duracion = fechaRecordatorioMillis - tiempoActual

        if (duracion <= 0L) {
            Log.w(TAG, "La fecha de recordatorio ya pasó para deuda ID: ${deuda.id}. No se programa notificación.")
            return false
        }

        // Preparar datos para la notificación
        val tituloNotif = " Recordatorio de Pago"
        val detalleNotif = "Recuerda pagar: ${deuda.empresa} - S/ %.2f".format(deuda.monto)

        val data = Data.Builder()
            .putString("titulo", tituloNotif)
            .putString("detalle", detalleNotif)
            .putLong("idNoti", deuda.id.toLong())
            .build()

        val tag = "deuda_${deuda.id}"
        DeudaNotificationWorker.programarNotificacion(duracion, data, tag)

        Log.d(TAG, " Notificación programada: ID=${deuda.id}, Empresa=${deuda.empresa}, Fecha=${deuda.fechaLimite} ${deuda.horaNotificacion}")
        return true
    }

    private fun importarDeudas(deudas: List<DeudaImportada>) {
        lifecycleScope.launch {
            var notificacionesProgramadas = 0

            try {
                val deudasGuardadas = withContext(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(this@LeerArchivosActivity)
                    val deudaDao = database.deudaDao()

                    val listaDeudasGuardadas = mutableListOf<DeudaEntity>()

                    deudas.forEach { deudaImportada ->
                        val deuda = DeudaEntity()
                        deuda.numeroDocumento = deudaImportada.numeroDocumento
                        deuda.tipo = deudaImportada.tipo
                        deuda.empresa = deudaImportada.empresa
                        deuda.monto = deudaImportada.monto
                        deuda.fechaLimite = deudaImportada.fechaLimite
                        deuda.horaNotificacion = deudaImportada.horaNotificacion
                        deuda.estado = "Por pagar"

                        // Insertar y obtener el ID generado
                        val idGenerado = deudaDao.insertarDeudaEntitySync(deuda)
                        deuda.id = idGenerado.toInt()

                        listaDeudasGuardadas.add(deuda)
                    }

                    listaDeudasGuardadas
                }

                // Programar notificaciones en el hilo principal
                deudasGuardadas.forEach { deuda ->
                    if (programarNotificacionDeuda(deuda)) {
                        notificacionesProgramadas++
                    }
                }

                Toast.makeText(
                    this@LeerArchivosActivity,
                    "✓ ${deudas.size} deudas importadas\n notificacionesProgramadas notificaciones programadas",
                    Toast.LENGTH_LONG
                ).show()

                MaterialAlertDialogBuilder(this@LeerArchivosActivity)
                    .setTitle("Importación Exitosa")
                    .setMessage("Se importaron ${deudas.size} deudas correctamente.\n\nSe programaron $notificacionesProgramadas notificaciones.\n\n¿Deseas importar más archivos?")
                    .setPositiveButton("Sí") { _, _ ->
                        reiniciarVista()
                    }
                    .setNegativeButton("Salir") { _, _ ->
                        finish()
                    }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@LeerArchivosActivity, " Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun reiniciarVista() {
        deudasImportadas = emptyList()
        adapter.actualizarDatos(emptyList())
        tvArchivoSeleccionado.text = "Ningún archivo seleccionado"
        tvCantidadRegistros.text = "0 registros encontrados"
        cardVistaPrevia.visibility = View.GONE
        btnImportar.visibility = View.GONE
        uriSeleccionado = null
        tipoArchivo = null
    }
}