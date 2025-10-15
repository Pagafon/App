package com.example.pagafon.ui.importar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pagafon.R
import com.example.pagafon.data.model.DeudaImportada

class DeudaImportadaAdapter(
    private var deudas: List<DeudaImportada>
) : RecyclerView.Adapter<DeudaImportadaAdapter.DeudaViewHolder>() {

    class DeudaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmpresa: TextView = itemView.findViewById(R.id.tvEmpresa)
        val tvMonto: TextView = itemView.findViewById(R.id.tvMonto)
        val tvNumeroDocumento: TextView = itemView.findViewById(R.id.tvNumeroDocumento)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
        val tvFechaLimite: TextView = itemView.findViewById(R.id.tvFechaLimite)
        val tvHoraNotificacion: TextView = itemView.findViewById(R.id.tvHoraNotificacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeudaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deuda_previa, parent, false)
        return DeudaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeudaViewHolder, position: Int) {
        val deuda = deudas[position]

        holder.tvEmpresa.text = deuda.empresa
        holder.tvMonto.text = "S/ %.2f".format(deuda.monto)
        holder.tvNumeroDocumento.text = "Nro. Doc: ${deuda.numeroDocumento}"
        holder.tvTipo.text = deuda.tipo.uppercase()
        holder.tvFechaLimite.text = "Vence: ${deuda.fechaLimite}"
        holder.tvHoraNotificacion.text = "Notificar: ${deuda.horaNotificacion}"

        // Marcar en rojo si hay error
        if (!deuda.esValido) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE"))
            holder.tvEmpresa.setTextColor(Color.RED)
            // Mostrar el error en el campo que corresponda, por ejemplo, en el número de documento
            holder.tvNumeroDocumento.text = " ${deuda.mensajeError}"
            holder.tvNumeroDocumento.setTextColor(Color.RED)
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
            holder.tvEmpresa.setTextColor(Color.parseColor("#333333"))
            holder.tvNumeroDocumento.setTextColor(Color.parseColor("#666666"))
        }
    }

    override fun getItemCount() = deudas.size

    fun actualizarDatos(nuevasDeudas: List<DeudaImportada>) {
        deudas = nuevasDeudas
        notifyDataSetChanged()
    }
}
