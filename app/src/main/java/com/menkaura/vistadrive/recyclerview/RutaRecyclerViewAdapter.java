package com.menkaura.vistadrive.recyclerview;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.menkaura.vistadrive.databinding.CardviewRutasBinding;
import com.menkaura.vistadrive.modelos.RutaData;
import java.util.ArrayList;


/**
 * Adapter del RecyclerView de rutas. Se usa en RecyclerFragment para mostrar
 * la lista de rutas disponibles ordenadas por distancia al usuario.
 */
public class RutaRecyclerViewAdapter extends RecyclerView.Adapter<RutaViewHolder> {

    /** Lista de rutas a mostrar */
    private final ArrayList<RutaData> rutas;

    /** Listener para cuando se hace clic en una ruta */
    private final OnRutaClickListener listener;

    /**
     * Interfaz para manejar clics en las rutas.
     */
    public interface OnRutaClickListener {
        /**
         * Se ejecuta cuando se hace click en una ruta.
         *
         * @param ruta la ruta que se ha pulsado
         * @param view la vista del item pulsado
         */
        void onRutaClick(RutaData ruta, android.view.View view);
    }

    /**
     * Constructor del adapter.
     *
     * @param rutas    lista de rutas a mostrar
     * @param listener listener para manejar clicks en las rutas
     */
    public RutaRecyclerViewAdapter(ArrayList<RutaData> rutas, OnRutaClickListener listener) {
        this.rutas = rutas;
        this.listener = listener;
    }

    /**
     * Crea un nuevo ViewHolder inflando el layout de la tarjeta de ruta.
     *
     * @param parent   contenedor padre
     * @param viewType tipo de vista
     * @return un nuevo RutaViewHolder
     */
    @NonNull
    @Override
    public RutaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardviewRutasBinding binding = CardviewRutasBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new RutaViewHolder(binding);
    }

    /**
     * Enlaza los datos de una ruta al ViewHolder y configura el listener de click.
     *
     * @param holder   el ViewHolder donde mostrar los datos
     * @param position la posicion de la ruta en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull RutaViewHolder holder, int position) {
        RutaData rutaActual = rutas.get(position);
        holder.bind(rutaActual);

        // Listener de click en el item
        holder.itemView.setOnClickListener(view -> listener.onRutaClick(rutaActual, view));
    }

    /**
     * Devuelve el numero total de rutas en la lista.
     *
     * @return el tamanio de la lista de rutas
     */
    @Override
    public int getItemCount() {
        return rutas.size();
    }
}
