package com.menkaura.vistadrive.recyclerview;

import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.menkaura.vistadrive.databinding.CardviewRutasBinding;
import com.menkaura.vistadrive.modelos.RutaData;


/**
 * ViewHolder para las tarjetas de ruta en el RecyclerView.
 * Muestra el nombre, descripcion, creador, estrellas de puntuacion
 * y número de valoraciones de cada ruta usando ViewBinding.
 */
public class RutaViewHolder extends RecyclerView.ViewHolder {

    /** ViewBinding del layout */
    private final CardviewRutasBinding binding;

    /** Simbolo de estrella llena para la puntuacion */
    private static final String ESTRELLA_LLENA = "★";

    /** Simbolo de estrella vacia para la puntuacion */
    private static final String ESTRELLA_VACIA = "☆";

    /**
     * Constructor del ViewHolder.
     *
     * @param binding el ViewBinding de la tarjeta de ruta
     */
    public RutaViewHolder(CardviewRutasBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    /**
     * Rellena la tarjeta con los datos de la ruta. Si algún campo es null,
     * muestra valores por defecto.
     *
     * @param ruta los datos de la ruta a mostrar
     */
    public void bind(RutaData ruta) {
        if (ruta != null) {
            binding.nombreRuta.setText(ruta.getNombreRuta() != null ? ruta.getNombreRuta() : "Sin nombre");
            binding.descripcionRuta.setText(ruta.getDescripcionRuta() != null ? ruta.getDescripcionRuta() : "Sin descripcion");
            binding.creadorRuta.setText(ruta.getCreadorRuta() != null ? ruta.getCreadorRuta() : "Sin creador");

            // Mostrar estrellas según la puntuacion
            mostrarEstrellas(ruta.getPuntuacion());

            // Mostrar numero de valoraciones
            binding.numValoraciones.setText("(" + ruta.getNumValoraciones() + ")");
        }
        binding.executePendingBindings();
    }

    /**
     * Muestra las estrellas según la puntuacion. Redondea al entero más cercano
     * y pone estrellas llenas hasta ese número, el resto vacias.
     *
     * @param puntuacion la puntuacion media de la ruta
     */
    private void mostrarEstrellas(double puntuacion) {
        int estrellas = (int) Math.round(puntuacion);

        TextView[] tvEstrellas = {
                binding.estrella1,
                binding.estrella2,
                binding.estrella3,
                binding.estrella4,
                binding.estrella5
        };

        for (int i = 0; i < 5; i++) {
            if (i < estrellas) {
                tvEstrellas[i].setText(ESTRELLA_LLENA);
            } else {
                tvEstrellas[i].setText(ESTRELLA_VACIA);
            }
        }
    }
}
