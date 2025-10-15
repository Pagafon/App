    package com.example.pagafon.ui.deudas;

    import android.app.AlertDialog;
    import android.app.DatePickerDialog;
    import android.app.TimePickerDialog;
    import android.content.Intent;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Looper;
    import android.view.View;
    import android.widget.AdapterView;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.Spinner;
    import android.widget.TextView;

    import androidx.appcompat.app.AppCompatActivity;
    import com.example.pagafon.R;
    import com.example.pagafon.data.database.AppDatabase;
    import com.example.pagafon.data.database.DeudaDao;
    import com.example.pagafon.data.database.DeudaEntity;
    import com.example.pagafon.utils.DeudaNotificationWorker;

    import java.util.Calendar;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;

    public class ActualizarDeudaActivity extends AppCompatActivity {
        private Button actualizarButton, cambiarEstadoButton;
        private DeudaDao deudaDao;
        private DeudaEntity deuda;
        private TextView empresaTextView, fechaTextView, horaTextView, idTextView, montoTextView;
        private ImageButton seleccionarFechaButton, seleccionarHoraButton;
        private ImageView imageView;
        public String selecciontipo;
        Spinner tipoSpinner;
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.actualizar_deuda);

            deudaDao = AppDatabase.getDatabase(getApplicationContext()).deudaDao();

            idTextView = findViewById(R.id.textNumber);
            empresaTextView = findViewById(R.id.editTextEmpresa);
            montoTextView = findViewById(R.id.editTextNumberDecimal);
            fechaTextView = findViewById(R.id.textDate);
            horaTextView = findViewById(R.id.TextTime);
            imageView = findViewById(R.id.imageView);
            tipoSpinner = findViewById(R.id.spinner_lista);
            seleccionarFechaButton = findViewById(R.id.FechaButton);
            seleccionarHoraButton = findViewById(R.id.HoraButton);
            cambiarEstadoButton = findViewById(R.id.btnEstado);
            actualizarButton = findViewById(R.id.btnCheck);

            int deudaId = Integer.parseInt(getIntent().getStringExtra("id"));

            executorService.execute(() -> {
                deuda = deudaDao.obtenerDeudaEntityPorIdSync(deudaId);
                handler.post(() -> {
                    if (deuda != null) {
                        populateUI();
                    }
                });
            });
        }

        private void populateUI() {
            idTextView.setText(String.valueOf(deuda.getId()));
            empresaTextView.setText(deuda.getEmpresa());
            montoTextView.setText(String.valueOf(deuda.getMonto()));
            fechaTextView.setText(deuda.getFechaLimite());
            horaTextView.setText(deuda.getHoraNotificacion());

            ArrayAdapter<CharSequence> listaAdapter = ArrayAdapter.createFromResource(this,
                    R.array.opciones_spinner_lista, android.R.layout.simple_spinner_item);
            listaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tipoSpinner.setAdapter(listaAdapter);

            if (!deuda.getTipo().isEmpty()) {
                int posicionLista = listaAdapter.getPosition(deuda.getTipo());
                tipoSpinner.setSelection(posicionLista);
            }

            seleccionarFechaButton.setOnClickListener(v -> mostrarSelectorFecha());
            fechaTextView.setOnClickListener(v -> mostrarSelectorFecha());
            seleccionarHoraButton.setOnClickListener(v -> mostrarSelectorHora());
            horaTextView.setOnClickListener(v -> mostrarSelectorHora());

            tipoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selecciontipo = (String) parent.getItemAtPosition(position);
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    selecciontipo = (String) parent.getItemAtPosition(0);
                }
            });

            // ✅ CAMBIO 2: Aceptar tanto "Por pagar" como "Pendiente" (estados editables)
            String estado = deuda.getEstado();
            if ("Por pagar".equals(estado) || "Pendiente".equals(estado)) {
                cambiarEstadoButton.setVisibility(View.VISIBLE);
                actualizarButton.setVisibility(View.VISIBLE);
            } else {
                // Si ya está pagado, ocultar botones de edición
                cambiarEstadoButton.setVisibility(View.GONE);
                actualizarButton.setVisibility(View.GONE);
            }
        }

        public void eliminarRecordatorio(View view) {
            executorService.execute(() -> {
                deudaDao.eliminarDeudaEntitySync(deuda);
                handler.post(() -> finish());
            });
        }

        private void mostrarSelectorFecha() {
            final Calendar calendar = Calendar.getInstance();
            int año = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int día = calendar.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                fechaTextView.setText(fechaSeleccionada);
            }, año, mes, día).show();
        }

        private void mostrarSelectorHora() {
            final Calendar calendar = Calendar.getInstance();
            int horas = calendar.get(Calendar.HOUR_OF_DAY);
            int minuto = calendar.get(Calendar.MINUTE);

            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String horaSeleccionada = String.format("%02d:%02d", hourOfDay, minute);
                horaTextView.setText(horaSeleccionada);
            }, horas, minuto, true).show();
        }

        public void irASegundoLayout(View view) {
            try {
                String empresa = empresaTextView.getText().toString();
                double monto = Double.parseDouble(montoTextView.getText().toString());
                String fecha = fechaTextView.getText().toString();
                String hora = horaTextView.getText().toString();
                String tipo = selecciontipo;

                DeudaEntity deudaActualizada = new DeudaEntity();
                deudaActualizada.setId(deuda.getId());
                deudaActualizada.setNumeroDocumento(deuda.getNumeroDocumento());
                deudaActualizada.setTipo(tipo);
                deudaActualizada.setEmpresa(empresa);
                deudaActualizada.setMonto(monto);
                deudaActualizada.setFechaLimite(fecha);
                deudaActualizada.setHoraNotificacion(hora);
                deudaActualizada.setEstado(deuda.getEstado());

                executorService.execute(() -> {
                    deudaDao.actualizarDeudaEntitySync(deudaActualizada);

                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, obtenerHora(hora));
                    selectedCalendar.set(Calendar.MINUTE, obtenerMinuto(hora));
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, obtenerDia(fecha));
                    selectedCalendar.set(Calendar.MONTH, obtenerMes(fecha) - 1);

                    Long alertTime = selectedCalendar.getTimeInMillis() - System.currentTimeMillis();
                    if (alertTime > 0) {
                        String tag = java.util.UUID.randomUUID().toString();
                        androidx.work.Data data = new androidx.work.Data.Builder()
                                .putString("titulo", empresa + " - " + tipo)
                                .putString("detalle", "S/." + monto)
                                .putInt("id_noti", (int) (Math.random() * 50 + 1))
                                .build();
                        DeudaNotificationWorker.programarNotificacion(alertTime, data, tag);
                    }

                    handler.post(() -> finish());
                });

            } catch (Exception e) {
                mostrarAlerta("Error", "Se requiere llenar todos los campos");
            }
        }

        public void cambiarEstado(View view) {
            DeudaEntity deudaActualizada = new DeudaEntity();
            deudaActualizada.setId(deuda.getId());
            deudaActualizada.setNumeroDocumento(deuda.getNumeroDocumento());
            deudaActualizada.setTipo(deuda.getTipo());
            deudaActualizada.setEmpresa(deuda.getEmpresa());
            deudaActualizada.setMonto(deuda.getMonto());
            deudaActualizada.setFechaLimite(deuda.getFechaLimite());
            deudaActualizada.setHoraNotificacion(deuda.getHoraNotificacion());
            deudaActualizada.setEstado("Pagado");

            executorService.execute(() -> {
                deudaDao.actualizarDeudaEntitySync(deudaActualizada);
                handler.post(() -> finish());
            });
        }

        private void mostrarAlerta(String titulo, String mensaje) {
            new AlertDialog.Builder(this)
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        private int obtenerHora(String hora) {
            String[] partes = hora.split(":");
            return Integer.parseInt(partes[0]);
        }

        private int obtenerMinuto(String hora) {
            String[] partes = hora.split(":");
            return Integer.parseInt(partes[1]);
        }

        private int obtenerDia(String fecha) {
            String[] partes = fecha.split("/");
            return Integer.parseInt(partes[0]);
        }

        private int obtenerMes(String fecha) {
            String[] partes = fecha.split("/");
            return Integer.parseInt(partes[1]);
        }
    }