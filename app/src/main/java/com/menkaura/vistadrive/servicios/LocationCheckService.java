package com.menkaura.vistadrive.servicios;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.menkaura.vistadrive.R;
import java.util.Set;


/**
 * Clase que aporta mayor precisión al sistema de geovallado de Google. Se utiliza la geovalla para activar este servicio y
 * desde aquí lanzar el TTS cuando se esté realmente cerca del punto de interes.
 */
public class LocationCheckService extends Service {

    /** Tag para logs de debug. */
    private static final String TAG = "Debug/LCS";
    /*
     * Distancia mínima para activar TTS
     *
     * Para rutas andando:
     *    - DISTANCIA_MINIMA_TTS = 15
     *    - INTERVALO_VERIFICACION = 5000
     * Para rutas en coche:
     *    - DISTANCIA_MINIMA_TTS = 25
     *    - INTERVALO_VERIFICACION = 500     *
     */
    /** Distancia minima en metros al POI para activar el TTS. */
    public static final float DISTANCIA_MINIMA_TTS = 30f;
    /** Intervalo de verificacion de ubicacion en milisegundos. */
    private static final long INTERVALO_VERIFICACION = 500;

    /** ID del canal de notificacion para el servicio en primer plano. */
    private static final String CHANNEL_ID = "LocationCheckChannel";
    /** ID de la notificacion del servicio en primer plano. */
    private static final int NOTIFICATION_ID = 1001;
    /** Cliente de ubicacion de Google Play Services. */
    private FusedLocationProviderClient fusedLocationClient;
    /** Callback que recibe las actualizaciones de ubicacion del GPS. */
    private LocationCallback locationCallback;
    /** Configuracion de las actualizaciones de ubicacion. */
    private LocationRequest locationRequest;


