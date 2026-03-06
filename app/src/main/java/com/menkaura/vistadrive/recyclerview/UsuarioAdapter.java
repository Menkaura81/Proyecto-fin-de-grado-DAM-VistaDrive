package com.menkaura.vistadrive.recyclerview;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.menkaura.vistadrive.databinding.CardviewUsuarioBinding;
import com.menkaura.vistadrive.modelos.UsuarioData;
import java.util.ArrayList;


/**
 * Adapter del RecyclerView de usuarios
 */
public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioViewHolder> {

    /** Lista de usuarios a mostrar */
    private final ArrayList<UsuarioData> usuarios;

    /** Listener para cuando se hace click en un usuario */
    private final OnUsuarioClickListener listener;

    /**
     * Interfaz para manejar clics en los usuarios.
     */
    public interface OnUsuarioClickListener {
        /**
         * Se ejecuta cuando se hace clic en un usuario.
         *
         * @param usuario el usuario que se ha pulsado
         */
        void onUsuarioClick(UsuarioData usuario);
    }

    /**
     * Constructor del adapter.
     *
     * @param usuarios lista de usuarios a mostrar
     * @param listener listener para manejar clics en los usuarios
     */
    public UsuarioAdapter(ArrayList<UsuarioData> usuarios, OnUsuarioClickListener listener) {
        this.usuarios = usuarios;
        this.listener = listener;
    }

    /**
     * Crea un nuevo ViewHolder inflando el layout de la tarjeta de usuario.
     *
     * @param parent   contenedor padre
     * @param viewType tipo de vista (no se usa, solo hay un tipo)
     * @return un nuevo UsuarioViewHolder
     */
    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardviewUsuarioBinding binding = CardviewUsuarioBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new UsuarioViewHolder(binding);
    }

    /**
     * Enlaza los datos de un usuario al ViewHolder y configura el listener de click.
     *
     * @param holder   el ViewHolder donde mostrar los datos
     * @param position la posicion del usuario en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        UsuarioData usuarioActual = usuarios.get(position);
        holder.bind(usuarioActual);

        holder.itemView.setOnClickListener(view -> listener.onUsuarioClick(usuarioActual));
    }

    /**
     * Devuelve el número total de usuarios en la lista.
     *
     * @return el tamanio de la lista de usuarios
     */
    @Override
    public int getItemCount() {
        return usuarios.size();
    }
}
