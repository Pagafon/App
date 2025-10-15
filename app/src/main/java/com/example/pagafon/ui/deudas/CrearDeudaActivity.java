package com.example.pagafon.ui.deudas;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;

import com.example.pagafon.R;
import com.example.pagafon.data.database.AppDatabase;
import com.example.pagafon.data.database.DeudaDao;
import com.example.pagafon.data.database.DeudaEntity;
import com.example.pagafon.utils.DeudaNotificationWorker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearDeudaActivity extends AppCompatActivity {

    private static final String TAG = "CrearDeudaActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private EditText id, empresa, monto, fecha, hora;
    private Spinner tipo;
    private ImageButton seleccionarFechaButton, seleccionarHoraButton;
    private String selecciontipo;
    private DeudaDao deudaDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crear_deuda);

        // Solicitar permiso de notificaciones al iniciar
        solicitarPermisoNotificaciones();

        deudaDao = AppDatabase.getDatabase(getApplicationContext()).deudaDao();

        id = findViewById(R.id.editTextNumber);
        empresa = findViewById(R.id.editTextEmpresa);
        monto = findViewById(R.id.editTextNumberDecimal);
        fecha = findViewById(R.id.textDate);
        hora = findViewById(R.id.TextTime);
        tipo = findViewById(R.id.spinner);
        seleccionarFechaButton = findViewById(R.id.FechaButton);
        seleccionarHoraButton = findViewById(R.id.HoraButton);

        seleccionarFechaButton.setOnClickListener(v -> mostrarSelectorFecha());
        fecha.setOnClickListener(v -> mostrarSelectorFecha());
        seleccionarHoraButton.setOnClickListener(v -> mostrarSelectorHora());
        hora.setOnClickListener(v -> mostrarSelectorHora());

        ArrayAdapter<CharSequence> adapterlista = ArrayAdapter.createFromResource(this,
                R.array.opciones_spinner_lista, android.R.layout.simple_spinner_item);
        adapterlista.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipo.setAdapter(adapterlista);

        tipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selecciontipo = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selecciontipo = (String) parent.getItemAtPosition(0);
            }
        });
    }

    /**
     * Solicita el permiso de notificaciones para Android 13+
     */
    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Solicitando permiso de notificaciones");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Permiso de notificaciones ya concedido");
            }
        } else {
            Log.d(TAG, "Android < 13, no se requiere solicitar permiso explícito");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, " Permiso de notificaciones concedido");
                Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, " Permiso de notificaciones denegado");
                Toast.makeText(this, "No recibirás recordatorios de deudas", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Verifica si el permiso de notificaciones está concedido
     */
    private boolean tienePermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void mostrarSelectorFecha() {
        final Calendar calendar = Calendar.getInstance();
        int año = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int día = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
            fecha.setText(fechaSeleccionada);
        }, año, mes, día).show();
    }

    private void mostrarSelectorHora() {
        final Calendar calendar = Calendar.getInstance();
        int horas = calendar.get(Calendar.HOUR_OF_DAY);
        int minuto = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String horaSeleccionada = String.format("%02d:%02d", hourOfDay, minute);
            hora.setText(horaSeleccionada);
        }, horas, minuto, true).show();
    }

    /**
     * Convierte fecha (dd/MM/yyyy) y hora (HH:mm) a milisegundos
     */
    private long convertirFechaHoraAMillis(String fechaStr, String horaStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String fechaHoraCompleta = fechaStr + " " + horaStr;
            Date fecha = sdf.parse(fechaHoraCompleta);
            return fecha != null ? fecha.getTime() : -1;
        } catch (Exception e) {
            Log.e(TAG, "Error al convertir fecha/hora: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Programa la notificación para una deuda
     */
    private void programarNotificacionDeuda(int idDeuda, String empresaStr, String tipoStr,
                                            double montoDouble, String fechaStr, String horaStr) {

        // Verificar permiso
        if (!tienePermisoNotificaciones()) {
            Log.w(TAG, "No se puede programar notificación: permiso denegado");
            return;
        }

        // Convertir fecha y hora a milisegundos
        long fechaRecordatorioMillis = convertirFechaHoraAMillis(fechaStr, horaStr);

        if (fechaRecordatorioMillis == -1) {
            Log.e(TAG, "Error al convertir fecha/hora para notificación");
            return;
        }

        // Calcular tiempo hasta la notificación
        long tiempoActual = System.currentTimeMillis();
        long duracion = fechaRecordatorioMillis - tiempoActual;

        if (duracion <= 0) {
            Log.w(TAG, "La fecha de recordatorio ya pasó. No se programa notificación.");
            return;
        }

        // Preparar datos para la notificación
        String tituloNotif = " Recordatorio de Pago";
        String detalleNotif = String.format("%s - %s: S/ %.2f", empresaStr, tipoStr, montoDouble);

        Data data = new Data.Builder()
                .putString("titulo", tituloNotif)
                .putString("detalle", detalleNotif)
                .putLong("idNoti", idDeuda)
                .build();

        // Tag único para poder cancelar la notificación después
        String tag = "deuda_" + idDeuda;

        // Programar la notificación
        DeudaNotificationWorker.programarNotificacion(duracion, data, tag);

        Log.d(TAG, String.format(" Notificación programada: ID=%d, Empresa=%s, Fecha=%s %s, En=%d ms",
                idDeuda, empresaStr, fechaStr, horaStr, duracion));
    }

    public void irASegundoLayout(View view) {
        try {
            String numeroDocumento = id.getText().toString();
            String empresaStr = empresa.getText().toString();
            double montoDouble = Double.parseDouble(monto.getText().toString());
            String fechaStr = fecha.getText().toString();
            String horaStr = hora.getText().toString().isEmpty() ? "08:00" : hora.getText().toString();

            if (numeroDocumento.isEmpty() || empresaStr.isEmpty() || fechaStr.isEmpty()) {
                mostrarAlerta("Error", "Se requiere llenar todos los campos");
                return;
            }

            DeudaEntity nuevaDeuda = new DeudaEntity();
            nuevaDeuda.setNumeroDocumento(numeroDocumento);
            nuevaDeuda.setTipo(selecciontipo);
            nuevaDeuda.setEmpresa(empresaStr);
            nuevaDeuda.setMonto(montoDouble);
            nuevaDeuda.setFechaLimite(fechaStr);
            nuevaDeuda.setHoraNotificacion(horaStr);
            nuevaDeuda.setEstado("Por pagar");

            executorService.execute(() -> {
                if (deudaDao.countByNumeroDocumento(numeroDocumento) > 0) {
                    handler.post(() -> mostrarAlerta("Error", "Esta deuda ya existe"));
                } else {
                    // Insertar y obtener el ID generado
                    long idGenerado = deudaDao.insertarDeudaEntitySync(nuevaDeuda);

                    // Programar notificación con el ID real
                    handler.post(() -> {
                        programarNotificacionDeuda(
                                (int) idGenerado,
                                empresaStr,
                                selecciontipo,
                                montoDouble,
                                fechaStr,
                                horaStr
                        );

                        Toast.makeText(CrearDeudaActivity.this,
                                "✓ Deuda guardada\n Notificación programada",
                                Toast.LENGTH_SHORT).show();

                        finish();
                    });
                }
            });

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El monto debe ser un número válido");
        } catch (Exception e) {
            mostrarAlerta("Error", "Se requiere llenar todos los campos correctamente");
            Log.e(TAG, "Error al guardar deuda: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}