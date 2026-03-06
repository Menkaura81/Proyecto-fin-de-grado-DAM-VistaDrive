package com.menkaura.vistadrive.fragmentos;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.databinding.FragmentCreateBinding;
import com.menkaura.vistadrive.modelos.POIData;
import com.menkaura.vistadrive.servicios.SpellCheckHelper;
import java.util.ArrayList;


/**
 * Fragmento para crear nuevas rutas turisticas. Muestra un mapa centrado en la
 * ubicacion del usuario donde se pueden anadir puntos de interes (POIs) en la
 * posicion actual del GPS.
 */
public class CreateFragment extends Fragment implements OnMapReadyCallback {

    /** Tag para logs de debug */
    private static final String TAG = "Debug/CreateFragment";

    /** Key para guardar la lista de POIs en el Bundle */
    private static final String KEY_LISTA_POIS = "lista_pois";

    /** ViewBinding del layout */
    private FragmentCreateBinding binding;

    /** Instancia del mapa de Google */
    private GoogleMap mMap;

    /** Cliente de ubicacion para obtener posicion GPS */
    private FusedLocationProviderClient fusedLocationClient;

    /** Ubicacion actual del usuario */
    private Location ubicacionActual;

    /** Lista de POIs que el usuario ha anadido a la ruta */
    private final ArrayList<POIData> listaPOIs = new ArrayList<>();

    /** Callback para actualizaciones continuas de ubicacion */
    private LocationCallback locationCallback;

    /** Controla si la camara sigue al usuario */
    private boolean seguirUbicacion = true;

    /** Ubicacion obtenida antes de que el mapa esté listo */
    private Location ubicacionInicial = null;

    /** Flag que indica si el mapa ya esta listo */
    private boolean mapaListo = false;

    /** Flag para el primer zoom con animacion */
    private boolean primerZoomRealizado = false;

