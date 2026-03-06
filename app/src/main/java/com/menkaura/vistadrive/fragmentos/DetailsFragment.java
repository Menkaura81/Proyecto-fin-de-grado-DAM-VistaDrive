package com.menkaura.vistadrive.fragmentos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.modelos.ComentarioData;
import com.menkaura.vistadrive.modelos.RutaData;
import com.menkaura.vistadrive.recyclerview.ComentarioAdapter;
import com.menkaura.vistadrive.databinding.FragmentDetailsBinding;
import java.util.ArrayList;
import java.util.List;


/**
 * Fragmento que muestra los detalles de una ruta: nombre, descripcion, creador
 * y los comentarios de otros usuarios.
 */
public class DetailsFragment extends Fragment {

    /** Tag para logs de debug */
    private static final String TAG = "Debug/DetailsFragment";

    /** ViewBinding del layout */
    private FragmentDetailsBinding binding;

    /** La ruta que estamos mostrando, viene del Bundle como Parcelable */
    private RutaData ruta;

    /** Lista de comentarios cargados desde Firestore */
    private ArrayList<ComentarioData> comentarios;

    /** Adapter para el RecyclerView de comentarios */
    private ComentarioAdapter comentarioAdapter;

    /** Instancia de Firestore para las consultas */
    private FirebaseFirestore db;

    /** Flag que indica si el usuario actual es administrador */
    private boolean esAdmin = false;


    /** Constructor vacio requerido por el framework de Fragments. */
    public DetailsFragment() {
        // Required empty public constructor
    }


    /**
     * Infla el layout, carga los datos de la ruta desde el Bundle y configura
     * el boton "¡Vamos!".
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        // Cargar los datos de la ruta que vienen en el Bundle
        if (getArguments() != null) {
            ruta = getArguments().getParcelable("ruta");
            if (ruta != null) {
                binding.nombreRuta.setText(ruta.getNombreRuta());
                binding.descripcionRuta.setText(ruta.getDescripcionRuta());
                binding.creadorRuta.setText(ruta.getCreadorRuta());
                Log.d(TAG, "Ruta cargada: " + ruta.getNombreRuta() + " con " + ruta.getPois().size() + " POIs");
            } else {
                Log.e(TAG, "Ruta es null");
            }
        }

        // Listener del boton Vamos
        binding.botonVamos.setOnClickListener(v -> {
            if (ruta != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("ruta", ruta);

                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_detailsFragment_to_navigateFragment, bundle);

                Log.d(TAG, "Navegando con ruta: " + ruta.getNombreRuta());
            } else {
                Log.e(TAG, "No se puede navegar, ruta es null");
            }
        });

        return view;
    }


    /**
     * Se ejecuta cuando la vista ya está creada. Configura el RecyclerView de comentarios,
     * verifica si el usuario es admin, carga los comentarios y muestra el tutorial de primer uso
     * si es necesario.
     *
     * @param view la vista raiz ya creada
     * @param savedInstanceState datos guardados
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        comentarios = new ArrayList<>();

        // Configurar RecyclerView de comentarios
        comentarioAdapter = new ComentarioAdapter(comentarios);
        binding.recyclerComentarios.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerComentarios.setAdapter(comentarioAdapter);

        // Verificar si el usuario es admin
        verificarAdmin();

        // Cargar comentarios desde Firestore
        if (ruta != null && ruta.getRutaId() != null) {
            cargarComentarios();
        }

        // Tutorial
        verificarTutorial();
    }


    /* **********************************************************************************************
     * TUTORIAL DE PRIMER USO
     **********************************************************************************************/

