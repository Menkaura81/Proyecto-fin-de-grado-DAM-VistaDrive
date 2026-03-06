package com.menkaura.vistadrive.fragmentos;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.databinding.FragmentAdminBinding;
import com.menkaura.vistadrive.modelos.UsuarioData;
import com.menkaura.vistadrive.recyclerview.UsuarioAdapter;
import java.util.ArrayList;


/**
 * Fragmento de la consola de administrador. Muestra una lista con todos los
 * usuarios registrados y permite bloquearlos. Solo se puede acceder desde
 * ConfigFragment si el usuario actual tiene admin=true en Firestore.
 */
public class AdminFragment extends Fragment {

    /** ViewBinding del layout */
    private FragmentAdminBinding binding;

    /** Lista de usuarios cargados desde Firestore */
    private final ArrayList<UsuarioData> listaUsuarios = new ArrayList<>();

    /** Adapter para el RecyclerView de usuarios */
    private UsuarioAdapter adapter;

    /** Constructor vacio requerido por el framework de Fragments. */
    public AdminFragment() {
        // Required empty public constructor
    }


    /**
     * Infla el layout del fragmento.
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Se ejecuta cuando la vista esta creada. Configura el RecyclerView de usuarios
     * con el adapter y carga la lista de usuarios desde Firestore.
     *
     * @param view la vista raiz
     * @param savedInstanceState datos guardados
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configurar listener de click para bloquear usuarios
        adapter = new UsuarioAdapter(listaUsuarios, this::mostrarDialogoBloqueo);
        binding.recyclerUsuarios.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerUsuarios.setAdapter(adapter);

        cargarUsuarios();
    }


    /**
     * Carga todos los usuarios desde la coleccion "usuarios" de Firestore.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void cargarUsuarios() {
        FirebaseFirestore.getInstance().collection("usuarios")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listaUsuarios.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String uid = doc.getId();
                        String alias = doc.getString("alias");

                        // Comprobar el campo admin. Hay 2 comprobaciones porque al principio la lie con el tipo
                        Object adminField = doc.get("admin");
                        boolean admin = false;
                        if (adminField instanceof Boolean) {
                            admin = (Boolean) adminField;
                        } else if (adminField instanceof Number) {
                            admin = ((Number) adminField).intValue() != 0;
                        }
                        listaUsuarios.add(new UsuarioData(uid, alias, admin));
                    }
                    adapter.notifyDataSetChanged();
                });
    }


    /**
     * Muestra un dialogo de confirmacion para bloquear a un usuario.
     *
     * @param usuario el usuario a bloquear
     */
    private void mostrarDialogoBloqueo(UsuarioData usuario) {
        new AlertDialog.Builder(requireContext())
                .setTitle(usuario.getAlias())
                .setPositiveButton(R.string.bloquear_usuario, (dialog, which) -> bloquearUsuario(usuario))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    /**
     * Bloquea a un usuario poniendo bloqueado=true en su documento de Firestore.
     *
     * @param usuario el usuario a bloquear
     */
    private void bloquearUsuario(UsuarioData usuario) {
        FirebaseFirestore.getInstance().collection("usuarios")
                .document(usuario.getUid())
                .update("bloqueado", true)
                .addOnSuccessListener(unused -> Toast.makeText(getContext(), R.string.usuario_bloqueado, Toast.LENGTH_SHORT).show());
    }


    /**
     * Destruye la vista del fragmento.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
