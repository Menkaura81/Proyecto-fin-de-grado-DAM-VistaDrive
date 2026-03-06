package com.menkaura.vistadrive.modelos;


/**
 * Clase para representar un comentario de una ruta. Guarda el texto del comentario,
 * el alias del usuario que lo escribio y el ID del documento de puntuacion en Firestore.
 */
public class ComentarioData {

    /** Texto del comentario */
    private final String comentario;

    /** Alias del usuario que hizo el comentario */
    private final String alias;

    /** ID del documento de puntuaciones en Firestore */
    private final String puntuacionId;

    /**
     * Constructor de ComentarioData.
     *
     * @param comentario   texto del comentario
     * @param alias        alias del usuario que comento
     * @param puntuacionId ID del documento de puntuaciones en Firestore
     */
    public ComentarioData(String comentario, String alias, String puntuacionId) {
        this.comentario = comentario;
        this.alias = alias;
        this.puntuacionId = puntuacionId;
    }

    /**
     * Devuelve el texto del comentario.
     *
     * @return el comentario
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * Devuelve el alias del usuario que hizo el comentario.
     *
     * @return el alias del usuario
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Devuelve el ID del documento de puntuaciones en Firestore.
     *
     * @return el ID del documento de puntuaciones
     */
    public String getPuntuacionId() {
        return puntuacionId;
    }

}
