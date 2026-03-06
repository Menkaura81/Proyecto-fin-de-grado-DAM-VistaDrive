package com.menkaura.vistadrive.modelos;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.maps.model.LatLng;


/**
 * Clase para representar un punto de interes (POI) de una ruta. Implementa Parcelable
 * para poder pasar los datos entre fragmentos a traves de Bundle.
 *
 * <p>Cada POI tiene un nombre, una descripcion y unas coordenadas (latitud/longitud).</p>
 */
public class POIData implements Parcelable {

    /** Nombre del punto de interes */
    private final String nombre;

    /** Descripcion del punto de interes */
    private final String descripcion;

    /** Latitud de la coordenada del POI */
    private final double latitud;

    /** Longitud de la coordenada del POI */
    private final double longitud;

    /**
     * Constructor principal de POIData.
     *
     * @param nombre      nombre del POI
     * @param descripcion descripcion del POI
     * @param latitud     latitud de la coordenada
     * @param longitud    longitud de la coordenada
     */
    public POIData(String nombre, String descripcion, double latitud, double longitud) {
        this.nombre = nombre != null ? nombre : "POI";
        this.descripcion = descripcion != null ? descripcion : "";
        this.latitud = latitud;
        this.longitud = longitud;
    }

    /**
     * Constructor para reconstruir el objeto desde un Parcel .
     *
     * @param in el Parcel con los datos serializados
     */
    protected POIData(Parcel in) {
        nombre = in.readString();
        descripcion = in.readString();
        latitud = in.readDouble();
        longitud = in.readDouble();
    }

    /**
     * Escribe los datos del objeto en un Parcel.
     *
     * @param dest  el Parcel donde se escriben los datos
     * @param flags flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nombre);
        dest.writeString(descripcion);
        dest.writeDouble(latitud);
        dest.writeDouble(longitud);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Creador necesario para la interfaz Parcelable */
    public static final Creator<POIData> CREATOR = new Creator<>() {
        @Override
        public POIData createFromParcel(Parcel in) {
            return new POIData(in);
        }

        @Override
        public POIData[] newArray(int size) {
            return new POIData[size];
        }
    };

    /**
     * Devuelve el nombre del punto de interes.
     *
     * @return el nombre del POI
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve la descripcion del punto de interes.
     *
     * @return la descripcion del POI
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Devuelve la latitud de la coordenada del POI.
     *
     * @return la latitud
     */
    public double getLatitud() {
        return latitud;
    }

    /**
     * Devuelve la longitud de la coordenada del POI.
     *
     * @return la longitud
     */
    public double getLongitud() {
        return longitud;
    }

    /**
     * Convierte las coordenadas del POI en un objeto LatLng de Google Maps.
     *
     * @return un LatLng con la latitud y longitud del POI
     */
    public LatLng toLatLng() {
        return new LatLng(latitud, longitud);
    }
}