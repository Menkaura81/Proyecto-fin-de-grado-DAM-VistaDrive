package com.menkaura.vistadrive.actividades;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.menkaura.vistadrive.databinding.ActivityMainBinding;
import com.menkaura.vistadrive.servicios.LocationCheckService;


/**
 * Actividad principal de la app. Es una Single Activity que usa un NavHostFragment
 * para manejar la navegacion entre los distintos fragmentos (RecyclerFragment,
 * DetailsFragment, NavigateFragment, etc.).
 *
 * <p>Cuando se destruye la actividad, limpia todos los datos de navegacion y geofences
 * que pudieran quedar en SharedPreferences.</p>
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Se ejecuta al crear la actividad. Infla el layout con ViewBinding.
     *
     * @param savedInstanceState datos de la instancia anterior
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**
     * Se ejecuta cuando la actividad se destruye. Limpia todos los datos
     * de navegacion
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        limpiarDatosNavegacion();
    }

    /**
     * Limpia todos los SharedPreferences relacionados con la navegacion y geofences.
     * También para el LocationCheckService sí está corriendo.
     */
    private void limpiarDatosNavegacion() {
        // Limpiar geofences pendientes
        getSharedPreferences("GeofencePending", Context.MODE_PRIVATE).edit().clear().apply();
        // Limpiar datos de geofences
        getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE).edit().clear().apply();
        // Limpiar estado de ruta
        getSharedPreferences("nav_state", Context.MODE_PRIVATE).edit().clear().apply();
        // Parar el servicio de verificacion de ubicacion
        stopService(new Intent(this, LocationCheckService.class));
    }
}
