package com.menkaura.vistadrive.servicios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Clase para tratar geofences
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    /** Tag para logs de debug. */
    private static final String TAG = "Debug/GeofenceReceiver";
    /** Nombre del fichero de SharedPreferences para geofences pendientes y completados. */
    private static final String PREFS_PENDING = "GeofencePending";
    /** Clave del StringSet de geofences pendientes en SharedPreferences. */
    private static final String KEY_PENDING_GEOFENCES = "pending_geofences";
    /** Clave del StringSet de geofences completados en SharedPreferences. */
    private static final String KEY_COMPLETED_GEOFENCES = "completed_geofences";

    /** Accion de broadcast para notificar al fragmento cuando el usuario sale de una geovalla. */
    public static final String ACTION_GEOFENCE_EXIT = "com.menkaura.vistadrive.GEOFENCE_EXIT";
    /** Extra del broadcast que contiene el índice del POI cuya geovalla se ha abandonado. */
    public static final String EXTRA_GEOFENCE_INDEX = "geofence_index";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "========== onReceive llamado ==========");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent es null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Error en geofence: " + errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.d(TAG, "Tipo de transición: " + geofenceTransition);

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "Transición: ENTER");
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                Geofence geofence = triggeringGeofences.get(0);
                String geofenceId = geofence.getRequestId();

                Log.d(TAG, "Geofence ENTER detectado: " + geofenceId);

                // Marcar este geofence como pendiente
                marcarGeofencePendiente(context, geofenceId);

                // Iniciar servicio de verificación continua
                Intent serviceIntent = new Intent(context, LocationCheckService.class);
                serviceIntent.putExtra("geofence_id", geofenceId);

                context.startForegroundService(serviceIntent);

                Log.d(TAG, "Servicio LocationCheckService iniciado");
            }

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Transición: EXIT");
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                String geofenceId = triggeringGeofences.get(0).getRequestId();
                Log.d(TAG, "Geofence EXIT detectado: " + geofenceId);

                // Desmarcar como pendiente
                limpiarGeofencePendiente(context, geofenceId);

                // Marcar como completado
                marcarGeofenceCompletado(context, geofenceId);
            }

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.d(TAG, "Transición: DWELL (permanencia)");
        }
    }


    /**
     * Marca un geofence como pendiente en SharedPreferences.
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence a marcar
     */
    private void marcarGeofencePendiente(Context context, String geofenceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE);
        Set<String> pendientes = prefs.getStringSet(KEY_PENDING_GEOFENCES, new HashSet<>());
        Set<String> nuevos = new HashSet<>(pendientes);
        nuevos.add(geofenceId);
        prefs.edit().putStringSet(KEY_PENDING_GEOFENCES, nuevos).apply();
        Log.d(TAG, "Geofence marcado como pendiente: " + geofenceId);
    }


    /**
     * Marca un geofence como completado en SharedPreferences.
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence completado
     */
    private void marcarGeofenceCompletado(Context context, String geofenceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE);
        Set<String> completados = prefs.getStringSet(KEY_COMPLETED_GEOFENCES, new HashSet<>());
        Set<String> nuevos = new HashSet<>(completados);
        nuevos.add(geofenceId);
        prefs.edit().putStringSet(KEY_COMPLETED_GEOFENCES, nuevos).apply();
        Log.d(TAG, "Geofence marcado como completado: " + geofenceId);
    }


    /**
     * Extrae el índice numerico del POI a partir del ID del geofence
     *
     * @param geofenceId ID del geofence c
     * @return indice del POI
     */
    private int extraerIndice(String geofenceId) {
        // Extraer el número de POI
        try {
            return Integer.parseInt(geofenceId.replace("POI_", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    /**
     * Notifica la salida de un geofence.
     *
     * @param context  contexto de la aplicacion
     * @param poiIndex índice del POI cuya geovalla se ha completado
     */
    private void notificarSalida(Context context, int poiIndex) {
        // Primera vez
        GeofenceEventManager.getInstance().notificarSalida(poiIndex);

        // Respaldo. Daba problemas
        Intent broadcastIntent = new Intent(ACTION_GEOFENCE_EXIT);
        broadcastIntent.putExtra(EXTRA_GEOFENCE_INDEX, poiIndex);
        broadcastIntent.setPackage(context.getPackageName());
        context.sendBroadcast(broadcastIntent);

        Log.d(TAG, "Broadcast enviado: POI_" + poiIndex + " completado");
    }


    /**
     * Obtiene el conjunto de IDs de geofences que están pendientes de completar.
     *
     * @param context contexto de la aplicacion
     * @return conjunto de IDs de geofences pendientes
     */
    public static Set<String> obtenerGeofencesPendientes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_PENDING_GEOFENCES, new HashSet<>()));
    }


    /**
     * Elimina un geofence del conjunto de pendientes.
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence a limpiar
     */
    public static void limpiarGeofencePendiente(Context context, String geofenceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE);
        Set<String> pendientes = prefs.getStringSet(KEY_PENDING_GEOFENCES, new HashSet<>());
        Set<String> nuevos = new HashSet<>(pendientes);
        nuevos.remove(geofenceId);
        prefs.edit().putStringSet(KEY_PENDING_GEOFENCES, nuevos).apply();
    }
}
