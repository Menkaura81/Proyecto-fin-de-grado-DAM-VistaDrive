package com.menkaura.vistadrive.recyclerview;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.menkaura.vistadrive.databinding.CardviewComentarioBinding;
import com.menkaura.vistadrive.modelos.ComentarioData;
import java.util.ArrayList;


/**
 * Adapter del RecyclerView de comentarios. Se usa en DetailsFragment para
 * mostrar los comentarios que los usuarios han dejado en una ruta.
 *
 * <p>Tiene un listener de clic opcional que se activa
 * solo para administradores, permitiendo borrar comentarios.</p>
 */
public class ComentarioAdapter extends RecyclerView.Adapter<ComentarioViewHolder> {

    /** Lista de comentarios a mostrar */
    private final ArrayList<ComentarioData> comentarios;

    /** Listener para cuando se hace clic en un comentario */
    private OnItemClickListener listener;

    /**
     * Interfaz para manejar clics en los comentarios.
     */
    public interface OnItemClickListener {
        /**
         * Se ejecuta cuando se hace clic en un comentario.
         *
         * @param comentario el comentario que se ha pulsado
         */
        void onItemClick(ComentarioData comentario);
    }

    /**
     * Constructor del adapter.
     *
     * @param comentarios lista de comentarios a mostrar
     */
    public ComentarioAdapter(ArrayList<ComentarioData> comentarios) {
        this.comentarios = comentarios;
    }

    /**
     * Asigna el listener de clic. Si es null, los clicks no hacen nada.
     *
     * @param listener el listener a asignar
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Crea un nuevo ViewHolder inflando el layout de la tarjeta de comentario.
     *
     * @param parent   contenedor padre
     * @param viewType tipo de vista
     * @return un nuevo ComentarioViewHolder
     */
    @NonNull
    @Override
    public ComentarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardviewComentarioBinding binding = CardviewComentarioBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ComentarioViewHolder(binding);
    }

    /**
     * Enlaza los datos de un comentario al ViewHolder y configura el listener de click.
     *
     * @param holder   el ViewHolder donde mostrar los datos
     * @param position la posicion del comentario en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull ComentarioViewHolder holder, int position) {
        ComentarioData comentarioActual = comentarios.get(position);
        holder.bind(comentarioActual);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(comentarioActual);
            }
        });
    }

    /**
     * Devuelve el número total de comentarios en la lista.
     *
     * @return el tamanio de la lista de comentarios
     */
    @Override
    public int getItemCount() {
        return comentarios.size();
    }
}
