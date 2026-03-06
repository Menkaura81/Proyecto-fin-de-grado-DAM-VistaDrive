package com.menkaura.vistadrive.recyclerview;

import androidx.recyclerview.widget.RecyclerView;
import com.menkaura.vistadrive.databinding.CardviewComentarioBinding;
import com.menkaura.vistadrive.modelos.ComentarioData;


/**
 * ViewHolder para las tarjetas de comentarios en el RecyclerView.
 * Muestra el alias del usuario y el texto del comentario usando ViewBinding.
 */
public class ComentarioViewHolder extends RecyclerView.ViewHolder {

    /** ViewBinding del layout  */
    private final CardviewComentarioBinding binding;

    /**
     * Constructor del ViewHolder.
     *
     * @param binding el ViewBinding de la tarjeta de comentario
     */
    public ComentarioViewHolder(CardviewComentarioBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    /**
     * Rellena la tarjeta con los datos del comentario. Si el alias o el texto
     * son null, muestra valores por defecto
     *
     * @param comentario los datos del comentario a mostrar
     */
    public void bind(ComentarioData comentario) {
        if (comentario != null) {
            binding.aliasUsuario.setText(comentario.getAlias() != null ? comentario.getAlias() : "Usuario");
            binding.textoComentario.setText(comentario.getComentario() != null ? comentario.getComentario() : "");
        }
        binding.executePendingBindings();
    }
}
