package com.menkaura.vistadrive.servicios;

import android.util.Log;


/**
 * Clase para notificar al fragmento de la salida de un POI
 */
public class GeofenceEventManager {

    /** Tag para logs de debug. */
    private static final String TAG = "Debug/GeofenceEventManager";
    /** Instancia del singleton. */
    private static GeofenceEventManager instance;
    /** Listener registrado para recibir eventos de salida de geovalla. */
    private GeofenceExitListener listener;

    /**
     * Obtiene la instancia unica del GeofenceEventManager.
     *
     * @return instancia del GeofenceEventManager
     */
    public static GeofenceEventManager getInstance() {
        if (instance == null) {
            instance = new GeofenceEventManager();
        }
        return instance;
    }


    /**
     * Registra un listener para recibir eventos de salida de geovalla.
     *
     * @param listener listener a registrar
     */
    public void setListener(GeofenceExitListener listener) {
        this.listener = listener;
        Log.d(TAG, "Listener registrado");
    }


    /** Elimina el listener registrado. */
    public void removeListener() {
        this.listener = null;
        Log.d(TAG, "Listener removido");
    }


    /**
     * Notifica al listener que el usuario ha salido de la geovalla de un POI.
     *
     * @param poiIndex indice del POI
     */
    public void notificarSalida(int poiIndex) {
        Log.d(TAG, "notificarSalida llamado para POI_" + poiIndex);
        if (listener != null) {
            Log.d(TAG, "✓ Notificando al listener");
            listener.onGeofenceExit(poiIndex);
        } else {
            Log.w(TAG, "✗ No hay listener registrado");
        }
    }


    /** Listener para eventos de salida de geovalla. */
    public interface GeofenceExitListener {
        /**
         * Se invoca cuando el usuario sale de la geovalla de un POI.
         *
         * @param poiIndex indice del POI
         */
        void onGeofenceExit(int poiIndex);
    }
}