    /** Launcher para solicitar permisos de ubicacion */
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);

                if (fineLocationGranted != null && fineLocationGranted) {
                    Log.d(TAG, "Permisos de ubicacion concedidos");
                    obtenerUbicacion(location -> {
                        ubicacionInicial = location;
                        if (mapaListo) {
                            habilitarMiUbicacion();
                        }
                    });
                } else {
                    Log.w(TAG, "Permisos de ubicacion denegados");
                    Toast.makeText(requireContext(),
                            "Se necesitan permisos de ubicacion para mostrar tu posicion",
                            Toast.LENGTH_LONG).show();
                }
            });

    /** Constructor vacio requerido por el framework de Fragments. */
    public CreateFragment() {
        // Required empty public constructor
    }


    /**
     * Se ejecuta al crear el fragmento. Inicializa el cliente de ubicacion y
     * restaura los POIs guardados si vienen de un savedInstanceState.
     *
     * @param savedInstanceState datos guardados con los POIs
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Restaurar POIs guardados por si bloqueo o rotacion de la pantalla
        if (savedInstanceState != null) {
            ArrayList<POIData> poisGuardados = savedInstanceState.getParcelableArrayList(KEY_LISTA_POIS);
            if (poisGuardados != null) {
                listaPOIs.addAll(poisGuardados);
                Log.d(TAG, "POIs restaurados: " + listaPOIs.size());
            }
        }
    }


    /**
     * Guarda la lista de POIs en el Bundle para que sobreviva a la destruccion
     * del fragmento.
     *
     * @param outState Bundle donde guardar los datos
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_LISTA_POIS, listaPOIs);
    }


    /**
     * Infla el layout del fragmento.
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    /**
     * Se ejecuta cuando la vista esta creada.
     *
     * @param view la vista raiz
     * @param savedInstanceState datos guardados
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_create);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Mantener la pantalla encendida
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        solicitarPermisosUbicacion();

        if (hayPoisSuficientes(listaPOIs.size())) {
            binding.BotonGuardarRuta.setVisibility(View.VISIBLE);
        }

        // Botones
        binding.BotonAnadirPoi.setOnClickListener(v -> mostrarDialogoAnadirPoi());
        binding.BotonGuardarRuta.setOnClickListener(v -> mostrarDialogoGuardarRuta());

        // Confirmar si hay POIs creados al hacer back
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isAdded() || getContext() == null) return;
                if (hayPoisSuficientes(listaPOIs.size())) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.salir_crear_ruta_titulo)
                            .setMessage(R.string.salir_crear_ruta_mensaje)
                            .setPositiveButton(R.string.si, (dialog, which) -> {
                                setEnabled(false);
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Tutorial si es el primer uso
        verificarTutorial();
    }


    /* **********************************************************************************************
     * TUTORIAL DE PRIMER USO
     **********************************************************************************************/

    /**
     * Comprueba si el tutorial de creacion ya se mostro. Si no, lo muestra.
     */
    private void verificarTutorial() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("tutorial_create", false)) {
            mostrarDialogoTutorial(
                    getString(R.string.tutorial_create_titulo),
                    getString(R.string.tutorial_create_mensaje),
                    getString(R.string.tutorial_aceptar),
                    () -> prefs.edit().putBoolean("tutorial_create", true).apply()
            );
        }
    }


    /**
     * Muestra dialogo de tutorial
     *
     * @param titulo     titulo del dialogo
     * @param mensaje    mensaje del diálogo
     * @param textoBoton texto del boton
     * @param onAceptar  accion a ejecutar cuando se pulsa el boton
     */
    private void mostrarDialogoTutorial(String titulo, String mensaje, String textoBoton, Runnable onAceptar) {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_tutorial, null);

        ((TextView) dialogView.findViewById(R.id.tituloTutorial)).setText(titulo);
        ((TextView) dialogView.findViewById(R.id.mensajeTutorial)).setText(mensaje);
        ((Button) dialogView.findViewById(R.id.botonTutorial)).setText(textoBoton);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.botonTutorial).setOnClickListener(v -> {
            dialog.dismiss();
            if (onAceptar != null) onAceptar.run();
        });

        dialog.show();
    }


    /**
     * Se ejecuta cuando el mapa está listo. Configura los controles, restaura los
     * marcadores de POIs guardados y habilita la capa de ubicacion si hay permisos.
     *
     * @param googleMap la instancia del mapa
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mapaListo = true;

        // Configurar controles del mapa
        configurarMapa();

        // Restaurar marcadores de POIs que estaban guardados
        for (int i = 0; i < listaPOIs.size(); i++) {
            POIData poi = listaPOIs.get(i);
            LatLng poiLocation = new LatLng(poi.getLatitud(), poi.getLongitud());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(poiLocation)
                    .title(poi.getNombre()));
            if (marker != null) marker.setTag(i);
        }

        // Habilitar ubicacion en el mapa
        if (checkLocationPermission()) {
            habilitarMiUbicacion();
        }
    }


    /**
     * Configura los controles del mapa
     */
    private void configurarMapa() {
        if (mMap == null)
            return;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Desactivar seguimiento si el usuario mueve el mapa manualmente
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                seguirUbicacion = false;
                Log.d(TAG, "Seguimiento de ubicacion desactivado (gesto del usuario)");
            }
        });

        // Reactivar seguimiento al pulsar boton "Mi ubicacion"
        mMap.setOnMyLocationButtonClickListener(() -> {
            seguirUbicacion = true;
            Log.d(TAG, "Seguimiento de ubicacion activado");
            return false; // false para que se ejecute el comportamiento por defecto
        });

        // Abrir diálogo de edicion/borrado del POI
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Integer) {
                mostrarDialogoEditarPoi((int) tag);
                return true; // consumir el evento
            }
            return false;
        });
    }


    /**
     * Solicita permisos de ubicacion. Si ya los tenemos, obtiene ubicacion.
     */
    private void solicitarPermisosUbicacion() {
        if (checkLocationPermission()) {
            obtenerUbicacion(location -> {
                ubicacionInicial = location;
                if (mapaListo) {
                    habilitarMiUbicacion();
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS
                });
            } else {
                requestPermissionLauncher.launch(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        }
    }


    /**
     * Habilita la capa de "Mi ubicacion" en el mapa y centra la camara
     * en la posicion del usuario.
     */
    private void habilitarMiUbicacion() {
        if (mMap == null || !isAdded() || getContext() == null)
            return;

        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);

            // Si ya tenemos ubicacion, centrar ahi directamente
            if (ubicacionInicial != null) {
                Log.d(TAG, "Usando ubicacion temprana para centrar mapa");
                moverCamara(ubicacionInicial);
            } else {
                // No hay ubicación: obtener ubicacion
                obtenerUbicacion(location -> {
                    moverCamara(location);
                    ubicacionInicial = location;
                });
            }

            // Iniciar actualizaciones continuas de ubicacion
            iniciarActualizacionesUbicacion();
            Log.d(TAG, "Capa 'Mi ubicacion' habilitada");
        }
    }


    /**
     * Inicia actualizaciones continuas de ubicacion.
     * Actualiza ubicacionActual continuamente y mueve la camara si el seguimiento
     * está activo.
     */
    private void iniciarActualizacionesUbicacion() {
        if (getContext() == null)
            return;
        if (!checkLocationPermission())
            return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isAdded() || getContext() == null)
                    return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Actualizar ubicacion actual (se usa para anadir POIs)
                    ubicacionActual = location;
                    // Mover la camara si el seguimiento está activo
                    if (seguirUbicacion && mMap != null) {
                        moverCamara(location);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }



    /**
     * Muestra un diálogo para anadir un nuevo POI en la posicion actual.
     */
    private void mostrarDialogoAnadirPoi() {
        if (ubicacionActual == null) {
            Toast.makeText(requireContext(),
                    "Esperando ubicacion...",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_anadir_poi, null);

        EditText etNombre = dialogView.findViewById(R.id.etNombrePoi);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcionPoi);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Anadir Punto de Interes")
                .setView(dialogView)
                .setPositiveButton("Anadir", null) // null para evitar autocierre
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();

        // Texto verificado por el corrector (para no repetir la comprobacion)
        final String[] descripcionVerificada = {null};

        // Sobreescribimos el listener para controlar la validacion sin cerrar el diálogo
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            // Validaciones
            if (!validarNombrePoi(nombre)) {
                String msg = nombre.isEmpty() ? "El nombre es obligatorio" : getString(R.string.error_nombre_largo);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                return; // No cierra el diálogo
            }
            if (!validarDescripcionPoi(descripcion)) {
                Toast.makeText(requireContext(),
                        R.string.error_descripcion_poi_larga,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Si la descripcion está verificada y no cambia, guardar directamente
            if (descripcion.equals(descripcionVerificada[0]) || descripcion.isEmpty()) {
                agregarPoi(nombre, descripcion);
                dialog.dismiss();
                return;
            }

            // Verificar ortografia
            v.setEnabled(false);
            SpellCheckHelper.verificarOrtografia(descripcion, new SpellCheckHelper.SpellCheckCallback() {
                @Override
                public void onResultado(String textoCorregido) {
                    v.setEnabled(true);
                    if (textoCorregido != null && !textoCorregido.equals(descripcion)) {
                        // Hay correcciones: mostrar diálogo y volver al formulario
                        mostrarDialogoCorreccion(descripcion, textoCorregido, textoFinal -> {
                            etDescripcion.setText(textoFinal);
                            etDescripcion.setSelection(textoFinal.length());
                            descripcionVerificada[0] = textoFinal;
                        });
                    } else {
                        // Sin correcciones: guardar directamente
                        agregarPoi(nombre, descripcion);
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(Exception e) {
                    v.setEnabled(true);
                    agregarPoi(nombre, descripcion);
                    dialog.dismiss();
                }
            });
        });
    }


    /**
     * Muestra un diálogo para guardar la ruta con nombre y descripcion.
     */
    private void mostrarDialogoGuardarRuta() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_guardar_ruta, null);

        EditText etNombre = dialogView.findViewById(R.id.etNombreRuta);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcionRuta);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Guardar Ruta")
                .setView(dialogView)
                .setPositiveButton("Guardar", null) // null para evitar autocierre
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();

        // Igual que en la funcion anterior
        final String[] descripcionVerificada = {null};


        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            if (!validarNombreRuta(nombre)) {
                String msg = nombre.isEmpty() ? "El nombre es obligatorio" : getString(R.string.error_nombre_largo);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!validarDescripcionRuta(descripcion)) {
                Toast.makeText(requireContext(),
                        R.string.error_descripcion_ruta_larga,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (descripcion.equals(descripcionVerificada[0]) || descripcion.isEmpty()) {
                dialog.dismiss();
                guardarRuta(nombre, descripcion);
                return;
            }

            v.setEnabled(false);
            SpellCheckHelper.verificarOrtografia(descripcion, new SpellCheckHelper.SpellCheckCallback() {
                @Override
                public void onResultado(String textoCorregido) {
                    v.setEnabled(true);
                    if (textoCorregido != null && !textoCorregido.equals(descripcion)) {
                        // Hay correcciones: mostrar diálogo y volver al formulario
                        mostrarDialogoCorreccion(descripcion, textoCorregido, textoFinal -> {
                            etDescripcion.setText(textoFinal);
                            etDescripcion.setSelection(textoFinal.length());
                            descripcionVerificada[0] = textoFinal;
                        });
                    } else {
                        // Sin correcciones: guardar directamente
                        dialog.dismiss();
                        guardarRuta(nombre, descripcion);
                    }
                }

                @Override
                public void onError(Exception e) {
                    v.setEnabled(true);
                    dialog.dismiss();
                    guardarRuta(nombre, descripcion);
                }
            });
        });
    }


    /**
     * Anade un POI a la lista y coloca un marcador en el mapa.
     *
     * @param nombre      nombre del POI
     * @param descripcion descripcion del POI
     */
    private void agregarPoi(String nombre, String descripcion) {
        POIData nuevoPoi = new POIData(
                nombre,
                descripcion,
                ubicacionActual.getLatitude(),
                ubicacionActual.getLongitude());

        listaPOIs.add(nuevoPoi);

        if (mMap != null) {
            LatLng poiLocation = new LatLng(nuevoPoi.getLatitud(), nuevoPoi.getLongitud());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(poiLocation)
                    .title(nombre));
            if (marker != null) marker.setTag(listaPOIs.size() - 1);
        }

        if (listaPOIs.size() == 1) {
            binding.BotonGuardarRuta.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "POI anadido: " + nombre + " en (" +
                ubicacionActual.getLatitude() + ", " +
                ubicacionActual.getLongitude() + "). Total: " + listaPOIs.size());

        Toast.makeText(requireContext(),
                "POI anadido: " + nombre,
                Toast.LENGTH_SHORT).show();
    }


    /**
     * Elimina todos los marcadores del mapa y los vuelve a crear a partir
     * de listaPOIs.
     */
    private void redibujarMarcadores() {
        if (mMap == null) return;
        mMap.clear();
        for (int i = 0; i < listaPOIs.size(); i++) {
            POIData poi = listaPOIs.get(i);
            LatLng poiLocation = new LatLng(poi.getLatitud(), poi.getLongitud());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(poiLocation)
                    .title(poi.getNombre()));
            if (marker != null) marker.setTag(i);
        }
    }


    /**
     * Muestra un diálogo para editar o borrar un POI existente.
     *
     * @param index indice del POI en listaPOIs
     */
    private void mostrarDialogoEditarPoi(int index) {
        if (index < 0 || index >= listaPOIs.size()) return;

        POIData poi = listaPOIs.get(index);

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_anadir_poi, null);

        EditText etNombre = dialogView.findViewById(R.id.etNombrePoi);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcionPoi);

        etNombre.setText(poi.getNombre());
        etDescripcion.setText(poi.getDescripcion());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.editar_poi_titulo)
                .setView(dialogView)
                .setPositiveButton(R.string.guardar_cambios, null)
                .setNegativeButton(R.string.cancelar, null)
                .setNeutralButton(R.string.borrar_poi, null)
                .create();

        dialog.show();

        final String[] descripcionVerificada = {null};

        // Boton guardar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            if (!validarNombrePoi(nombre)) {
                String msg = nombre.isEmpty() ? "El nombre es obligatorio" : getString(R.string.error_nombre_largo);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!validarDescripcionPoi(descripcion)) {
                Toast.makeText(requireContext(),
                        R.string.error_descripcion_poi_larga,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Ortografia otra vez
            if (descripcion.equals(descripcionVerificada[0]) || descripcion.isEmpty()) {
                listaPOIs.set(index, new POIData(nombre, descripcion, poi.getLatitud(), poi.getLongitud()));
                redibujarMarcadores();
                Toast.makeText(requireContext(),
                        getString(R.string.poi_editado, nombre),
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            v.setEnabled(false);
            SpellCheckHelper.verificarOrtografia(descripcion, new SpellCheckHelper.SpellCheckCallback() {
                @Override
                public void onResultado(String textoCorregido) {
                    v.setEnabled(true);
                    if (textoCorregido != null && !textoCorregido.equals(descripcion)) {
                        mostrarDialogoCorreccion(descripcion, textoCorregido, textoFinal -> {
                            etDescripcion.setText(textoFinal);
                            etDescripcion.setSelection(textoFinal.length());
                            descripcionVerificada[0] = textoFinal;
                        });
                    } else {
                        listaPOIs.set(index, new POIData(nombre, descripcion, poi.getLatitud(), poi.getLongitud()));
                        redibujarMarcadores();
                        Toast.makeText(requireContext(),
                                getString(R.string.poi_editado, nombre),
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(Exception e) {
                    v.setEnabled(true);
                    listaPOIs.set(index, new POIData(nombre, descripcion, poi.getLatitud(), poi.getLongitud()));
                    redibujarMarcadores();
                    Toast.makeText(requireContext(),
                            getString(R.string.poi_editado, nombre),
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        // Boton borrar
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.borrar_poi_titulo)
                .setMessage(getString(R.string.borrar_poi_mensaje, poi.getNombre()))
                .setPositiveButton(R.string.borrar, (d, w) -> {
                    listaPOIs.remove(index);
                    redibujarMarcadores();
                    if (!hayPoisSuficientes(listaPOIs.size())) {
                        binding.BotonGuardarRuta.setVisibility(View.GONE);
                    }
                    Toast.makeText(requireContext(),
                            R.string.poi_eliminado,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show());
    }


    /**
     * Interfaz para recibir el texto seleccionado por el usuario
     * en el diálogo de correccion ortografica.
     */
    private interface OnTextoSeleccionado {
        void onTexto(String texto);
    }


    /**
     * Muestra un diálogo que permite al usuario elegir entre el texto original
     * y el texto corregido.
     *
     * @param original  texto original escrito por el usuario
     * @param corregido texto con las correcciones aplicadas
     * @param callback  callback con el texto elegido
     */
    private void mostrarDialogoCorreccion(String original, String corregido, OnTextoSeleccionado callback) {
        if (!isAdded() || getContext() == null) {
            callback.onTexto(original);
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.correccion_titulo)
                .setMessage(getString(R.string.correccion_mensaje, original, corregido))
                .setPositiveButton(R.string.corregir, (d, w) -> callback.onTexto(corregido))
                .setNegativeButton(R.string.mantener_original, (d, w) -> callback.onTexto(original))
                .setCancelable(false)
                .show();
    }


    /**
     * Guarda la ruta con todos sus POIs en Firestore.
     *
     * @param nombre      nombre de la ruta
     * @param descripcion descripcion de la ruta
     */
    private void guardarRuta(String nombre, String descripcion) {
        Log.d(TAG, "Guardando ruta: " + nombre + " con " + listaPOIs.size() + " POIs");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(),
                    "Error: Usuario no autenticado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Referencia al usuario creador
        DocumentReference creadorRef = db.collection("usuarios").document(user.getUid());

        // Lista para guardar las referencias de los POIs ya creados en Firestore
        ArrayList<DocumentReference> poisRefs = new ArrayList<>();
        final int[] poisGuardados = { 0 };
        int totalPOIs = listaPOIs.size();

        // Guardar cada POI en Firestore
        for (int i = 0; i < totalPOIs; i++) {
            POIData poi = listaPOIs.get(i);
            final int index = i; // Guardamos el índice

            java.util.Map<String, Object> poiData = new java.util.HashMap<>();
            poiData.put("nombre", poi.getNombre());
            poiData.put("descripcion", poi.getDescripcion());
            poiData.put("latitud", new GeoPoint(poi.getLatitud(), poi.getLongitud()));

            db.collection("puntos_interes")
                    .add(poiData)
                    .addOnSuccessListener(poiDocRef -> {
                        Log.d(TAG, "POI guardado: " + poi.getNombre() + " [" + poiDocRef.getId() + "]");

                        // Guardar la referencia en la posicion correcta del ArrayList
                        // Usamos synchronized porque los callbacks pueden llegar en cualquier orden
                        synchronized (poisRefs) {
                            // Rellenar con nulls hasta la posicion que necesitamos para ese POI
                            while (poisRefs.size() <= index) {
                                poisRefs.add(null);
                            }
                            poisRefs.set(index, poiDocRef);
                        }

                        poisGuardados[0]++;

                        // Cuando todos los POIs esten guardados, crear el documento de la ruta
                        if (poisGuardados[0] == totalPOIs) {
                            crearRutaEnFirestore(db, nombre, descripcion, creadorRef, poisRefs);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando POI: " + poi.getNombre(), e);
                        Toast.makeText(requireContext(),
                                "Error guardando POI: " + poi.getNombre(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }


    /**
     * Crea el documento de la ruta en la coleccion "rutas" de Firestore.
     *
     * @param db          instancia de Firestore
     * @param nombre      nombre de la ruta
     * @param descripcion descripcion de la ruta
     * @param creadorRef  referencia al documento del usuario creador
     * @param poisRefs    lista de referencias a los documentos de POIs
     */
    private void crearRutaEnFirestore(FirebaseFirestore db, String nombre, String descripcion,
            DocumentReference creadorRef, ArrayList<DocumentReference> poisRefs) {
        java.util.Map<String, Object> rutaData = new java.util.HashMap<>();
        rutaData.put("nombre", nombre);
        rutaData.put("descripcion", descripcion);
        rutaData.put("creador", creadorRef);
        rutaData.put("puntos_interes", poisRefs);

        db.collection("rutas")
                .add(rutaData)
                .addOnSuccessListener(rutaDocRef -> {
                    Log.d(TAG, "Ruta guardada: " + nombre + " [" + rutaDocRef.getId() + "]");

                    Toast.makeText(requireContext(),
                            "Ruta guardada correctamente",
                            Toast.LENGTH_SHORT).show();

                    // Volver al inicio
                    if (isAdded() && getView() != null) {
                        Navigation.findNavController(getView()).popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando ruta", e);
                    Toast.makeText(requireContext(),
                            "Error guardando la ruta",
                            Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Se ejecuta cuando se destruye la vista.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Permitir que la pantalla se apague de nuevo
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Detener actualizaciones de ubicacion para evitar memory leaks
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        binding = null;
    }


    /**
     * Interfaz para callbacks de ubicacion.
     * Se usa internamente para pasar la ubicacion obtenida.
     */
    private interface OnUbicacionRecibida {
        /**
         * Se llama cuando se recibe una ubicacion valida.
         *
         * @param location la ubicacion recibida
         */
        void onUbicacion(Location location);
    }


    /**
     * Verifica si tenemos permisos de ubicacion.
     *
     * @return true si tenemos ACCESS_FINE_LOCATION
     */
    private boolean checkLocationPermission() {
        return isAdded() && getContext() != null &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Mueve la camara del mapa a la ubicacion dada.
     *
     * @param location la ubicacion a la que mover la camara
     */
    private void moverCamara(Location location) {
        if (!mapaListo || mMap == null || location == null)
            return;

        ubicacionActual = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Primer zoom o zoom muy alejado: forzar zoom a nivel 17
        if (!primerZoomRealizado || mMap.getCameraPosition().zoom < 15) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17), 2000, null);
            primerZoomRealizado = true;
            Log.d(TAG, "Zoom forzado (Unificado): " + latLng);
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }


    /**
     * Metodo unificado para obtener ubicacion. Intenta getLastLocation primero
     * y si devuelve null, pide una actualizacion fresca.
     *
     * @param callback callback al que se le pasa la ubicacion
     */
    private void obtenerUbicacion(OnUbicacionRecibida callback) {
        if (!checkLocationPermission())
            return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (!isAdded() || getContext() == null)
                        return;

                    if (location != null) {
                        callback.onUbicacion(location);
                    } else {
                        Log.w(TAG, "getLastLocation es null, solicitando update fresco...");
                        solicitarUpdateFresco(callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en getLastLocation", e);
                    solicitarUpdateFresco(callback);
                });
    }


    /**
     * Solicita una actualizacion de ubicacion fresca al GPS. Se usa cuando getLastLocation
     * devuelve null. Si falla todo, centra el mapa en Madrid.
     *
     * @param callback callback al que se le pasa la ubicacion
     */
    private void solicitarUpdateFresco(OnUbicacionRecibida callback) {
        if (!checkLocationPermission())
            return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                if (!isAdded() || getContext() == null)
                    return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    callback.onUbicacion(location);
                } else {
                    Log.e(TAG, "No se pudo obtener ubicacion fresca");
                    // Fallback a Madrid si falla todo
                    if (mMap != null) {
                        LatLng madrid = new LatLng(40.416775, -3.703790);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(madrid, 16), 2000, null);
                    }
                }
            }
        }, Looper.getMainLooper());
    }


    /**
     * Verifica si el nombre del POI es válido.
     * @param nombre nombre del POI
     * @return true si el nombre es válido, false si no
     */
    private static boolean validarNombrePoi(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return nombre.length() < 25;
    }


    /**
     * Verifica si la descripcion del POI es válida.
     * @param descripcion descripcion del POI
     * @return true si la descripcion es válida, false si no
     */
    private static boolean validarDescripcionPoi(String descripcion) {
        if (descripcion == null) return true;
        return descripcion.length() < 500;
    }


    /**
     * Verifica si el nombre de la ruta es válido.
     * @param nombre nombre de la ruta
     * @return true si el nombre es válido, false si no
     */
    private static boolean validarNombreRuta(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        return nombre.length() < 25;
    }


    /**
     * Verifica si la descripcion de la ruta es válida.
     * @param descripcion descripcion de la ruta
     * @return true si la descripcion es válida, false si no
     */
    private static boolean validarDescripcionRuta(String descripcion) {
        if (descripcion == null) return true;
        return descripcion.length() < 100;
    }


    /**
     * Verifica si hay POIs suficientes para crear una ruta.
     *
     * @param cantidadPois cantidad de POIs
     * @return true si hay suficientes POIs para crear una ruta, false si no
     */
    private static boolean hayPoisSuficientes(int cantidadPois) {
        return cantidadPois > 0;
    }


    /* *********************************************************************************************
     * PUNTOS DE ENTRADA PARA TEST
     **********************************************************************************************/

    /** Punto de entrada público para tests: delega en validarDescripcionPoiImpl. */
    public static boolean TestValidarDescripcionPoi(String descripcion) {
        return validarDescripcionPoi(descripcion);
    }


    /** Punto de entrada público para tests: delega en validarNombreRutaImpl. */
    public static boolean TestValidarNombreRuta(String nombre) {
        return validarNombreRuta(nombre);
    }


    /** Punto de entrada público para tests: delega en hayPoisSuficientesImpl. */
    public static boolean TestHayPoisSuficientes(int cantidadPois) {
        return hayPoisSuficientes(cantidadPois);
    }


    /** Punto de entrada público para tests: delega en validarNombrePoiImpl. */
    public static boolean TestValidarNombrePoi(String nombre) {
        return validarNombrePoi(nombre);
    }


    /** Punto de entrada público para tests: delega en validarDescripcionRutaImpl. */
    public static boolean TestvalidarDescripcionRuta(String descripcion) {
        return validarDescripcionRuta(descripcion);
    }
}
