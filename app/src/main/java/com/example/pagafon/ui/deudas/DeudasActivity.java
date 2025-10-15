package com.example.pagafon.ui.deudas;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pagafon.R;
import com.example.pagafon.data.database.AppDatabase;
import com.example.pagafon.data.database.DeudaDao;
import com.example.pagafon.domain.model.Deuda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeudasActivity extends AppCompatActivity {

    private LinearLayout layout;
    private DeudaDao deudaDao;
    private ArrayList<Deuda> deudas;
    private Spinner spinner;
    private String selecciontipo = "Todos";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @SuppressLint("UseCompatLoadingForColorStateLists")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deudas);

        // ⭐ CONFIGURAR TOOLBAR CON FLECHA DE REGRESO
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        layout = findViewById(R.id.layoutprincipal);
        deudaDao = AppDatabase.getDatabase(getApplicationContext()).deudaDao();
        spinner = findViewById(R.id.spinner2);

        ArrayAdapter<CharSequence> adapterlista = ArrayAdapter.createFromResource(this,
                R.array.opciones_spinner_listaRecordatorios, android.R.layout.simple_spinner_item);
        adapterlista.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterlista);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selecciontipo = (String) parent.getItemAtPosition(position);
                cargarDeudas();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selecciontipo = null;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDeudas();
    }

    private void cargarDeudas() {
        executorService.execute(() -> {
            List<com.example.pagafon.data.database.DeudaEntity> deudasFromRoom = deudaDao.obtenerTodasLasDeudaEntitysSync();
            ArrayList<Deuda> deudasConvertidas = new ArrayList<>();
            for (com.example.pagafon.data.database.DeudaEntity deudaRoom : deudasFromRoom) {
                try {
                    LocalDate fecha = LocalDate.parse(deudaRoom.getFechaLimite(), dateFormatter);
                    LocalTime hora = LocalTime.parse(deudaRoom.getHoraNotificacion(), timeFormatter);
                    Deuda deudaDomain = new Deuda(
                            String.valueOf(deudaRoom.getId()),
                            deudaRoom.getNumeroDocumento(),
                            deudaRoom.getEmpresa(),
                            deudaRoom.getTipo(),
                            (float)deudaRoom.getMonto(),
                            fecha,
                            hora,
                            deudaRoom.getEstado()
                    );
                    deudasConvertidas.add(deudaDomain);
                } catch (Exception e) {
                    // Log error or handle exception
                }
            }

            ArrayList<Deuda> deudasFiltradas = new ArrayList<>();
            if (Objects.equals(selecciontipo, "Todos")) {
                deudasFiltradas.addAll(deudasConvertidas);
            } else {
                int month = spinner.getSelectedItemPosition();
                for (Deuda deuda : deudasConvertidas) {
                    if (deuda.getFecha().getMonthValue() == month) {
                        deudasFiltradas.add(deuda);
                    }
                }
            }

            handler.post(() -> {
                layout.removeAllViews();
                crearBotones(deudasFiltradas);
            });
        });
    }

    public void irASegundoLayout(View view) {
        Intent intent = new Intent(this, CrearDeudaActivity.class);
        startActivity(intent);
    }

    private void crearBotones(ArrayList<Deuda> deudas) {
        for (Deuda deuda : deudas) {
            Button button = new Button(this);
            button.setId(View.generateViewId());
            button.setText(deuda.getEmpresa() + " - " + deuda.getTipo());
            button.setTag(deuda.getId());
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            button.setAllCaps(false);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 10, 25, 0);
            button.setLayoutParams(layoutParams);

            LocalDate inicioSemana = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate finSemana = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            if (deuda.getEstado().equals("Pagado")) {
                // 🟡 AMARILLO - Deuda pagada
                button.setTextColor(Color.BLACK);
                button.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_orange_light));
            } else if (deuda.getFecha().isBefore(LocalDate.now())) {
                // 🔴 ROJO OSCURO - Deuda vencida (ya pasó la fecha)
                button.setTextColor(Color.WHITE);
                button.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
            } else if (!deuda.getFecha().isBefore(inicioSemana) && !deuda.getFecha().isAfter(finSemana)) {
                // 🟠 ROJO CLARO - Deuda de esta semana (próxima a vencer)
                button.setTextColor(Color.WHITE);
                button.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));
            } else {
                // 🔵 AZUL - Deuda futura (más de una semana)
                button.setTextColor(Color.WHITE);
                button.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
            }

            button.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics()));
            layout.addView(button);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DeudasActivity.this, ActualizarDeudaActivity.class);
                    intent.putExtra("id", button.getTag().toString());
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}