    /**
     * Comprueba si el tutorial de este fragmento ya se mostro.
     * Si no, muestra un diálogo explicando los detalles de la ruta.
     */
    private void verificarTutorial() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("tutorial_details", false)) {
            mostrarDialogoTutorial(
                    getString(R.string.tutorial_details_titulo),
                    getString(R.string.tutorial_details_mensaje),
                    getString(R.string.tutorial_aceptar),
                    () -> prefs.edit().putBoolean("tutorial_details", true).apply()
            );
        }
    }


    /**
     * Muestra un dialogo de tutorial reutilizable con la mascota.
     *
     * @param titulo     titulo del dialogo
     * @param mensaje    mensaje del diálogo
     * @param textoBoton texto del boton
     * @param onAceptar  accion a ejecutar cuando se pulsa el boton
     */
    private void mostrarDialogoTutorial(String titulo, String mensaje, String textoBoton, Runnable onAceptar) {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_tutorial, null);

        ((TextView) dialogView.findViewById(R.id.tituloTutorial)).setText(titulo);
        ((TextView) dialogView.findViewById(R.id.mensajeTutorial)).setText(mensaje);
        ((Button) dialogView.findViewById(R.id.botonTutorial)).setText(textoBoton);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.botonTutorial).setOnClickListener(v -> {
            dialog.dismiss();
            if (onAceptar != null) onAceptar.run();
        });

        dialog.show();
    }


    /**
     * Se ejecuta cuando el fragmento se destruye.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    /**
     * Carga los comentarios de la ruta desde la coleccion "puntuaciones" de Firestore.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void cargarComentarios() {
        DocumentReference rutaRef = db.collection("rutas").document(ruta.getRutaId());

        db.collection("puntuaciones")
                .whereEqualTo("ruta", rutaRef)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    comentarios.clear();

                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No hay comentarios para esta ruta");
                        comentarioAdapter.notifyDataSetChanged();
                        return;
                    }

                    int totalDocs = querySnapshot.size();
                    // Contador con array para poder modificarlo en el listener
                    final int[] docsProcesados = {0};

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String comentarioTexto = doc.getString("comentario");
                        String puntuacionId = doc.getId();

                        // Solo procesamos si hay texto de comentario
                        if (comentarioTexto != null && !comentarioTexto.trim().isEmpty()) {
                            DocumentReference usuarioRef = doc.getDocumentReference("usuario");

                            if (usuarioRef != null) {
                                // Referencia del usuario para obtener su alias
                                usuarioRef.get().addOnSuccessListener(userDoc -> {
                                    String alias = userDoc.getString("alias");
                                    if (alias == null) {
                                        alias = "Usuario";
                                    }

                                    comentarios.add(new ComentarioData(comentarioTexto, alias, puntuacionId));

                                    docsProcesados[0]++;
                                    if (docsProcesados[0] == totalDocs) {
                                        actualizarUIComentarios();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Error obteniendo usuario", e);
                                    comentarios.add(new ComentarioData(comentarioTexto, "Usuario", puntuacionId));

                                    docsProcesados[0]++;
                                    if (docsProcesados[0] == totalDocs) {
                                        actualizarUIComentarios();
                                    }
                                });
                            } else {
                                comentarios.add(new ComentarioData(comentarioTexto, "Usuario", puntuacionId));

                                docsProcesados[0]++;
                                if (docsProcesados[0] == totalDocs) {
                                    actualizarUIComentarios();
                                }
                            }
                        } else {
                            docsProcesados[0]++;
                            if (docsProcesados[0] == totalDocs) {
                                actualizarUIComentarios();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando comentarios", e));
    }


    /**
     * Actualiza la UI con los comentarios cargados. Muestra el título de comentarios
     * si hay alguno y notifica al adapter.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void actualizarUIComentarios() {
        // El binding puede ser null si el usuario ya salio del fragmento
        if (!isAdded() || binding == null) return;
        if (!comentarios.isEmpty()) {
            binding.tituloComentarios.setVisibility(View.VISIBLE);
        }
        comentarioAdapter.notifyDataSetChanged();
        Log.d(TAG, "Comentarios cargados: " + comentarios.size());
    }


    /**
     * Verifica si el usuario actual es administrador consultando Firestore.
     */
    private void verificarAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("usuarios").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("admin");
                        if (isAdmin != null && isAdmin) {
                            esAdmin = true;
                            // Habilitar click en comentarios para borrarlos
                            comentarioAdapter.setOnItemClickListener(this::mostrarDialogoBorrar);
                            // Mostrar boton de borrar ruta
                            binding.botonBorrarRuta.setVisibility(View.VISIBLE);
                            binding.botonBorrarRuta.setOnClickListener(v -> mostrarDialogoBorrarRuta());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error verificando admin", e));
    }


    /**
     * Muestra un diálogo de confirmacion para borrar un comentario. Solo se llama si el usuario es admin.
     *
     * @param comentario el comentario a borrar
     */
    private void mostrarDialogoBorrar(ComentarioData comentario) {
        if (!esAdmin || !isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.borrar_comentario_titulo)
                .setMessage(getString(R.string.borrar_comentario_mensaje, comentario.getAlias()))
                .setPositiveButton(R.string.borrar, (dialog, which) -> {
                    // Borrar el documento de puntuaciones (que contiene el comentario)
                    db.collection("puntuaciones").document(comentario.getPuntuacionId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Comentario borrado: " + comentario.getPuntuacionId());
                                Toast.makeText(requireContext(),
                                        R.string.comentario_borrado,
                                        Toast.LENGTH_SHORT).show();
                                // Recargar comentarios para actualizar la lista
                                cargarComentarios();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error borrando comentario", e);
                                Toast.makeText(requireContext(),
                                        R.string.error_borrar_comentario,
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }


    /**
     * Muestra un dialogo de confirmacion para borrar la ruta completa. Solo se llama si el usuario es admin.
     */
    private void mostrarDialogoBorrarRuta() {
        if (!esAdmin || !isAdded() || ruta == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.borrar_ruta_titulo)
                .setMessage(R.string.borrar_ruta_mensaje)
                .setPositiveButton(R.string.borrar, (dialog, which) -> borrarRuta())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }


    /**
     * Borra la ruta completa de Firestore
     */
    private void borrarRuta() {
        String rutaId = ruta.getRutaId();
        DocumentReference rutaRef = db.collection("rutas").document(rutaId);

        db.collection("puntuaciones")
                .whereEqualTo("ruta", rutaRef)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Puntuaciones borradas: " + querySnapshot.size());
                });

        rutaRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Obtener las referencias a los POIs desde el documento de la ruta
                List<DocumentReference> poisRefs =
                        (List<DocumentReference>) documentSnapshot.get("puntos_interes");
                if (poisRefs != null) {
                    for (DocumentReference poiRef : poisRefs) {
                        poiRef.delete();
                    }
                    Log.d(TAG, "POIs borrados: " + poisRefs.size());
                }
            }

            rutaRef.delete()
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Ruta borrada: " + rutaId);
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    R.string.ruta_borrada,
                                    Toast.LENGTH_SHORT).show();
                            // Volver al RecyclerFragment
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error borrando ruta", e);
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    R.string.error_borrar_ruta,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