    /**
     * Se ejecuta cuando se crea el servicio.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========== onCreate ==========");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar LocationRequest para actualizaciones periódicas
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_VERIFICACION)
                .setMinUpdateIntervalMillis(INTERVALO_VERIFICACION / 2)
                .setWaitForAccurateLocation(false)
                .build();

        // Configurar LocationCallback para recibir actualizaciones
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    procesarNuevaUbicacion(location);
                }
            }
        };

        crearCanalNotificacion();

        // Iniciar foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, crearNotificacion(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, crearNotificacion());
        }

        Log.d(TAG, "Servicio de verificación creado como Foreground Service");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "========== onStartCommand ==========");
        Log.d(TAG, "Servicio iniciado");

        // Iniciar actualizaciones de ubicación
        iniciarActualizacionesUbicacion();

        return START_STICKY;
    }


    /** Crea el canal de notificacion requerido para el servicio en primer plano. */
    private void crearCanalNotificacion() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Verificación de ubicación",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Verifica tu proximidad a puntos de interés");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }


    /**
     * Crea la notificacion persistente del servicio en primer plano.
     *
     * @return notificacion configurada
     */
    private Notification crearNotificacion() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VistaDrive")
                .setContentText("Verificando proximidad a puntos de interés...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Cambia al icono de tu app
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }


    /**
     * Inicia las actualizaciones de ubicación usando requestLocationUpdates.Actualizaciones de GPS
     * se hacen continuas incluso con la pantalla bloqueada.
     */
    private void iniciarActualizacionesUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No hay permisos de ubicación");
            return;
        }

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );

        Log.d(TAG, "Actualizaciones de ubicación iniciadas");
    }


    /**
     * Detiene las actualizaciones de ubicación.
     */
    private void detenerActualizacionesUbicacion() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Actualizaciones de ubicación detenidas");
        }
    }


    /**
     * Procesa cada nueva ubicación recibida del GPS.
     * @param location Ubicación actual del dispositivo
     */
    private void procesarNuevaUbicacion(Location location) {
        Set<String> pendientes = GeofenceBroadcastReceiver.obtenerGeofencesPendientes(this);

        Log.d(TAG, "========== Nueva ubicación recibida ==========");
        Log.d(TAG, "Ubicación: " + location.getLatitude() + ", " + location.getLongitude());
        Log.d(TAG, "Precisión: " + location.getAccuracy() + " metros");
        Log.d(TAG, "Geofences pendientes: " + pendientes.size());

        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay geofences pendientes, deteniendo servicio");
            detenerActualizacionesUbicacion();
            stopForeground(true);
            stopSelf();
            return;
        }

        for (String id : pendientes) {
            Log.d(TAG, "  - " + id);
        }

        // Verificar distancia para cada geofence pendiente
        for (String geofenceId : pendientes) {
            verificarDistancia(geofenceId, location);
        }
    }


    /**
     * Verificar si estoy a menos de DISTANCIA_MINIMA_TTS metros del centro de la geovalla
     * @param geofenceId Id de la geovalla
     * @param miUbicacion Ubicación actual
     */
    private void verificarDistancia(String geofenceId, Location miUbicacion) {
        Log.d(TAG, "--- Verificando distancia para " + geofenceId + " ---");

        // Obtener coordenadas del centro de la geovalla
        double latGeofence = GeofenceHelper.obtenerLatitud(this, geofenceId);
        double lngGeofence = GeofenceHelper.obtenerLongitud(this, geofenceId);
        Log.d(TAG, "Mi ubicación: " + miUbicacion.getLatitude() + ", " + miUbicacion.getLongitude());

        Log.d(TAG, "Centro geofence: " + latGeofence + ", " + lngGeofence);

        // Crear Location del centro del geofence
        Location centroGeofence = new Location("");
        centroGeofence.setLatitude(latGeofence);
        centroGeofence.setLongitude(lngGeofence);

        // Calcular distancia en metros
        float distancia = miUbicacion.distanceTo(centroGeofence);

        Log.d(TAG, geofenceId + " - Distancia al centro: " + distancia + " metros");

        // Solo hablar si estoy a menos de DISTANCIA_MINIMA_TTS metros
        if (distancia <= DISTANCIA_MINIMA_TTS) {
            Log.d(TAG, "✓ Distancia válida (" + distancia + "m), activando TTS");
            String descripcion = GeofenceHelper.obtenerDescripcion(this, geofenceId);
            Log.d(TAG, "Mensaje a enviar: " + descripcion);
            hablar(descripcion);

            // Marcar como completado
            GeofenceBroadcastReceiver.limpiarGeofencePendiente(this, geofenceId);
            // Desactivar la geovalla de este POI
            GeofenceHelper.removeGeofenceStatic(this, geofenceId);
            Log.d(TAG, "Geovalla " + geofenceId + " desactivada");

            // Notificar para mostrar ruta al siguiente POI
            int poiIndex = extraerIndice(geofenceId);
            GeofenceEventManager.getInstance().notificarSalida(poiIndex);

            // Detener el servicio hasta que se active otra geovalla
            Log.d(TAG, "Deteniendo servicio tras completar " + geofenceId);
            detenerActualizacionesUbicacion();
            stopForeground(true);
            stopSelf();
        } else {
            Log.d(TAG, "Aún lejos (" + distancia + "m), esperando...");
        }
    }


    /**
     * Extrae el índice numerico del POI a partir del ID del geofence.
     *
     * @param geofenceId ID del geofence con formato "POI_N"
     * @return indice del POI, o -1 si el formato es invalido
     */
    private int extraerIndice(String geofenceId) {
        try {
            return Integer.parseInt(geofenceId.replace("POI_", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    /**
     * Activar TTS
     * @param mensaje Mensaje a decir
     */
    private void hablar(String mensaje) {
        Log.d(TAG, "Iniciando TTS con mensaje: " + mensaje);
        Intent intent = new Intent(this, TTSService.class);
        intent.putExtra(TTSService.EXTRA_MENSAJE, mensaje);
        startService(intent);
    }


    /**
     * Metodo que se ejecuta cuando se destruye la app. Asegura que detenemos el servicio.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        detenerActualizacionesUbicacion();
        Log.d(TAG, "Servicio detenido");
    }


    /**
     * Metodo que se ejecuta cuando se elimina la app de Recientes. Asegura que detenemos el servicio.
     * @param rootIntent Intent que se ejecuta cuando se elimina la app de Recientes
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "App eliminada de Recientes. Deteniendo servicio...");

        detenerActualizacionesUbicacion();
        stopForeground(true);
        stopSelf();

        super.onTaskRemoved(rootIntent);
    }


    /**
     * Metodo que se ejecuta cuando se elimina la app de Recientes. Asegura que detenemos el servicio.
     * @param intent Intent que se ejecuta cuando se elimina la app de Recientes
     * @return null
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}