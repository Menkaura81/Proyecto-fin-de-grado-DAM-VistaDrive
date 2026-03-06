package com.menkaura.vistadrive.modelos;


/**
 * Clase para representar un usuario de la app. Contiene el UID de Firebase,
 * el alias y si es administrador o no.
 */
public class UsuarioData {

    /** UID del usuario en Firebase Authentication */
    private final String uid;

    /** Alias del usuario */
    private final String alias;

    /** Si el usuario tiene permisos de administrador */
    private final boolean admin;

    /**
     * Constructor de UsuarioData.
     *
     * @param uid   UID del usuario en Firebase
     * @param alias alias del usuario
     * @param admin true si es administrador
     */
    public UsuarioData(String uid, String alias, boolean admin) {
        this.uid = uid;
        this.alias = alias;
        this.admin = admin;
    }

    /**
     * Devuelve el UID del usuario en Firebase.
     *
     * @return el UID
     */
    public String getUid() {
        return uid;
    }

    /**
     * Devuelve el alias del usuario.
     *
     * @return el alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Indica si el usuario es administrador.
     *
     * @return true si es admin, false si no
     */
    public boolean isAdmin() {
        return admin;
    }
}
