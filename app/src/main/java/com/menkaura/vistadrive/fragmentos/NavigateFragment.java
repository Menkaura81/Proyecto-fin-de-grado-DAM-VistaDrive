package com.menkaura.vistadrive.fragmentos;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import android.location.Location;
import android.os.Looper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.menkaura.vistadrive.databinding.FragmentNavigateBinding;
import com.menkaura.vistadrive.modelos.POIData;
import com.menkaura.vistadrive.modelos.RutaData;
import com.menkaura.vistadrive.servicios.GeofenceHelper;
import com.menkaura.vistadrive.servicios.GeofenceEventManager;
import com.menkaura.vistadrive.servicios.LocationCheckService;
import java.util.ArrayList;
import com.menkaura.vistadrive.R;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.List;
import java.util.Objects;
import com.menkaura.vistadrive.servicios.PolylineDecoder;
import com.menkaura.vistadrive.BuildConfig;

/**
 * Fragmento de navegacion GPS. Es el más importante y complejo de la app.
 * Se encarga de guiar al usuario por los POIs de una ruta usando:
 * <ul>
 *   <li>Google Maps para mostrar el mapa con marcadores y circulos de geofence</li>
 *   <li>Geofencing (50m radio) para detectar cuando el usuario llega a un POI</li>
 *   <li>TTS (Text-to-Speech) para dar instrucciones de voz al llegar</li>
 *   <li>Google Directions API (via OkHttp) para trazar la ruta en el mapa</li>
 * </ul>
 */
public class NavigateFragment extends Fragment implements OnMapReadyCallback {

    /** Tag para los logs de debug */
    private final String TAG = "Debug/NavigateFragment";

    /** La ruta que estamos navegando */
    private RutaData ruta;

    /** Helper para crear y gestionar geofences */
    private GeofenceHelper geofenceHelper;

    /** POIs pendientes de activar geofencing */
    private ArrayList<POIData> poisPendientes;

    /** Cliente de ubicacion para seguimiento GPS */
    private FusedLocationProviderClient fusedLocationClient;

    /** Instancia del mapa de Google */
    private GoogleMap mMap;

    /** Callback para recibir actualizaciones continuas de ubicacion */
    private LocationCallback locationCallback;

    /** Controla si la camara del mapa sigue al usuario automaticamente */
    private boolean seguirUbicacion = true;

    /** Ubicacion obtenida antes de que el mapa esté listo (patron de carga paralela) */
    private Location ubicacionInicial = null;

    /** Flag que indica si el mapa ya está listo para usarse */
    private boolean mapaListo = false;

    /** Flag para evitar trazar la ruta inicial más de una vez */
    private boolean rutaInicialTrazada = false;

    /** Flag para asegurar que el primer zoom se hace con animacion completa */
    private boolean primerZoomRealizado = false;

    /** Polyline que se dibuja en el mapa mostrando la ruta al siguiente POI */
    private Polyline rutaPolyline;

    /** API key de Google Maps sacada de BuildConfig */
    private static final String GOOGLE_API_KEY = BuildConfig.MAPS_API_KEY;
    /** Modo de transporte para Google Directions API */
    private static final String MODO_RUTAS = "driving";

    /** Índice del siguiente POI al que hay que ir. Se incrementa cada vez que se completa uno */
    private int poiActualIndex = 0;

    /**
     * Set de índices de POIs que el usuario realmente ha visitado (validados por el servicio).
     * Usamos un Set para evitar duplicados y tener un conteo exacto de POIs completados,
     * independientemente del orden en que se visiten.
     */
    private final java.util.Set<Integer> poisValidados = new java.util.HashSet<>();


    /* **********************************************************************************************
     * LAUNCHER DE PERMISOS
     **********************************************************************************************/

