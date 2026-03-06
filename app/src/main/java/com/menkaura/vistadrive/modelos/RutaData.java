package com.menkaura.vistadrive.modelos;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;


/**
 * Clase para representar una ruta turistica. Implementa Parcelable para poder
 * pasar los datos entre fragmentos a traves de Bundle.
 *
 * <p>Contiene la información basica de la ruta (nombre, descripcion, creador),
 * la lista de puntos de interes (POIs) y los datos de puntuacion.</p>
 */
public class RutaData implements Parcelable {

    /** ID del documento de la ruta en Firestore */
    private final String rutaId;

    /** Nombre de la ruta */
    private final String nombreRuta;

    /** Descripcion de la ruta */
    private final String descripcionRuta;

    /** Alias del usuario que creo la ruta */
    private final String creadorRuta;

    /** Lista de puntos de interes de la ruta */
    private final ArrayList<POIData> pois;

    /** Puntuacion media de la ruta */
    private double puntuacion;

    /** Número total de valoraciones que tiene la ruta */
    private int numValoraciones;

    /**
     * Constructor principal de RutaData.
     *
     * @param rutaId          ID del documento en Firestore
     * @param nombreRuta      nombre de la ruta
     * @param descripcionRuta descripcion de la ruta
     * @param creadorRuta     alias del creador
     * @param pois            lista de puntos de interes
     */
    public RutaData(String rutaId, String nombreRuta, String descripcionRuta, String creadorRuta, ArrayList<POIData> pois) {
        this.rutaId = rutaId;
        this.nombreRuta = nombreRuta;
        this.descripcionRuta = descripcionRuta;
        this.creadorRuta = creadorRuta;
        this.pois = pois != null ? new ArrayList<>(pois) : new ArrayList<>();
        this.puntuacion = 0;
        this.numValoraciones = 0;
    }

    /**
     * Constructor para reconstruir el objeto desde un Parcel.
     *
     * @param in el Parcel con los datos serializados
     */
    protected RutaData(Parcel in) {
        rutaId = in.readString();
        nombreRuta = in.readString();
        descripcionRuta = in.readString();
        creadorRuta = in.readString();
        pois = in.createTypedArrayList(POIData.CREATOR);
        puntuacion = in.readDouble();
        numValoraciones = in.readInt();
    }

    /**
     * Escribe los datos del objeto en un Parcel para poder enviarlo entre componentes.
     *
     * @param dest  el Parcel donde se escriben los datos
     * @param flags flags adicionales (no se usan)
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rutaId);
        dest.writeString(nombreRuta);
        dest.writeString(descripcionRuta);
        dest.writeString(creadorRuta);
        dest.writeTypedList(pois);
        dest.writeDouble(puntuacion);
        dest.writeInt(numValoraciones);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Creador necesario para la interfaz Parcelable */
    public static final Creator<RutaData> CREATOR = new Creator<>() {
        @Override
        public RutaData createFromParcel(Parcel in) {
            return new RutaData(in);
        }

        @Override
        public RutaData[] newArray(int size) {
            return new RutaData[size];
        }
    };

    /**
     * Devuelve el ID del documento de la ruta en Firestore.
     *
     * @return el ID de la ruta
     */
    public String getRutaId() {
        return rutaId;
    }

    /**
     * Devuelve el nombre de la ruta.
     *
     * @return el nombre
     */
    public String getNombreRuta() {
        return nombreRuta;
    }

    /**
     * Devuelve la descripcion de la ruta.
     *
     * @return la descripcion
     */
    public String getDescripcionRuta() {
        return descripcionRuta;
    }

    /**
     * Devuelve el alias del creador de la ruta.
     *
     * @return el alias del creador
     */
    public String getCreadorRuta() {
        return creadorRuta;
    }

    /**
     * Devuelve una copia de la lista de puntos de interes.
     *
     * @return copia de la lista de POIs
     */
    public ArrayList<POIData> getPois() {
        return new ArrayList<>(pois);
    }

    /**
     * Devuelve la puntuacion media de la ruta.
     *
     * @return la puntuacion
     */
    public double getPuntuacion() {
        return puntuacion;
    }

    /**
     * Establece la puntuacion media de la ruta.
     *
     * @param puntuacion la nueva puntuacion
     */
    public void setPuntuacion(double puntuacion) {
        this.puntuacion = puntuacion;
    }

    /**
     * Devuelve el numero total de valoraciones de la ruta.
     *
     * @return el número de valoraciones
     */
    public int getNumValoraciones() {
        return numValoraciones;
    }

    /**
     * Establece el numero total de valoraciones de la ruta.
     *
     * @param numValoraciones el nuevo número de valoraciones
     */
    public void setNumValoraciones(int numValoraciones) {
        this.numValoraciones = numValoraciones;
    }
}