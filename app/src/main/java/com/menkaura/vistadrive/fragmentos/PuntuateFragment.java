package com.menkaura.vistadrive.fragmentos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.TextView;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.databinding.FragmentPuntuateBinding;
import java.util.Objects;


/**
 * Fragmento para puntuar una ruta después de completarla.
 */
public class PuntuateFragment extends Fragment {

    /** Tag para logs de debug */
    private static final String TAG = "Debug/PuntuateFragment";

    /** ViewBinding del layout */
    private FragmentPuntuateBinding binding;

    /** Boton para enviar la puntuacion */
    private Button botonEnviar;

    /** Simbolo de estrella llena */
    private static final String ESTRELLA_LLENA = "★";

    /** Simbolo de estrella vacia */
    private static final String ESTRELLA_VACIA = "☆";

    /** TextView de la estrella 1. */
    private TextView estrella1;
    /** TextView de la estrella 2. */
    private TextView estrella2;
    /** TextView de la estrella 3. */
    private TextView estrella3;
    /** TextView de la estrella 4. */
    private TextView estrella4;
    /** TextView de la estrella 5. */
    private TextView estrella5;

    /** Puntuacion seleccionada por el usuario */
    private int puntuacion = 0;

    /** ID de la ruta que estamos puntuando */
    private String rutaId;

    /** Número de POIs que el usuario realmente ha visitado */
    private int poisCompletados;

    /** Numero total de POIs de la ruta */
    private int poisTotales;

    /** Instancia de Firestore */
    private FirebaseFirestore db;


    /** Constructor vacio requerido por el framework de Fragments. */
    public PuntuateFragment() {
        // Required empty public constructor
    }


