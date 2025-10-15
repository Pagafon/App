package com.example.pagafon.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.pagafon.data.model.DeudaImportada
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object FileReaderUtil {

    private const val TAG = "FileReaderUtil"
    private val TIPOS_VALIDOS = listOf("factura", "recibo", "impuesto", "prestamo", "otros")

    fun leerExcel(context: Context, uri: Uri): List<DeudaImportada> {
        val deudas = mutableListOf<DeudaImportada>()

        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            if (sheet.lastRowNum < 1) {
                Log.e(TAG, "El archivo no tiene filas de datos")
                return emptyList()
            }

            val headerRow = sheet.getRow(0)
            if (headerRow == null) {
                Log.e(TAG, "No se pudo leer la fila de encabezados")
                return emptyList()
            }

            val headers = mutableListOf<String>()
            headerRow.cellIterator().forEach { cell ->
                val headerValue = obtenerValorCelda(cell).trim().replace(" ", "").lowercase()
                headers.add(headerValue)
                Log.d(TAG, "Encabezado encontrado: '$headerValue'")
            }

            Log.d(TAG, "Total de encabezados: ${headers.size}")
            Log.d(TAG, "Encabezados: $headers")

            val colNumeroDocumento = buscarColumna(headers, listOf(
                "numerodedocumento", "numerodocumento", "documento", "nrodocumento",
                "nrodoc", "numdocumento", "nodocumento", "númerodedocumento"
            ))
            val colTipo = buscarColumna(headers, listOf(
                "tipo", "tipodeuda", "tipodocumento", "categoria"
            ))
            val colEmpresa = buscarColumna(headers, listOf(
                "empresa", "proveedor", "acreedor", "entidad"
            ))
            val colMonto = buscarColumna(headers, listOf(
                "monto", "importe", "valor", "cantidad", "total"
            ))
            val colFechaLimite = buscarColumna(headers, listOf(
                "fechalimite", "fechalímite", "fechavencimiento",
                "vencimiento", "fecha", "fechapago", "fechalímite"
            ))
            val colHoraNotificacion = buscarColumna(headers, listOf(
                "horadenotificacion", "horanotificacion", "hora",
                "horaaviso", "horarecordatorio", "horadelanotificacion"
            ))

            Log.d(TAG, "Índices encontrados - Doc: $colNumeroDocumento, Tipo: $colTipo, Empresa: $colEmpresa, Monto: $colMonto, Fecha: $colFechaLimite, Hora: $colHoraNotificacion")

            if (colNumeroDocumento == -1 || colEmpresa == -1 || colMonto == -1) {
                Log.e(TAG, "No se encontraron las columnas requeridas")
                return emptyList()
            }

            var filasValidas = 0
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val errores = mutableListOf<String>()

                val numeroDocumento = if (colNumeroDocumento != -1)
                    obtenerValorCelda(row.getCell(colNumeroDocumento)) else ""
                val tipo = if (colTipo != -1)
                    obtenerValorCelda(row.getCell(colTipo)).lowercase() else "otros"
                val empresa = if (colEmpresa != -1)
                    obtenerValorCelda(row.getCell(colEmpresa)) else ""
                val montoStr = if (colMonto != -1)
                    obtenerValorCelda(row.getCell(colMonto)) else ""
                val fechaLimite = if (colFechaLimite != -1)
                    obtenerValorCelda(row.getCell(colFechaLimite)) else ""
                val horaNotificacion = if (colHoraNotificacion != -1)
                    obtenerValorCelda(row.getCell(colHoraNotificacion)) else ""

                Log.d(TAG, "Fila $rowIndex - Doc: '$numeroDocumento', Tipo: '$tipo', Empresa: '$empresa', Monto: '$montoStr', Fecha: '$fechaLimite', Hora: '$horaNotificacion'")

                if (numeroDocumento.isBlank() && empresa.isBlank() && montoStr.isBlank()) {
                    continue
                }

                if (numeroDocumento.isBlank()) errores.add("El número de documento está vacío.")
                if (empresa.isBlank()) errores.add("La empresa está vacía.")

                val monto = montoStr.replace("[^0-9.]".toRegex(), "").toDoubleOrNull()
                if (monto == null || monto <= 0) errores.add("El monto no es válido.")

                if (errores.isEmpty()) {
                    filasValidas++
                    val fechaNormalizada = normalizarFecha(fechaLimite)
                    val horaNormalizada = normalizarHora(horaNotificacion)

                    Log.d(TAG, " Fila válida - Fecha normalizada: '$fechaNormalizada', Hora normalizada: '$horaNormalizada'")

                    deudas.add(
                        DeudaImportada(
                            numeroDocumento = numeroDocumento,
                            tipo = if (esTipoValido(tipo)) tipo else "otros",
                            empresa = empresa,
                            monto = monto!!,
                            fechaLimite = fechaNormalizada,
                            horaNotificacion = horaNormalizada,
                            esValido = true
                        )
                    )
                } else {
                    Log.w(TAG, "❌ Fila inválida - Errores: ${errores.joinToString(", ")}")
                    deudas.add(
                        DeudaImportada(
                            numeroDocumento = numeroDocumento,
                            tipo = tipo,
                            empresa = empresa,
                            monto = monto ?: 0.0,
                            fechaLimite = fechaLimite,
                            horaNotificacion = horaNotificacion,
                            esValido = false,
                            mensajeError = errores.joinToString(" ")
                        )
                    )
                }
            }

            Log.d(TAG, "Total de filas procesadas: ${deudas.size}, Válidas: $filasValidas")

            workbook.close()
            inputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer Excel: ${e.message}", e)
            e.printStackTrace()
        }
        return deudas
    }

    fun leerPDF(context: Context, uri: Uri): List<DeudaImportada> {
        try {
            PDFBoxResourceLoader.init(context)
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val document = PDDocument.load(inputStream)
            val pdfStripper = PDFTextStripper()
            val text = pdfStripper.getText(document)
            document.close()
            inputStream.close()

            Log.d(TAG, "Texto extraído del PDF:\n$text")

            val dataMap = mutableMapOf<String, String>()
            text.lines().forEach { linea ->
                val partes = linea.split(":", limit = 2)
                if (partes.size == 2) {
                    val clave = partes[0].trim().replace(" ", "").lowercase()
                    val valor = partes[1].trim()
                    dataMap[clave] = valor
                    Log.d(TAG, "PDF - Clave: '$clave' = '$valor'")
                }
            }

            val numeroDocumento = dataMap["numerodedocumento"] ?: dataMap["numerodocumento"] ?:
            dataMap["numerodoc"] ?: dataMap["documento"] ?: ""
            val tipo = dataMap["tipo"]?.lowercase() ?: "otros"
            val empresa = dataMap["empresa"] ?: ""
            val montoStr = dataMap["monto"] ?: ""
            val fechaLimiteRaw = dataMap["fechalimite"] ?: dataMap["fechalímite"] ?:
            dataMap["fecha"] ?: ""
            val horaNotificacionRaw = dataMap["hora"] ?: dataMap["horanotificacion"] ?:
            dataMap["horadelanotificacion"] ?: ""

            Log.d(TAG, "PDF - Valores extraídos:")
            Log.d(TAG, "  Número documento: '$numeroDocumento'")
            Log.d(TAG, "  Tipo: '$tipo'")
            Log.d(TAG, "  Empresa: '$empresa'")
            Log.d(TAG, "  Monto: '$montoStr'")
            Log.d(TAG, "  Fecha raw: '$fechaLimiteRaw'")
            Log.d(TAG, "  Hora raw: '$horaNotificacionRaw'")

            val errores = mutableListOf<String>()
            if (numeroDocumento.isBlank()) errores.add("No se encontró 'numero de documento'.")
            if (empresa.isBlank()) errores.add("No se encontró 'empresa'.")

            val monto = montoStr.replace("[^0-9.]".toRegex(), "").toDoubleOrNull()
            if (monto == null || monto <= 0) errores.add("Monto no válido o no se encontró.")

            // ⭐ NORMALIZAR FECHA Y HORA PARA PDF
            val fechaNormalizada = normalizarFecha(fechaLimiteRaw)
            val horaNormalizada = normalizarHora(horaNotificacionRaw)

            Log.d(TAG, "PDF - Normalizados:")
            Log.d(TAG, "  Fecha normalizada: '$fechaNormalizada'")
            Log.d(TAG, "  Hora normalizada: '$horaNormalizada'")

            val deuda = if (errores.isEmpty()) {
                Log.d(TAG, " PDF válido - Creando deuda")
                DeudaImportada(
                    numeroDocumento = numeroDocumento,
                    tipo = if (esTipoValido(tipo)) tipo else "otros",
                    empresa = empresa,
                    monto = monto!!,
                    fechaLimite = fechaNormalizada,
                    horaNotificacion = horaNormalizada,
                    esValido = true
                )
            } else {
                Log.w(TAG, " PDF inválido - Errores: ${errores.joinToString(", ")}")
                DeudaImportada(
                    numeroDocumento = numeroDocumento,
                    tipo = tipo,
                    empresa = empresa,
                    monto = monto ?: 0.0,
                    fechaLimite = fechaNormalizada,
                    horaNotificacion = horaNormalizada,
                    esValido = false,
                    mensajeError = errores.joinToString(" ")
                )
            }
            return listOf(deuda)

        } catch (e: Exception) {
            Log.e(TAG, "Error al leer PDF: ${e.message}", e)
            e.printStackTrace()
            return listOf(
                DeudaImportada(
                    "Error", "", "", 0.0, "", "", false,
                    "Error al leer PDF: ${e.message}"
                )
            )
        }
    }

    private fun normalizarFecha(fecha: String): String {
        if (fecha.isBlank()) {
            Log.d(TAG, "Fecha vacía, usando fecha actual")
            return obtenerFechaActual()
        }

        // Si ya está en formato dd/MM/yyyy, retornarla
        if (fecha.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
            Log.d(TAG, "Fecha ya en formato correcto: $fecha")
            return fecha
        }

        // Si está en formato dd-MM-yyyy, convertir a dd/MM/yyyy
        if (fecha.matches(Regex("\\d{2}-\\d{2}-\\d{4}"))) {
            val fechaConvertida = fecha.replace("-", "/")
            Log.d(TAG, "Fecha convertida de - a /: $fechaConvertida")
            return fechaConvertida
        }

        // Si está en formato dd/MM/yy (año de 2 dígitos), convertir a 4 dígitos
        if (fecha.matches(Regex("\\d{2}/\\d{2}/\\d{2}"))) {
            val partes = fecha.split("/")
            val año = partes[2].toInt()
            val añoCompleto = if (año < 50) 2000 + año else 1900 + año
            val fechaConvertida = "${partes[0]}/${partes[1]}/$añoCompleto"
            Log.d(TAG, "Fecha con año de 2 dígitos convertida: $fechaConvertida")
            return fechaConvertida
        }

        // Si no coincide con ningún formato, usar fecha actual
        Log.w(TAG, "Formato de fecha no reconocido: '$fecha', usando fecha actual")
        return obtenerFechaActual()
    }

    private fun normalizarHora(hora: String): String {
        if (hora.isBlank()) {
            Log.d(TAG, "Hora vacía, usando 08:00")
            return "08:00"
        }

        // Si ya está en formato HH:mm, retornarla
        if (hora.matches(Regex("\\d{2}:\\d{2}"))) {
            Log.d(TAG, "Hora ya en formato correcto: $hora")
            return hora
        }

        // Si tiene formato H:mm (sin cero inicial), agregar cero
        if (hora.matches(Regex("\\d{1}:\\d{2}"))) {
            val horaConCero = "0$hora"
            Log.d(TAG, "Hora con cero agregado: $horaConCero")
            return horaConCero
        }

        // Si tiene segundos (HH:mm:ss), removerlos
        if (hora.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
            val horaSinSegundos = hora.substring(0, 5)
            Log.d(TAG, "Hora sin segundos: $horaSinSegundos")
            return horaSinSegundos
        }

        // Si tiene formato de 12 horas (ej: 2:30 PM), convertir a 24 horas
        if (hora.matches(Regex("\\d{1,2}:\\d{2}\\s*[AaPp][Mm]"))) {
            try {
                val inputFormat = SimpleDateFormat("h:mm a", Locale.US)
                val outputFormat = SimpleDateFormat("HH:mm", Locale.US)
                val date = inputFormat.parse(hora)
                val horaConvertida = outputFormat.format(date!!)
                Log.d(TAG, "Hora convertida de 12h a 24h: $horaConvertida")
                return horaConvertida
            } catch (e: Exception) {
                Log.w(TAG, "Error al convertir hora de 12h a 24h: '$hora'", e)
            }
        }

        // Si no coincide con ningún formato, usar hora por defecto
        Log.w(TAG, "Formato de hora no reconocido: '$hora', usando 08:00")
        return "08:00"
    }

    private fun buscarColumna(headers: List<String>, variantes: List<String>): Int {
        for (variante in variantes) {
            val index = headers.indexOf(variante)
            if (index != -1) {
                Log.d(TAG, "Columna encontrada: '$variante' en índice $index")
                return index
            }
        }
        Log.w(TAG, "No se encontró columna para variantes: $variantes")
        return -1
    }

    private fun esTipoValido(tipo: String): Boolean {
        return TIPOS_VALIDOS.contains(tipo.lowercase())
    }

    private fun obtenerValorCelda(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    val value = cell.numericCellValue
                    if (value % 1.0 == 0.0) {
                        value.toLong().toString()
                    } else {
                        value.toString()
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> try {
                cell.numericCellValue.toString()
            } catch (e: Exception) {
                cell.stringCellValue
            }
            else -> ""
        }
    }

    private fun obtenerFechaActual(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
}