package com.menkaura.vistadrive.fragmentos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.actividades.LoginActivity;
import com.menkaura.vistadrive.databinding.FragmentConfigBinding;
import java.util.Objects;


/**
 * Fragmento de configuracion de la app. Muestra las opciones de cerrar sesion,
 * politica de privacidad y consola de administrador.
 */
public class ConfigFragment extends Fragment {

    /** Tag para logs de debug */
    private final String TAG = "Debug/ConfigFragment";

    /** ViewBinding del layout */
    private FragmentConfigBinding binding;

    /** Instancia de Firestore para consultas */
    private FirebaseFirestore db;


    /**
     * Constructor vacio requerido por Android para los fragmentos.
     */
    public ConfigFragment() {
        // Required empty public constructor
    }


    /* **********************************************************************************************
     *                              CICLO DE VIDA DEL FRAGMENTO
     **********************************************************************************************/

    /**
     * Infla el layout, configura los listeners de las opciones y el spinner de idioma.
     * También verifica si el usuario es admin.
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentConfigBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        binding.opcionCerrarSesion.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.cerrando_sesion, Toast.LENGTH_SHORT).show();
            cerrarSesion();
        });

        binding.opcionPoliticaPrivacidad.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://menkaura81.github.io/VistaDrive/politica_privacidad.html"));
            startActivity(intent);
        });

        binding.opcionAdmin.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configFragment_to_adminFragment));

        // Verificar si el usuario es admin y solo mostrar la opcion si es true
        verificarAdmin();

        // Spinner de idioma
        configurarSpinnerIdioma();

        return view;
    }


    /**
     * Destruye la vista del fragmento.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }


    /* *********************************************************************************************
    *                                  METODOS PROPIOS
    ************************************************************************************************/

    /**
     * Configura el Spinner de idioma con las opciones Espanol e English.
     */
    private void configurarSpinnerIdioma() {
        String[] idiomas = {"Espanol", "English"};
        String[] tags = {"es", "en"}; // Tags de idioma para LocaleListCompat

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, idiomas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerIdioma.setAdapter(adapter);

        // Seleccionar el idioma actual en el Spinner
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String currentTag = locales.isEmpty() ? "es" : Objects.requireNonNull(locales.get(0)).getLanguage();
        int seleccion = "en".equals(currentTag) ? 1 : 0;
        binding.spinnerIdioma.setSelection(seleccion);

        binding.spinnerIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nuevoTag = tags[position];
                LocaleListCompat actuales = AppCompatDelegate.getApplicationLocales();
                String actual = actuales.isEmpty() ? "es" : Objects.requireNonNull(actuales.get(0)).getLanguage();

                if (!nuevoTag.equals(actual)) {
                    AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(nuevoTag));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }


    /**
     * Cierra la sesion del usuario en Firebase y redirige a LoginActivity.
     */
    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();

        // Redirigir a login limpiando la pila de actividades
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish(); // Destruir MainActivity para que no quede en la pila
    }


    /**
     * Verifica si el usuario actual es admin. Si es admin, muestra la opcion de consola
     * de administrador y su separador. Si no es admin, se quedan ocultos.
     */
    private void verificarAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("admin");
                        Log.d(TAG, "Verificando admin");
                        if (isAdmin != null && isAdmin) {
                            // Mostrar opcion de admin y su separador
                            binding.opcionAdmin.setVisibility(View.VISIBLE);
                            binding.separadorAdmin.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Si falla, no mostramos la opcion admin (fallo silencioso)
                    Log.d(TAG, "Error al verificar admin", e);
                });
    }
}
