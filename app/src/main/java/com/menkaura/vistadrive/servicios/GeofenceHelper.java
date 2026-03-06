package com.menkaura.vistadrive.servicios;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.menkaura.vistadrive.modelos.POIData;
import java.util.ArrayList;
import java.util.List;


/**
 * Clase para crear y gestionar geofences
 */
public class GeofenceHelper {

    /** Tag para logs de debug. */
    private static final String TAG = "Debug/GeofenceHelper";
    /** Radio de las geovallas en metros. */
    private static final float GEOFENCE_RADIUS = 50;
    /** Nombre del fichero de SharedPreferences para datos de geofences. */
    private static final String PREFS_NAME = "GeofencePrefs";

    /** Contexto de la aplicacion. */
    private final Context context;
    /** Cliente de geofencing de Google Play Services. */
    private final GeofencingClient geofencingClient;
    /** PendingIntent reutilizable para las transiciones de geofence. */
    private PendingIntent geofencePendingIntent;

    /**
     * Crea una nueva instancia de GeofenceHelper.
     *
     * @param context contexto de la aplicacion
     */
    public GeofenceHelper(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }


    /**
     * Crea un geofence para un POI.
     *
     * @param id     identificador unico del geofence
     * @param lat    latitud del centro
     * @param lng    longitud del centro
     * @param radius radio en metros
     * @return el geofence configurado
     */
    public Geofence createGeofence(String id, double lat, double lng, float radius) {
        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(1000) // 1 segundos
                .build();
    }


    /**
     * Guarda la descripcion del POI en SharedPreferences.
     *
     * @param geofenceId  ID del geofence
     * @param descripcion descripcion del POI
     */
    private void guardarDescripcion(String geofenceId, String descripcion) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(geofenceId, descripcion).apply();
    }


    /**
     * Obtiene la descripcion del POI desde SharedPreferences.
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence
     * @return descripcion del POI
     */
    public static String obtenerDescripcion(Context context, String geofenceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(geofenceId, "punto de interés");
    }


    /**
     * Guarda las coordenadas del POI en SharedPreferences.
     *
     * @param geofenceId ID del geofence
     * @param lat        latitud del POI
     * @param lng        longitud del POI
     */
    private void guardarCoordenadas(String geofenceId, double lat, double lng) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(geofenceId + "_lat", String.valueOf(lat))
                .putString(geofenceId + "_lng", String.valueOf(lng))
                .apply();
    }


    /**
     * Obtiene la latitud del POI desde SharedPreferences.
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence
     * @return latitud del POI
     */
    public static double obtenerLatitud(Context context, String geofenceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String latStr = prefs.getString(geofenceId + "_lat", "0");
        return Double.parseDouble(latStr);
    }


    /**
     * Obtiene la longitud del POI desde SharedPreferences.
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence
     * @return longitud del POI
     */
    public static double obtenerLongitud(Context context, String geofenceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lngStr = prefs.getString(geofenceId + "_lng", "0");
        return Double.parseDouble(lngStr);
    }


    /**
     * Crea un GeofencingRequest a partir de la lista de geofences.
     *
     * @param geofences lista de geofences a incluir en la peticion
     * @return la peticion de geofencing configurada
     */
    private GeofencingRequest getGeofencingRequest(List<Geofence> geofences) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();
    }


    /**
     * Obtiene o crea el PendingIntent para recibir transiciones de geofence.
     *
     * @return PendingIntent configurado para el GeofenceBroadcastReceiver
     */
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        return geofencePendingIntent;
    }


    /**
     * Anade geofences para una lista de POIs.
     *
     * @param pois     lista de POIs para los que crear geofences
     * @param callback callback con resultado de la operacion
     */
    public void addGeofences(ArrayList<POIData> pois, GeofenceCallback callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No hay permisos de ubicación");
            callback.onError("No hay permisos de ubicación");
            return;
        }

        // Limpiar estado de geofences anteriores antes de añadir
        limpiarEstadoGeofences();

        List<Geofence> geofences = new ArrayList<>();

        for (int i = 0; i < pois.size(); i++) {
            POIData poi = pois.get(i);
            String geofenceId = "POI_" + i;
            String descripcion = poi.getDescripcion() != null ? poi.getDescripcion() : poi.getNombre();

            // Guardar la descripción
            guardarDescripcion(geofenceId, descripcion);

            // Guardar las coordenadas
            guardarCoordenadas(geofenceId, poi.getLatitud(), poi.getLongitud());

            // Crear geofence
            geofences.add(createGeofence(geofenceId,
                    poi.getLatitud(), poi.getLongitud(), GEOFENCE_RADIUS));
        }

        geofencingClient.addGeofences(getGeofencingRequest(geofences), getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofences añadidos exitosamente: " + geofences.size());

                    callback.onSuccess(geofences.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error añadiendo geofences", e);
                    callback.onError(e.getMessage());
                });
    }


    /**
     * Remueve un geofence específico.
     *
     * @param geofenceId ID del geofence a remover
     */
    public void removeGeofence(String geofenceId) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(geofenceId);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence removido: " + geofenceId);
                    // Limpiar datos de este geofence en SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit()
                            .remove(geofenceId)
                            .remove(geofenceId + "_lat")
                            .remove(geofenceId + "_lng")
                            .apply();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error removiendo geofence: " + geofenceId, e));
    }


    /**
     * Remueve un geofence específico (metodo estatico).
     *
     * @param context    contexto de la aplicacion
     * @param geofenceId ID del geofence a remover
     */
    public static void removeGeofenceStatic(Context context, String geofenceId) {
        GeofencingClient client = LocationServices.getGeofencingClient(context);
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(geofenceId);

        client.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence removido (static): " + geofenceId);
                    // Limpiar datos de este geofence en SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit()
                            .remove(geofenceId)
                            .remove(geofenceId + "_lat")
                            .remove(geofenceId + "_lng")
                            .apply();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error removiendo geofence (static): " + geofenceId, e));
    }


    /**
     * Remover todos los geofences
     */
    public void removeGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofences removidos");
                    // Limpiar SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().clear().apply();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error removiendo geofences", e));
    }


    /**
     * Limpia el estado de geofences anteriores (pendientes y completados)
     */
    private void limpiarEstadoGeofences() {
        SharedPreferences prefsPending = context.getSharedPreferences("GeofencePending", Context.MODE_PRIVATE);
        prefsPending.edit().clear().apply();
        Log.d(TAG, "Estado de geofences anteriores limpiado");
    }


    /** Callback para operaciones de geofencing. */
    public interface GeofenceCallback {
        /**
         * Se invoca cuando los geofences se anaden correctamente.
         *
         * @param count numero de geofences anadidos
         */
        void onSuccess(int count);
        /**
         * Se invoca cuando ocurre un error al anadir geofences.
         *
         * @param error mensaje de error
         */
        void onError(String error);
    }
}