    /** Launcher para solicitar permisos de ubicacion y notificaciones */
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);

                if (fineLocationGranted != null && fineLocationGranted) {
                    Log.d(TAG, "Permisos de ubicacion concedidos");

                    // Obtener ubicacion y habilitarla
                    obtenerUbicacion(location -> {
                        ubicacionInicial = location;
                        if (mapaListo) {
                            habilitarMiUbicacion();
                        }
                    });

                    // Si había POIs pendientes de geofencing activar
                    if (poisPendientes != null && !poisPendientes.isEmpty()) {
                        activarGeofencingAhora(poisPendientes);
                    }

                    // Solicitar permiso de ubicacion en segundo plano
                    verificarPermisoBackground();
                } else {
                    Log.w("NavigateFragment", "Permisos de ubicacion denegados");
                    Toast.makeText(requireContext(),
                            "Se necesitan permisos de ubicacion para mostrar tu posicion",
                            Toast.LENGTH_LONG).show();
                }
            });


    /** Launcher para solicitar permiso de ubicacion en segundo plano */
    private final ActivityResultLauncher<String> backgroundLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    Log.d(TAG, "Permiso de ubicacion en segundo plano concedido");
                } else {
                    Log.w(TAG, "Permiso de ubicacion en segundo plano denegado");
                }
            });


    /** Constructor vacio requerido por el framework de Fragments. */
    public NavigateFragment() {
        // Required empty public constructor
    }

    /* **********************************************************************************************
     * CICLO DE VIDA DEL FRAGMENTO
     **********************************************************************************************/

    /**
     * Se ejecuta al crear el fragmento. Limpia datos de rutas anteriores,
     * inicializa el geofence helper y configura el boton back con un diálogo
     * de advertencia. También registra el listener del GeofenceEventManager
     * para recibir notificaciones cuando el usuario llega a un POI.
     *
     * @param savedInstanceState datos guardados
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mantener la pantalla siempre encendida
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Limpiar datos de rutas anteriores
        limpiarDatosRutaAnterior();

        geofenceHelper = new GeofenceHelper(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Manejar boton vovler
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalir();
            }
        });

        // Registrar listener del EventManager
        GeofenceEventManager.getInstance().setListener(poiIndex -> {
            Log.d(TAG, "========================================");
            Log.d(TAG, "EVENTO RECIBIDO via EventManager:");
            Log.d(TAG, "  POI Index recibido: " + poiIndex);
            Log.d(TAG, "  POI Actual: " + poiActualIndex);
            Log.d(TAG, "========================================");

            if (!isAdded() || getActivity() == null) {
                Log.w(TAG, "Fragment no activo, ignorando evento");
                return;
            }

            // Ejecutar en el hilo principal porque estamos modificando la UI
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null || ruta == null) {
                    Log.w(TAG, "Fragment o ruta no disponible");
                    return;
                }

                if (poiIndex >= 0) {
                    poisValidados.add(poiIndex);

                    // Avanzar al siguiente POI
                    int poiAnterior = poiActualIndex;
                    poiActualIndex = poiIndex + 1;

                    Log.d(TAG, "POI_" + poiIndex + " completado. Avanzando de POI_" + poiAnterior + " a POI_"
                            + poiActualIndex);

                    if (hayPoisPendientes(poiActualIndex, ruta.getPois().size())) {
                        // Trazar ruta al siguiente
                        Log.d(TAG, "Actualizando ruta al POI_" + poiActualIndex);
                        actualizarRutaAlSiguientePOI();
                    } else {
                        // Todos los POIs completados
                        Log.d(TAG, "Ruta completada!");
                        navegarAPuntuar(true);
                    }
                } else {
                    Log.w(TAG, "POI Index invalido: " + poiIndex);
                }
            });
        });

        Log.d(TAG, "GeofenceEventManager listener registrado");
    }


    /**
     * Infla el layout y carga la ruta desde el Bundle.
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentNavigateBinding binding = FragmentNavigateBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        if (getArguments() != null) {
            ruta = getArguments().getParcelable("ruta");
            if (ruta != null) {
                Log.d("NavigateFragment", "Ruta cargada: " + ruta.getNombreRuta() +
                        " con " + ruta.getPois().size() + " POIs");
            }
        }

        return view;
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
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Solicitar permisos
        solicitarPermisosUbicacion();

        // Tutorial de primer uso
        verificarTutorial();
    }


    /* **********************************************************************************************
     * TUTORIAL DE PRIMER USO
     **********************************************************************************************/


    /**
     * Comprueba si el tutorial de navegacion ya se mostro. Si no, lo muestra.
     */
    private void verificarTutorial() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("tutorial_navigate", false)) {
            mostrarDialogoTutorial(
                    getString(R.string.tutorial_navigate_titulo),
                    getString(R.string.tutorial_navigate_mensaje),
                    getString(R.string.tutorial_aceptar),
                    () -> prefs.edit().putBoolean("tutorial_navigate", true).apply()
            );
        }
    }


    /**
     * Muestra el diálogo de tutorial
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
     * Se ejecuta cuando el fragmento vuelve a primer plano. Comprueba si hay
     * estado de navegacion guardado en SharedPreferences. Si lo hay, navega
     * directamente a PuntuateFragment.
     */
    @Override
    public void onResume() {
        super.onResume();

        // Comprobar si hay estado de navegacion pendiente
        SharedPreferences prefs = requireContext().getSharedPreferences("nav_state", Context.MODE_PRIVATE);

        boolean completada = prefs.getBoolean("ruta_completada", false);

        if (completada) {
            // Recuperar datos guardados
            String rutaId = prefs.getString("rutaId", null);
            boolean todosCompletados = prefs.getBoolean("todos_completados", false);
            int poisCompletados = prefs.getInt("pois_completados", 0);
            int poisTotales = prefs.getInt("pois_totales", 0);

            // Limpiar el estado
            prefs.edit()
                    .remove("ruta_completada")
                    .remove("rutaId")
                    .remove("todos_completados")
                    .remove("pois_completados")
                    .remove("pois_totales")
                    .apply();

            // Navegar a PuntuateFragment
            Bundle bundle = new Bundle();
            bundle.putString("rutaId", rutaId);
            bundle.putBoolean("rutaCompletada", todosCompletados);
            bundle.putInt("poisCompletados", poisCompletados);
            bundle.putInt("poisTotales", poisTotales);

            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_navigateFragment_to_puntuateFragment, bundle);
        }
    }


    /**
     * Se ejecuta cuando el fragmento se destruye completamente. Limpia las
     * actualizaciones de ubicacion, el listener del EventManager, la polyline
     * y los geofences.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Detener actualizaciones de ubicacion
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Actualizaciones de ubicacion detenidas");
        }

        // Eliminar listener del EventManager
        GeofenceEventManager.getInstance().removeListener();
        Log.d(TAG, "GeofenceEventManager listener removido en onDestroy");

        // Limpiar linea ruta
        if (rutaPolyline != null) {
            rutaPolyline.remove();
        }

        // Quitar geofences
        if (geofenceHelper != null) {
            geofenceHelper.removeGeofences();
        }
    }


    /**
     * Se ejecuta cuando el mapa está listo. Configura los controles del mapa,
     * anade marcadores y circulos para cada POI, activa geofencing y centra
     * la camara en la ubicacion del usuario (o en el primer POI si aún no la tenemos).
     *
     * @param googleMap la instancia del mapa ya inicializada
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mapaListo = true;

        // Configurar mapa
        configurarMapa();

        LatLng startLocation;

        // Verificar que la ruta y los POIs existan
        if (ruta == null || ruta.getPois() == null || ruta.getPois().isEmpty()) {
            Log.w(TAG, "No hay POIs en la ruta");
            // Madrid si no hay POIs
            startLocation = new LatLng(40.416775, -3.703790);

            mMap.addMarker(new MarkerOptions()
                    .position(startLocation)
                    .title("Ubicacion por defecto"));
        } else {
            ArrayList<POIData> pois = ruta.getPois();
            startLocation = pois.get(0).toLatLng();

            // Anadir marcadores y circulos para cada POI
            for (int i = 0; i < pois.size(); i++) {
                mMap.addMarker(new MarkerOptions()
                        .position(pois.get(i).toLatLng())
                        .title(pois.get(i).getNombre())
                        .snippet(pois.get(i).getDescripcion()));
                // Círculo rojo que muestra la zona de activacion del TTS
                mMap.addCircle(new CircleOptions()
                        .center(pois.get(i).toLatLng())
                        .radius(LocationCheckService.DISTANCIA_MINIMA_TTS)
                        .strokeColor(Color.parseColor("#FF4444"))
                        .strokeWidth(4f)
                        .fillColor(Color.argb(35, 255, 0, 0))
                        .zIndex(1));
            }

            // Activar geofencing para todos los POIs
            activarGeofencing(pois);
        }

        // Centrar en la ubicacion del usuario
        if (ubicacionInicial != null) {
            moverCamara(ubicacionInicial);
            Log.d(TAG, "Mapa centrado en ubicacion del usuario (Initial)");
        } else {
            // Si no, centrar en el primer POI mientras llega la ubicacion
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 17), 2000, null);
            Log.d(TAG, "Mapa centrado en primer POI (ubicacion aun no disponible)");
        }

        // Si hay permisos, habilitar ubicacion en el mapa
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            habilitarMiUbicacion();
        }
    }


    /**
     * Configura los controles del mapa: zoom, brujula, boton de mi ubicacion.
     */
    private void configurarMapa() {
        if (mMap == null)
            return;

        // Habilitar controles del mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // No seguir al usuario si mueve el mapa
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                seguirUbicacion = false;
                Log.d(TAG, "Seguimiento de ubicacion desactivado (gesto del usuario)");
            }
        });

        // Volver al seguimiento si se pulsa el boton
        mMap.setOnMyLocationButtonClickListener(() -> {
            seguirUbicacion = true;
            Log.d(TAG, "Seguimiento de ubicacion activado");
            return false; // false = ejecutar también el comportamiento por defecto
        });
    }


    /**
     * Habilita la capa de "Mi ubicacion" en el mapa y centra la camara.
     */
    private void habilitarMiUbicacion() {
        if (mMap == null || !isAdded() || getContext() == null)
            return;

        if (checkLocationPermission()) {
            // Habilitar el punto azul en el mapa
            mMap.setMyLocationEnabled(true);

            // Si hay ubicacion trazar ruta al primer POI
            if (ubicacionInicial != null) {
                if (ruta != null && !ruta.getPois().isEmpty()) {
                    Log.d(TAG, "Usando ubicacion temprana para trazar ruta");
                    trazarRutaInicial(ubicacionInicial);
                }
                moverCamara(ubicacionInicial);
            } else {
                // Si no, pedir ubicacion
                obtenerUbicacion(location -> {
                    ubicacionInicial = location;
                    if (ruta != null && !ruta.getPois().isEmpty()) {
                        trazarRutaInicial(location);
                    }
                    moverCamara(location);
                });
            }

            // Iniciar actualizaciones continuas
            iniciarActualizacionesUbicacion();
            Log.d(TAG, "Capa 'Mi ubicacion' habilitada");
        }
    }


    /**
     * Inicia actualizaciones continuas de ubicacion
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
                if (location != null && seguirUbicacion) {
                    moverCamara(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Actualizaciones de ubicacion iniciadas");
    }


    /**
     * Traza la ruta inicial desde la ubicacion actual hasta el primer POI (o el
     * POI actual si ya se han completado algunos).
     *
     * @param location ubicacion actual del usuario
     */
    private void trazarRutaInicial(Location location) {
        if (rutaInicialTrazada)
            return;
        rutaInicialTrazada = true;

        if (ruta == null || ruta.getPois().isEmpty() || esRutaCompletada(poiActualIndex, ruta.getPois().size())) {
            Log.w(TAG, "No se puede trazar ruta inicial: ruta o POIs no disponibles");
            return;
        }

        LatLng miUbicacion = new LatLng(location.getLatitude(), location.getLongitude());
        LatLng primerPOI = ruta.getPois().get(poiActualIndex).toLatLng();

        Log.d(TAG, "Mi ubicacion: " + miUbicacion.latitude + ", " + miUbicacion.longitude);
        Log.d(TAG, "POI destino [" + poiActualIndex + "]: " + primerPOI.latitude + ", " + primerPOI.longitude);

        // Obtener y dibujar la ruta
        obtenerRutaHaciaPrimerPOI(miUbicacion, primerPOI);
    }


    /**
     * Llama a la Google Directions API para obtener la ruta entre dos puntos.
     *
     * @param origen  punto de origen (ubicacion del usuario)
     * @param destino punto de destino (POI)
     */
    private void obtenerRutaHaciaPrimerPOI(LatLng origen, LatLng destino) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=" + MODO_RUTAS +
                "&key=" + GOOGLE_API_KEY;

        Log.d(TAG, "Solicitando ruta a Directions API");

        // Hay que usarlo para la llamada
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Peticion con OkHttp
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error obteniendo ruta", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Error al obtener la ruta",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Respuesta no exitosa: " + response.code());
                    return;
                }

                try {
                    String jsonData = Objects.requireNonNull(response.body()).string();
                    Log.d(TAG, "Respuesta JSON recibida");

                    JSONObject jsonObject = new JSONObject(jsonData);

                    // Verificar OK
                    String status = jsonObject.getString("status");
                    if (!status.equals("OK")) {
                        Log.e(TAG, "API status: " + status);
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "No se pudo calcular la ruta: " + status,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        return;
                    }

                    // Extraer la polyline
                    JSONArray routes = jsonObject.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedString = overviewPolyline.getString("points");

                        // Decodificar la polyline a una lista de LatLng
                        List<LatLng> puntos = PolylineDecoder.decode(encodedString);

                        // Obtener distancia y duracion
                        JSONArray legs = route.getJSONArray("legs");
                        if (legs.length() > 0) {
                            JSONObject leg = legs.getJSONObject(0);
                            String distancia = leg.getJSONObject("distance").getString("text");
                            String duracion = leg.getJSONObject("duration").getString("text");

                            Log.d(TAG, "Ruta calculada: " + distancia + " - " + duracion);

                            // Dibujar la ruta en el hilo principal
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (isAdded() && getContext() != null) {
                                        dibujarRuta(puntos);
                                        Toast.makeText(getContext(),
                                                "Ruta: " + distancia + " (" + duracion + ")",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } else {
                        Log.w(TAG, "No se encontraron rutas");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error parseando JSON", e);
                }
            }
        });
    }


    /**
     * Dibuja una polyline en el mapa con los puntos dados. Si ya había una
     * polyline dibujada, la borra antes de dibujar la nueva.
     *
     * @param puntos lista de coordenadas LatLng que forman la ruta
     */
    private void dibujarRuta(List<LatLng> puntos) {
        if (mMap == null || puntos == null || puntos.isEmpty()) {
            Log.w(TAG, "No se puede dibujar la ruta");
            return;
        }

        // Eliminar ruta anterior
        if (rutaPolyline != null) {
            rutaPolyline.remove();
        }

        // Dibujar nueva línea azul
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(puntos)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true);

        rutaPolyline = mMap.addPolyline(polylineOptions);

        Log.d(TAG, "Ruta dibujada con " + puntos.size() + " puntos");
    }


    /**
     * Interfaz funcional para callbacks de ubicacion.
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
     * @return true si tenemos permiso ACCESS_FINE_LOCATION
     */
    private boolean checkLocationPermission() {
        return isAdded() && getContext() != null &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Mueve la camara del mapa a la ubicacion dada. Si es el primer movimiento hace un
     * zoom animado a nivel 17.
     *
     * @param location la ubicacion a la que mover la camara
     */
    private void moverCamara(Location location) {
        if (!mapaListo || mMap == null || location == null)
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Primer zoom
        if (!primerZoomRealizado || mMap.getCameraPosition().zoom < 15) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17), 2000, null);
            primerZoomRealizado = true;
            Log.d(TAG, "Zoom forzado (Unificado): " + latLng);
        } else {
            // Si ya estamos cerca, solo mover
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }


    /**
     * Metodo unificado para obtener la ubicacion. Primero getLastLocation, y si falla
     * pide una actualizacion fresca al GPS.
     *
     * @param callback callback al que se le pasa la ubicacion obtenida
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
                        // null asi que pedir ubicacion fresca
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
     * Solicita una unica actualizacion de ubicacion fresca al GPS.
     * Se usa cuando getLastLocation devuelve null.
     *
     * @param callback callback al que se le pasa la ubicacion
     */
    private void solicitarUpdateFresco(OnUbicacionRecibida callback) {
        if (!checkLocationPermission())
            return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1) // Solo queremos una actualizacion
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
                }
            }
        }, Looper.getMainLooper());
    }


    /**
     * Solicita permisos de ubicacion. Si ya los tenemos, obtiene la ubicacion directamente.
     */
    private void solicitarPermisosUbicacion() {
        if (checkLocationPermission()) {
            // Ya tenemos permisos de ubicacion en primer plano
            obtenerUbicacion(location -> {
                ubicacionInicial = location;
                if (mapaListo) {
                    habilitarMiUbicacion();
                }
            });
            // Verificar y solicitar permiso de ubicacion en segundo plano
            verificarPermisoBackground();
        } else {
            // Pedir permisos
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
     * Verifica si tenemos permiso de ubicacion en segundo plano (Android 10+).
     * Si no lo tenemos, muestra el aviso destacado requerido por Google Play antes de solicitarlo.
     */
    private void verificarPermisoBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isAdded() && getContext() != null) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mostrarAvisoPermisoBackground();
            }
        }
    }


    /**
     * Muestra el aviso destacado de ubicacion en segundo plano exigido por la politica de Google Play.
     * Solo tras la aceptacion del usuario se solicita el permiso ACCESS_BACKGROUND_LOCATION.
     */
    private void mostrarAvisoPermisoBackground() {
        if (!isAdded() || getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.permiso_background_titulo)
                .setMessage(R.string.permiso_background_mensaje)
                .setCancelable(false)
                .setPositiveButton(R.string.permiso_background_boton, (dialog, which) ->
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                .show();
    }


    /**
     * Intenta activar geofencing para los POIs. Si no tenemos permisos aún,
     * guarda los POIs como pendientes para activarlos cuando lleguen los permisos.
     *
     * @param pois lista de POIs para los que crear geofences
     */
    private void activarGeofencing(ArrayList<POIData> pois) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Guardar POIs pendientes hasta que tengamos permisos
            poisPendientes = pois;
            return;
        }

        activarGeofencingAhora(pois);
    }


    /**
     * Activa los geofences para todos los POIs usando el GeofenceHelper.
     *
     * @param pois lista de POIs para los que crear geofences
     */
    private void activarGeofencingAhora(ArrayList<POIData> pois) {
        geofenceHelper.addGeofences(pois, new GeofenceHelper.GeofenceCallback() {
            @Override
            public void onSuccess(int count) {
                Log.d("NavigateFragment", "Geofences activados: " + count);
                Toast.makeText(requireContext(),
                        "Alertas de proximidad activadas para " + count + " puntos",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.e("NavigateFragment", "Error activando geofences: " + error);
            }
        });
    }


    /**
     * Obtiene la ubicacion actual y traza la ruta al siguiente POI pendiente.
     */
    private void actualizarRutaAlSiguientePOI() {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment no disponible para actualizar ruta");
            return;
        }

        if (ruta == null || esRutaCompletada(poiActualIndex, ruta.getPois().size())) {
            Log.w(TAG, "Ruta null o poiActualIndex fuera de rango: " + poiActualIndex);
            return;
        }

        if (!checkLocationPermission()) {
            Log.w(TAG, "Sin permisos de ubicacion");
            return;
        }

        Log.d(TAG,
                "Actualizando ruta al POI " + poiActualIndex + ": " + ruta.getPois().get(poiActualIndex).getNombre());

        // Obtener ubicacion actual y trazar ruta al siguiente POI
        obtenerUbicacion(location -> {
            if (ruta != null && poiActualIndex < ruta.getPois().size()) {
                LatLng miUbicacion = new LatLng(
                        location.getLatitude(),
                        location.getLongitude());
                LatLng siguientePOI = ruta.getPois().get(poiActualIndex).toLatLng();

                Log.d(TAG, "Trazando ruta al siguiente POI: " +
                        ruta.getPois().get(poiActualIndex).getNombre());

                // Dibujar la nueva ruta
                obtenerRutaHaciaPrimerPOI(miUbicacion, siguientePOI);

                Toast.makeText(getContext(),
                        "Siguiente destino: " + ruta.getPois().get(poiActualIndex).getNombre(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Muestra un diálogo cuando el usuario pulsa el boton back.
     * Si todos los POIs están completados, navega directamente a puntuar.
     * Si no, da tres opciones: continuar la ruta, abandonar, o puntuar igualmente.
     */
    private void mostrarDialogoSalir() {
        if (ruta == null || !isAdded()) {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        int totalPois = ruta.getPois().size();
        int poisCompletados = poiActualIndex;

        if (esRutaCompletada(poisCompletados, totalPois)) {
            // Todos completados puntuar
            navegarAPuntuar(true);
        } else {
            // opciones
            new AlertDialog.Builder(requireContext())
                    .setTitle("Abandonar la ruta?")
                    .setMessage("Has completado " + poisCompletados + " de " + totalPois + " puntos de interes.\n\n" +
                            "Si abandonas ahora, no podras puntuar la ruta.")
                    .setPositiveButton("Continuar ruta", (dialog, which) -> dialog.dismiss())
                    .setNegativeButton("Abandonar", (dialog, which) -> {
                        detenerServicioUbicacion();
                        NavController navController = NavHostFragment.findNavController(NavigateFragment.this);
                        navController.popBackStack();
                    })
                    .setNeutralButton("Puntuar igualmente", (dialog, which) -> navegarAPuntuar(false))
                    .setCancelable(true)
                    .show();
        }
    }


    /**
     * Navega a PuntuateFragment pasando los datos necesarios
     *
     * @param rutaCompletada true si se completaron TODOS los POIs
     */
    private void navegarAPuntuar(boolean rutaCompletada) {
        if (!isAdded() || getContext() == null || ruta == null) {
            Log.w(TAG, "No se puede navegar: fragment o ruta no disponible");
            return;
        }

        if (rutaCompletada) {
            Toast.makeText(getContext(), "Felicitaciones! Has completado la ruta", Toast.LENGTH_LONG).show();
        }

        int poisRealmenteCompletados = poisValidados.size();
        Log.d(TAG, "POIs realmente completados: " + poisRealmenteCompletados + " de " + ruta.getPois().size());

        detenerServicioUbicacion();

        // Limpiar polyline
        if (rutaPolyline != null) {
            rutaPolyline.remove();
            rutaPolyline = null;
        }

        // Guardar estado en SharedPreferences
        SharedPreferences prefs = getContext().getSharedPreferences("nav_state", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("ruta_completada", true)
                .putString("rutaId", ruta.getRutaId())
                .putBoolean("todos_completados", rutaCompletada)
                .putInt("pois_completados", poisRealmenteCompletados)
                .putInt("pois_totales", ruta.getPois().size())
                .apply();

        // Navegar a PuntuateFragment
        Bundle bundle = new Bundle();
        bundle.putString("rutaId", ruta.getRutaId());
        bundle.putBoolean("rutaCompletada", rutaCompletada);
        bundle.putInt("poisCompletados", poisRealmenteCompletados);
        bundle.putInt("poisTotales", ruta.getPois().size());

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_navigateFragment_to_puntuateFragment, bundle);

        Log.d(TAG, "Navegando a PuntuateFragment con rutaId: " + ruta.getRutaId());
    }


    /**
     * Limpia todos los SharedPreferences de la ruta anterior antes de empezar una nueva.
     */
    private void limpiarDatosRutaAnterior() {
        if (getContext() == null) return;
        getContext().getSharedPreferences("GeofencePending", Context.MODE_PRIVATE).edit().clear().apply();
        getContext().getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE).edit().clear().apply();
        getContext().getSharedPreferences("nav_state", Context.MODE_PRIVATE).edit().clear().apply();
        poiActualIndex = 0;
        poisValidados.clear();
        Log.d(TAG, "Datos de ruta anterior limpiados");
    }


    /**
     * Detiene todo lo relacionado con la navegacion
     */
    private void detenerServicioUbicacion() {
        if (getContext() == null)
            return;

        // Servicio de verificacion
        Intent serviceIntent = new Intent(getContext(), LocationCheckService.class);
        getContext().stopService(serviceIntent);
        Log.d(TAG, "LocationCheckService detenido");

        // Geofences
        if (geofenceHelper != null) {
            geofenceHelper.removeGeofences();
            Log.d(TAG, "Todas las geovallas desactivadas");
        }

        // Limpiar geofences pendientes
        SharedPreferences prefsPending = getContext().getSharedPreferences("GeofencePending", Context.MODE_PRIVATE);
        prefsPending.edit().clear().apply();
        Log.d(TAG, "GeofencePending limpiado");

        // Limpiar datos de geofences
        SharedPreferences prefsGeofence = getContext().getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE);
        prefsGeofence.edit().clear().apply();
        Log.d(TAG, "GeofencePrefs limpiado");

        // Limpiar estado de navegacion
        SharedPreferences prefsNavState = getContext().getSharedPreferences("nav_state", Context.MODE_PRIVATE);
        prefsNavState.edit().clear().apply();
        Log.d(TAG, "nav_state limpiado");

        // Resetear contadores
        poiActualIndex = 0;
        poisValidados.clear();
        Log.d(TAG, "poiActualIndex y poisValidados reseteados");
    }


    /**
     * Verifica si la ruta ha sido completada.
     * @param poisVisitados numero de POIs visitados
     * @param totalPois numero total de POIs
     * @return true si la ruta ha sido completada
     */
    private static boolean esRutaCompletada(int poisVisitados, int totalPois) {
        return poisVisitados >= totalPois;
    }


    /**
     * Verifica si hay POIs pendientes.
     * @param poisVisitados numero de POIs visitados
     * @param totalPois numero total de POIs
     * @return true si hay POIs pendientes
     */
    private static boolean hayPoisPendientes(int poisVisitados, int totalPois) {
        return poisVisitados < totalPois;
    }


    /* *********************************************************************************************
     * PUNTOS DE ENTRADA PARA TEST
     **********************************************************************************************/

    /** Punto de entrada público para tests: delega en esRutaCompletadaImpl. */
    public static boolean TestRutaCompletada(int poisVisitados, int totalPois) {
        return esRutaCompletada(poisVisitados, totalPois);
    }


    /** Punto de entrada público para tests: delega en hayPoisPendientesImpl. */
    public static boolean TesthayPoisPendientes(int poisVisitados, int totalPois) {
        return hayPoisPendientes(poisVisitados, totalPois);
    }

}
