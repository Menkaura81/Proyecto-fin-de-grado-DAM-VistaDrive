package com.menkaura.vistadrive.servicios;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Locale;


/**
 * Servicio para hablar mediante Text To Speech
 */
public class TTSService extends Service {

    /** Tag para logs de debug. */
    private static final String TAG = "Debug/TTS";
    /** Clave del extra del Intent que contiene el mensaje a reproducir por TTS. */
    public static final String EXTRA_MENSAJE = "mensaje";

    /** TTS. */
    private TextToSpeech tts;
    /** Mensaje pendiente de reproducir mientras TTS se inicializa. */
    private String mensajePendiente;
    /** Indica si TTS está inicializado y listo para hablar. */
    private boolean isReady = false;


    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("es", "ES"));
                isReady = true;
                Log.d(TAG, "TTS inicializado con éxito");

                // Si llegó un mensaje mientras inicializábamos, hablarlo ahora
                if (mensajePendiente != null) {
                    hablar(mensajePendiente);
                    mensajePendiente = null;
                }

                configurarListeners();
            } else {
                stopSelf();
            }
        });
    }


    /**
     * Configura el listener de progreso del TTS para detener el servicio al terminar de hablar.
     * */
    private void configurarListeners() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { stopSelf(); }
            @Override public void onError(String utteranceId) { stopSelf(); }
        });
    }


    /**
     * Inicia el servicio de TTS.
     *
     * @param intent contiene el mensaje a reproducir
     * @param flags flag
     * @param startId
     * @return START_NOT_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_MENSAJE)) {
            String mensaje = intent.getStringExtra(EXTRA_MENSAJE);
            if (isReady) {
                hablar(mensaje);
            } else {
                // Guardar para cuando el motor esté listo
                mensajePendiente = mensaje;
            }
        }
        return START_NOT_STICKY;
    }


    /**
     * Reproduce el texto mediante el motor TTS, interrumpe cualquier mensaje anterior.
     *
     * @param texto texto a reproducir
     */
    private void hablar(String texto) {
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "id_guia");
    }


    /**
     * Detener TTS.
     */
    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
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