    /**
     * Se ejecuta al crear el fragmento. Obtiene los datos del Bundle y configura el boton back
     * con un diálogo de confirmacion para no salir sin puntuar accidentalmente.
     *
     * @param savedInstanceState datos guardados
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Obtener datos del bundle
        if (getArguments() != null) {
            rutaId = getArguments().getString("rutaId");
            // Si el usuario completo TODOS los POIs de la ruta, no se muestra el diálogo
            boolean rutaCompletada = getArguments().getBoolean("rutaCompletada", false);
            poisCompletados = getArguments().getInt("poisCompletados", 0);
            poisTotales = getArguments().getInt("poisTotales", 0);

            Log.d(TAG, "Datos recibidos - rutaId: " + rutaId +
                    ", completada: " + rutaCompletada +
                    ", POIs: " + poisCompletados + "/" + poisTotales);
        }

        // Control del boton volver
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isAdded() || getContext() == null) return;
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.salir_sin_puntuar_titulo)
                        .setMessage(R.string.salir_sin_puntuar_mensaje)
                        .setPositiveButton(R.string.si, (dialog, which) -> {
                            limpiarEstadoNavegacion();
                            navegarAInicio();
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });
    }


    /**
     * Limpia el estado de navegacion guardado en SharedPreferences.
     */
    private void limpiarEstadoNavegacion() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("nav_state", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "nav_state limpiado (back pressed)");
    }


    /**
     * Se ejecuta al destruir el fragmento. Limpia el binding.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    /**
     * Infla el layout, configura los listeners de las estrellas y del boton enviar.
     * Si el usuario no completo todos los POIs, muestra un diálogo de aviso.
     * Si los completo todos, guarda la ruta como terminada directamente.
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPuntuateBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        botonEnviar = binding.botonEnviar;
        estrella1 = binding.estrella1;
        estrella2 = binding.estrella2;
        estrella3 = binding.estrella3;
        estrella4 = binding.estrella4;
        estrella5 = binding.estrella5;

        // Listener del boton enviar
        botonEnviar.setOnClickListener(v -> {
            if (puntuacion > 0) {
                String comentario = Objects.requireNonNull(binding.etComentario.getText()).toString().trim();
                if (!validarComentario(comentario)) {
                    Toast.makeText(getContext(), R.string.error_comentario_largo, Toast.LENGTH_SHORT).show();
                    return;
                }
                enviarPuntuacion(comentario);
            } else {
                Toast.makeText(getContext(), "Por favor, selecciona una puntuacion", Toast.LENGTH_SHORT).show();
            }
        });

        // Cada estrella actualiza la puntuacion al número correspondiente
        estrella1.setOnClickListener(v -> actualizarPuntuacion(1));
        estrella2.setOnClickListener(v -> actualizarPuntuacion(2));
        estrella3.setOnClickListener(v -> actualizarPuntuacion(3));
        estrella4.setOnClickListener(v -> actualizarPuntuacion(4));
        estrella5.setOnClickListener(v -> actualizarPuntuacion(5));

        // Si quedan POIs pendientes, avisar al usuario si no guardar la ruta
        if (poisCompletados < poisTotales) {
            mostrarDialogoPoisPendientes();
        } else {
            guardarRutaTerminada();
        }

        return view;
    }


    /**
     * Actualiza la puntuacion seleccionada y cambia visualmente las estrellas.
     *
     * @param nuevaPuntuacion la puntuacion seleccionada (1-5)
     */
    private void actualizarPuntuacion(int nuevaPuntuacion) {
        puntuacion = nuevaPuntuacion;

        // Recorrer las 5 estrellas y poner llena o vacia según la puntuacion
        TextView[] estrellas = {estrella1, estrella2, estrella3, estrella4, estrella5};
        for (int i = 0; i < 5; i++) {
            estrellas[i].setText(i < nuevaPuntuacion ? ESTRELLA_LLENA : ESTRELLA_VACIA);
        }
    }


    /**
     * Envia la puntuacion a Firestore. Busca si ya existe un documento de
     * puntuaciones para esta ruta. Si existe, actualiza la nota. Si no existe, crea uno nuevo.
     *
     * @param comentario texto del comentario (puede estar vacio)
     */
    private void enviarPuntuacion(String comentario) {
        if (rutaId == null || rutaId.isEmpty()) {
            Log.e(TAG, "RutaId es null o vacio");
            Toast.makeText(getContext(), "Error: no se puede identificar la ruta", Toast.LENGTH_SHORT).show();
            navegarAInicio();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Usuario no autenticado");
            Toast.makeText(getContext(), "Error: usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference usuarioRef = db.collection("usuarios").document(user.getUid());

        Log.d(TAG, "Usuario - UID: " + user.getUid() +
                ", Ref: " + usuarioRef.getPath());

        // Deshabilitar boton mientras se procesa para evitar doble envío
        botonEnviar.setEnabled(false);
        botonEnviar.setText(R.string.enviando);

        // Crear referencia a la ruta
        DocumentReference rutaRef = db.collection("rutas").document(rutaId);

        // Buscar si ya existe un documento de puntuaciones para esta ruta
        db.collection("puntuaciones")
                .whereEqualTo("ruta", rutaRef)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No existe, crear uno nuevo
                        Log.w(TAG, "No existe documento de puntuaciones para esta ruta, creando uno nuevo");
                        crearNuevaPuntuacion(rutaRef, comentario, usuarioRef);
                        return;
                    }

                    // Si existe actualizar
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    actualizarPuntuacionExistente(doc, comentario, usuarioRef);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error buscando documento de puntuaciones", e);
                    Toast.makeText(getContext(), "Error al buscar puntuaciones", Toast.LENGTH_SHORT).show();
                    botonEnviar.setEnabled(true);
                    botonEnviar.setText(R.string.enviar);
                });
    }


    /**
     * Crea un nuevo documento de puntuacion en Firestore para una ruta que
     * no tenía ninguna valoracion previa.
     *
     * @param rutaRef     referencia al documento de la ruta
     * @param comentario  texto del comentario
     * @param usuarioRef  referencia al documento del usuario
     */
    private void crearNuevaPuntuacion(DocumentReference rutaRef, String comentario,
                                       DocumentReference usuarioRef) {
        db.collection("puntuaciones")
                .add(new java.util.HashMap<String, Object>() {{
                    put("ruta", rutaRef);
                    put("puntuacion", (double) puntuacion);
                    put("numero_valoraciones", 1L);
                    put("comentario", comentario);
                    put("usuario", usuarioRef);
                }})
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Nueva puntuacion creada: " + puntuacion + " estrellas");
                    Toast.makeText(getContext(), "Puntuacion enviada: " + puntuacion + " estrellas", Toast.LENGTH_SHORT).show();
                    navegarAInicio();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creando puntuacion", e);
                    Toast.makeText(getContext(), "Error al enviar puntuacion", Toast.LENGTH_SHORT).show();
                    botonEnviar.setEnabled(true);
                    botonEnviar.setText(R.string.enviar);
                });
    }


    /**
     * Actualiza un documento de puntuacion existente en Firestore.
     *
     * @param doc         el documento de puntuacion existente
     * @param comentario  texto del comentario
     * @param usuarioRef  referencia al documento del usuario
     */
    private void actualizarPuntuacionExistente(DocumentSnapshot doc, String comentario,
                                                DocumentReference usuarioRef) {
        String puntuacionDocId = doc.getId();

        // Leer valores actuales
        Double puntuacionActual = doc.getDouble("puntuacion");
        Long numValoraciones = doc.getLong("numero_valoraciones");

        if (puntuacionActual == null) puntuacionActual = 0.0;
        if (numValoraciones == null) numValoraciones = 0L;

        Log.d(TAG, "Puntuacion actual: " + puntuacionActual + ", Valoraciones: " + numValoraciones);

        // Calcular nueva puntuacion con promedio ponderado
        double nuevaPuntuacion = ((puntuacionActual * numValoraciones) + puntuacion) / (numValoraciones + 1);
        long nuevoNumValoraciones = numValoraciones + 1;

        Log.d(TAG, "Nueva puntuacion calculada: " + nuevaPuntuacion + ", Nuevas valoraciones: " + nuevoNumValoraciones);

        // Actualizar el documento en Firestore
        db.collection("puntuaciones").document(puntuacionDocId)
                .update(
                        "puntuacion", nuevaPuntuacion,
                        "numero_valoraciones", nuevoNumValoraciones,
                        "comentario", comentario,
                        "usuario", usuarioRef
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Puntuacion actualizada correctamente");
                    Toast.makeText(getContext(), "Puntuacion enviada: " + puntuacion + " estrellas", Toast.LENGTH_SHORT).show();
                    navegarAInicio();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error actualizando puntuacion", e);
                    Toast.makeText(getContext(), "Error al enviar puntuacion", Toast.LENGTH_SHORT).show();
                    botonEnviar.setEnabled(true);
                    botonEnviar.setText(R.string.enviar);
                });
    }


    /**
     * Muestra un diálogo advirtiendo que quedan POIs por visitar.
     */
    private void mostrarDialogoPoisPendientes() {
        if (!isAdded() || getContext() == null) return;

        String mensaje = getString(R.string.pois_pendientes_mensaje, poisCompletados, poisTotales);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.pois_pendientes_titulo)
                .setMessage(mensaje)
                .setCancelable(false)
                .setPositiveButton(R.string.volver_a_ruta, (dialog, which) -> {
                    // Volver a NavigateFragment
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .setNegativeButton(R.string.quedarse_puntuar, (dialog, which) -> {
                    // Quedarse a puntuar y guardar ruta como terminada
                    guardarRutaTerminada();
                    dialog.dismiss();
                })
                .show();
    }


    /**
     * Guarda la ruta como terminada en Firestore. Anade la referencia de la ruta
     * al array "rutas_terminadas" del usuario y la referencia del usuario al array
     * "usuarios_terminada" de la ruta.
     */
    private void guardarRutaTerminada() {
        if (rutaId == null || rutaId.isEmpty()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DocumentReference usuarioRef = db.collection("usuarios").document(user.getUid());
        DocumentReference rutaRef = db.collection("rutas").document(rutaId);

        // Anadir ruta al array rutas_terminadas del usuario
        usuarioRef.update("rutas_terminadas", FieldValue.arrayUnion(rutaRef))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ruta anadida a rutas_terminadas del usuario"))
                .addOnFailureListener(e -> Log.e(TAG, "Error anadiendo ruta a rutas_terminadas", e));

        // Anadir usuario al array usuarios_terminada de la ruta
        rutaRef.update("usuarios_terminada", FieldValue.arrayUnion(usuarioRef))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario anadido a usuarios_terminada de la ruta"))
                .addOnFailureListener(e -> Log.e(TAG, "Error anadiendo usuario a usuarios_terminada", e));
    }


    /**
     * Valida que el comentario no esté vacío ni tenga más de 100 caracteres.
     *
     * @param comentario texto del comentario
     * @return true si es valido, false si no lo es
     */
    private static boolean validarComentario(String comentario) {
        if (comentario == null) return true;
        return comentario.length() < 100;
    }


    /**
     * Navega a RecyclerFragment. Limpia el estado de navegacion.
     */
    private void navegarAInicio() {
        if (!isAdded() || getView() == null) return;

        // Limpiar estado de navegacion
        SharedPreferences prefs = requireContext().getSharedPreferences("nav_state", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "nav_state limpiado");

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_puntuateFragment_to_recyclerFragment2);
    }


    /* *********************************************************************************************
     * PUNTOS DE ENTRADA PARA TEST
     **********************************************************************************************/

    /** Punto de entrada público para tests: delega en validarComentarioImpl. */
    public static boolean TestValidarComentario(String comentario) {
        return validarComentario(comentario);
    }
}
