package com.example.pagafon.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.pagafon.R;
import com.example.pagafon.ui.deudas.CrearDeudaActivity;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DeudaNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "deudas_channel";
    private static final String CHANNEL_NAME = "Recordatorios de Deudas";
    private static final String TAG = "DeudaNotification";

    public DeudaNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Programa una notificación para recordar una deuda
     * @param duracion Tiempo en milisegundos hasta que se muestre la notificación
     * @param data Datos de la notificación (titulo, detalle, idNoti)
     * @param tag Tag único para identificar la notificación
     */
    public static void programarNotificacion(Long duracion, Data data, String tag) {
        OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(DeudaNotificationWorker.class)
                .setInitialDelay(duracion, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .setInputData(data)
                .build();

        WorkManager.getInstance().enqueue(notificationWork);
        Log.d(TAG, "Notificación programada con tag: " + tag + " para " + duracion + "ms");
    }

    /**
     * Cancela una notificación programada por su tag
     * @param context Contexto de la aplicación
     * @param tag Tag de la notificación a cancelar
     */
    public static void cancelarNotificacion(Context context, String tag) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag);
        Log.d(TAG, "Notificación cancelada con tag: " + tag);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker ejecutándose - doWork() llamado");

        String titulo = getInputData().getString("titulo");
        String detalle = getInputData().getString("detalle");
        int idNoti = (int) getInputData().getLong("idNoti", 0);

        Log.d(TAG, "Datos recibidos - Título: " + titulo + ", Detalle: " + detalle + ", ID: " + idNoti);

        mostrarNotificacion(titulo, detalle, idNoti);
        return Result.success();
    }

    private void mostrarNotificacion(String titulo, String detalle, int idNoti) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager es null");
            return;
        }

        // URI del sonido de notificación predeterminado
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Crear canal de notificación para Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);

            if (channel == null) {
                Log.d(TAG, "Creando nuevo canal de notificaciones");
                channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );

                channel.setDescription("Notificaciones para recordar el pago de deudas");
                channel.setShowBadge(true);
                channel.enableVibration(true);
                channel.enableLights(true);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                // Configurar sonido con AudioAttributes
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build();

                channel.setSound(soundUri, audioAttributes);

                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Canal de notificaciones creado exitosamente con sonido");
            } else {
                Log.d(TAG, "Canal de notificaciones ya existe");
            }
        }

        // Intent para abrir la app al tocar la notificación
        Intent intent = new Intent(getApplicationContext(), CrearDeudaActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                idNoti,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(titulo != null ? titulo : "Recordatorio de Deuda")
                .setContentText(detalle != null ? detalle : "Tienes una deuda pendiente")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS);

        // Usar el ID proporcionado o generar uno aleatorio
        int notificationId = (idNoti != 0) ? idNoti : new Random().nextInt(8000);

        Log.d(TAG, "Mostrando notificación con ID: " + notificationId);
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notificación mostrada exitosamente");
    }
}