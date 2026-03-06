package com.menkaura.vistadrive.servicios;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Helper para verificar la ortografia de textos usando la API de LanguageTool.
 * Detecta errores ortograficos (especialmente tildes) y devuelve
 * el texto corregido.
 */
public class SpellCheckHelper {

    private static final String TAG = "Debug/SpellCheckHelper";
    private static final String LANGUAGETOOL_URL = "https://api.languagetool.org/v2/check";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Callback para recibir el resultado de la verificacion ortografica.
     */
    public interface SpellCheckCallback {
        /**
         * Se llama cuando la verificacion termina correctamente.
         *
         * @param textoCorregido el texto con las correcciones aplicadas
         */
        void onResultado(String textoCorregido);

        /**
         * Se llama si ocurre un error durante la verificacion.
         *
         * @param e la excepcion que ocurrio
         */
        void onError(Exception e);
    }


    /**
     * Verifica la ortografia de un texto usando.
     *
     * @param texto    el texto a verificar
     * @param callback callback para recibir el resultado
     */
    public static void verificarOrtografia(String texto, SpellCheckCallback callback) {
        if (texto == null || texto.trim().isEmpty()) {
            mainHandler.post(() -> callback.onResultado(null));
            return;
        }

        // Usar el idioma del dispositivo
        String idioma = Locale.getDefault().getLanguage();
        if (!idioma.equals("es") && !idioma.equals("en")) {
            idioma = "es"; // Fallback a espanol
        }

        RequestBody body = new FormBody.Builder()
                .add("text", texto)
                .add("language", idioma)
                .build();

        Request request = new Request.Builder()
                .url(LANGUAGETOOL_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error al conectar con LanguageTool", e);
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onError(
                                new IOException("Respuesta vacia de LanguageTool")));
                        return;
                    }

                    String jsonString = responseBody.string();
                    JSONObject json = new JSONObject(jsonString);
                    JSONArray matches = json.getJSONArray("matches");

                    if (matches.length() == 0) {
                        Log.d(TAG, "Sin correcciones para: " + texto);
                        mainHandler.post(() -> callback.onResultado(null));
                        return;
                    }

                    String textoCorregido = aplicarCorrecciones(texto, matches);
                    Log.d(TAG, "Correcciones aplicadas: \"" + texto + "\" -> \"" + textoCorregido + "\"");
                    mainHandler.post(() -> callback.onResultado(textoCorregido));

                } catch (Exception e) {
                    Log.e(TAG, "Error al parsear respuesta de LanguageTool", e);
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }


    /**
     * Aplica las correcciones sugeridas por LanguageTool al texto original.
     *
     * @param textoOriginal el texto sin corregir
     * @param matches       array de LanguageTool
     * @return el texto con las correcciones aplicadas
     */
    private static String aplicarCorrecciones(String textoOriginal, JSONArray matches) throws Exception {
        List<int[]> offsets = new ArrayList<>();
        List<String> replacements = new ArrayList<>();

        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            int offset = match.getInt("offset");
            int length = match.getInt("length");
            JSONArray reps = match.getJSONArray("replacements");

            if (reps.length() > 0) {
                offsets.add(new int[]{offset, length});
                replacements.add(reps.getJSONObject(0).getString("value"));
            }
        }

        Integer[] indices = new Integer[offsets.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        java.util.Arrays.sort(indices, (a, b) -> offsets.get(b)[0] - offsets.get(a)[0]);

        StringBuilder sb = new StringBuilder(textoOriginal);
        for (int idx : indices) {
            int offset = offsets.get(idx)[0];
            int length = offsets.get(idx)[1];
            sb.replace(offset, offset + length, replacements.get(idx));
        }

        return sb.toString();
    }
}
