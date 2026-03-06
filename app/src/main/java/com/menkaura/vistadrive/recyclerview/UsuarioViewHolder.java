package com.menkaura.vistadrive.recyclerview;

import androidx.recyclerview.widget.RecyclerView;
import com.menkaura.vistadrive.databinding.CardviewUsuarioBinding;
import com.menkaura.vistadrive.modelos.UsuarioData;


/**
 * ViewHolder para las tarjetas de usuario en el RecyclerView.
 * Muestra el alias del usuario usando ViewBinding.
 */
public class UsuarioViewHolder extends RecyclerView.ViewHolder {

    /** ViewBinding del layout */
    private final CardviewUsuarioBinding binding;

    /**
     * Constructor del ViewHolder.
     *
     * @param binding el ViewBinding de la tarjeta de usuario
     */
    public UsuarioViewHolder(CardviewUsuarioBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    /**
     * Rellena la tarjeta con los datos del usuario. Si el alias es null,
     * muestra valor por defecto.
     *
     * @param usuario los datos del usuario a mostrar
     */
    public void bind(UsuarioData usuario) {
        if (usuario != null) {
            binding.aliasUsuario.setText(usuario.getAlias() != null ? usuario.getAlias() : "Usuario");
        }
        binding.executePendingBindings();
    }
}
