package com.menkaura.vistadrive.fragmentos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.modelos.POIData;
import com.menkaura.vistadrive.modelos.RutaData;
import com.menkaura.vistadrive.recyclerview.RutaRecyclerViewAdapter;
import com.menkaura.vistadrive.databinding.FragmentRecyclerBinding;
import java.util.ArrayList;


/**
 * Fragmento principal que muestra la lista de rutas disponibles.
 * Las rutas se cargan desde Firestore y se ordenan por distancia al usuario.
 */
public class RecyclerFragment extends Fragment {

    /** Tag para logs de debug */
    private final String TAG = "Debug/RecyclerFragment";

    /** ViewBinding del layout */
    private FragmentRecyclerBinding binding;

    /** Lista de rutas cargadas desde Firestore */
    private ArrayList<RutaData> rutas;

    /** Adapter del RecyclerView para mostrar las rutas */
    private RutaRecyclerViewAdapter adapter;

    /** Cliente de ubicacion para obtener la posicion del usuario */
    private FusedLocationProviderClient fusedLocationClient;

    /** Ubicacion actual del usuario, se usa para ordenar las rutas por distancia */
    private Location ubicacionUsuario;


    /**
     * Flag que indica si las rutas ya terminaron de cargarse desde Firestore.
     * Se usa para sincronizar con la ubicacion: si la ubicacion llega antes
     * que las rutas, esperamos; si las rutas llegan antes, esperamos la ubicacion.
     */
    private boolean rutasCargadasFlag = false;


    /** Launcher para pedir permisos de ubicacion al usuario */
    private ActivityResultLauncher<String[]> requestPermissionLauncher;


    /** Constructor vacio requerido por el framework de Fragments. */
    public RecyclerFragment() {
        // Required empty public constructor
    }


    /* **********************************************************************************************
     * CICLO DE VIDA DEL FRAGMENTO
     **********************************************************************************************/

    /**
     * Se ejecuta cuando se crea el fragmento. Registra el launcher de permisos
     * de ubicacion. Si el usuario concede el permiso, obtenemos la ubicacion.
     *
     * @param savedInstanceState datos guardados del estado anterior
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        obtenerUbicacion();
                    } else {
                        Log.d(TAG, "Permiso de ubicacion denegado, rutas sin ordenar por distancia");
                    }
                });
    }


    /**
     * Infla el layout y configura los listeners de los botones flotantes
     *
     * @param inflater  inflater para crear las vistas
     * @param container contenedor padre
     * @param savedInstanceState datos guardados
     * @return la vista raiz del fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRecyclerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Boton flotante para ir a la pantalla de configuracion
        FloatingActionButton botonConfig = binding.botonConfig;
        // Boton flotante crear una nueva ruta
        ExtendedFloatingActionButton botonCrear = binding.botonCrear;

        // Listener del boton Config
        botonConfig.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_recyclerFragment_to_configFragment);
        });

        // Listener del boton Crear ruta
        botonCrear.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_recyclerFragment_to_createFragment);
        });

        return view;
    }


    /**
     * Se ejecuta cuando la vista ya está creada. Inicializa el RecyclerView con el adapter,
     * solicita la ubicacion y carga las rutas.
     *
     * @param view la vista raiz ya creada
     * @param savedInstanceState datos guardados
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rutas = new ArrayList<>();

        // Configurar RecyclerView con listener para clicks
        adapter = new RutaRecyclerViewAdapter(rutas, this::rutaClicked);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerview.setAdapter(adapter);

        // Solicitar ubicacion del usuario y cargar rutas
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        solicitarUbicacion();
        loadRutas();

        // Tutorial de primer uso
        verificarTutorial();
    }


    /**
     * Se ejecuta cuando el usuario hace clic en una ruta del RecyclerView.
     * Empaqueta la ruta en un Bundle y navega a DetailsFragment.
     *
     * @param ruta la ruta seleccionada
     * @param view la vista del item clickado
     */
    public void rutaClicked(RutaData ruta, View view) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("ruta", ruta);

        // Ejecutar navegacion a DetailsFragment
        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_recyclerFragment_to_detailsFragment, bundle);
    }


    /**
     * Carga todas las rutas desde la coleccion "rutas" de Firestore.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void loadRutas() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("rutas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rutas.clear();

                    int totalRutas = queryDocumentSnapshots.size();

                    if (totalRutas == 0) {
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "No hay rutas");
                        return;
                    }

                    final int[] rutasCargadas = {0};

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String rutaId = doc.getId();
                        String nombreRuta = doc.getString("nombre");
                        String descripcionRuta = doc.getString("descripcion");
                        DocumentReference creadorRef = doc.getDocumentReference("creador");
                        ArrayList<DocumentReference> poisRefs = (ArrayList<DocumentReference>) doc.get("puntos_interes");

                        // Alias del creador
                        if (creadorRef != null) {
                            creadorRef.get().addOnSuccessListener(userDoc -> {
                                String creadorAlias = userDoc.getString("alias");
                                if (creadorAlias == null) {
                                    creadorAlias = "Sin alias";
                                }

                                // Luego cargamos los POIs de esta ruta
                                cargarPOIsYCrearRuta(rutaId, nombreRuta, descripcionRuta, creadorAlias,
                                        poisRefs, rutasCargadas, totalRutas);

                            }).addOnFailureListener(e -> {
                                Log.e("Firestore", "Error obteniendo usuario", e);
                                cargarPOIsYCrearRuta(rutaId, nombreRuta, descripcionRuta, "Usuario desconocido",
                                        poisRefs, rutasCargadas, totalRutas);
                            });
                        } else {
                            cargarPOIsYCrearRuta(rutaId, nombreRuta, descripcionRuta, "Sin creador",
                                    poisRefs, rutasCargadas, totalRutas);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error cargando rutas", e));
    }


    /**
     * Carga los POIs de una ruta desde Firestore y crea el objeto RutaData.
     *
     * @param rutaId         ID del documento de la ruta en Firestore
     * @param nombreRuta     nombre de la ruta
     * @param descripcionRuta descripcion de la ruta
     * @param creadorAlias   alias del creador
     * @param poisRefs       lista de DocumentReference a los POIs
     * @param rutasCargadas  contador compartido de rutas ya procesadas
     * @param totalRutas     total de rutas a cargar
     */
    @SuppressLint("NotifyDataSetChanged")
    private void cargarPOIsYCrearRuta(String rutaId, String nombreRuta, String descripcionRuta,
                                      String creadorAlias, ArrayList<DocumentReference> poisRefs,
                                      int[] rutasCargadas, int totalRutas) {

        // Si la ruta no tiene POIs, crearla vacia
        if (poisRefs == null || poisRefs.isEmpty()) {
            Log.w("Firestore", "Ruta '" + nombreRuta + "' no tiene POIs");
            RutaData ruta = new RutaData(rutaId, nombreRuta, descripcionRuta, creadorAlias, new ArrayList<>());
            rutas.add(ruta);

            // Cargar puntuacion de esta ruta
            cargarPuntuacion(ruta);

            rutasCargadas[0]++;
            if (rutasCargadas[0] == totalRutas) {
                rutasCargadasFlag = true;
                if (ubicacionUsuario != null) {
                    ordenarRutasPorDistancia();
                } else {
                    adapter.notifyDataSetChanged();
                }
                Log.d("Firestore", "Rutas cargadas: " + rutas.size());
            }
            return;
        }

        // Array fijo en lugar de ArrayList para mantener el orden
        int totalPOIs = poisRefs.size();
        POIData[] pois_ruta = new POIData[totalPOIs];
        final int[] poisCargados = {0};

        Log.d("Firestore", "Cargando " + totalPOIs + " POIs para ruta: " + nombreRuta);

        // Iteramos con índice para poder guardar cada POI en su posicion correcta
        for (int i = 0; i < poisRefs.size(); i++) {
            DocumentReference poiRef = poisRefs.get(i);
            final int index = i;

            Log.d("Firestore", "Buscando POI [" + index + "] en: " + poiRef.getPath());

            poiRef.get().addOnSuccessListener(poiDoc -> {
                if (poiDoc.exists()) {
                    // Obtener todos los datos del POI
                    String nombrePOI = poiDoc.getString("nombre");
                    String descripcionPOI = poiDoc.getString("descripcion");
                    GeoPoint geoPoint = poiDoc.getGeoPoint("latitud");

                    if (geoPoint != null) {
                        double latitud = geoPoint.getLatitude();
                        double longitud = geoPoint.getLongitude();

                        POIData poi = new POIData(nombrePOI, descripcionPOI, latitud, longitud);
                        pois_ruta[index] = poi; // Guardamos en la posicion correcta del array
                        Log.d("Firestore", "POI [" + index + "] cargado: " + nombrePOI + " (" + latitud + ", " + longitud + ")");
                    } else {
                        Log.w("Firestore", "POI [" + index + "] sin coordenadas validas: " + poiDoc.getId());
                        pois_ruta[index] = null;
                    }
                } else {
                    Log.w("Firestore", "POI [" + index + "] no existe: " + poiRef.getPath());
                    pois_ruta[index] = null;
                }

                poisCargados[0]++;
                // Cuando todos los POIs de esta ruta están cargados, crear la RutaData
                if (poisCargados[0] == totalPOIs) {
                    // Convertir array a ArrayList, quitando los nulls (POIs que fallaron)
                    ArrayList<POIData> poisList = new ArrayList<>();
                    for (POIData poi : pois_ruta) {
                        if (poi != null) {
                            poisList.add(poi);
                        }
                    }

                    RutaData ruta = new RutaData(rutaId, nombreRuta, descripcionRuta, creadorAlias, poisList);
                    rutas.add(ruta);

                    // Cargar puntuacion de esta ruta
                    cargarPuntuacion(ruta);

                    Log.d("Firestore", "Ruta creada: " + nombreRuta + " [" + rutaId + "] con " + poisList.size() + " POIs en orden");

                    rutasCargadas[0]++;
                    // Si todas las rutas están listas, ordenar por distancia
                    if (rutasCargadas[0] == totalRutas) {
                        rutasCargadasFlag = true;
                        if (ubicacionUsuario != null) {
                            ordenarRutasPorDistancia();
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        Log.d("Firestore", "Todas las rutas cargadas: " + rutas.size());
                    }
                }

            }).addOnFailureListener(e -> {
                Log.e("Firestore", "Error cargando POI [" + index + "]: " + poiRef.getPath(), e);
                pois_ruta[index] = null;

                poisCargados[0]++;
                if (poisCargados[0] == totalPOIs) {
                    // Misma logica que arriba: convertir array a ArrayList sin nulls
                    ArrayList<POIData> poisList = new ArrayList<>();
                    for (POIData poi : pois_ruta) {
                        if (poi != null) {
                            poisList.add(poi);
                        }
                    }

                    RutaData ruta = new RutaData(rutaId, nombreRuta, descripcionRuta, creadorAlias, poisList);
                    rutas.add(ruta);

                    // Cargar puntuacion de esta ruta
                    cargarPuntuacion(ruta);

                    rutasCargadas[0]++;
                    if (rutasCargadas[0] == totalRutas) {
                        rutasCargadasFlag = true;
                        if (ubicacionUsuario != null) {
                            ordenarRutasPorDistancia();
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        Log.d("Firestore", "Todas las rutas cargadas: " + rutas.size());
                    }
                }
            });
        }
    }


    /**
     * Solicita la ubicacion del usuario. Si ya tenemos permiso, la obtiene directamente.
     * Si no, lanza el launcher de permisos.
     */
    private void solicitarUbicacion() {
        if (isAdded() && getContext() != null &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }


    /**
     * Obtiene la última ubicacion conocida del dispositivo.
     */
    @SuppressWarnings("MissingPermission")
    private void obtenerUbicacion() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                ubicacionUsuario = location;
                Log.d(TAG, "Ubicacion obtenida: " + location.getLatitude() + ", " + location.getLongitude());
                // Si las rutas ya están cargadas, ordenarlas ahora
                if (rutasCargadasFlag) {
                    ordenarRutasPorDistancia();
                }
            } else {
                Log.d(TAG, "getLastLocation devolvio null, rutas sin ordenar por distancia");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error obteniendo ubicacion", e));
    }


    /**
     * Ordena las rutas por distancia al usuario (de más cercana a más lejana).
     */
    @SuppressLint("NotifyDataSetChanged")
    private void ordenarRutasPorDistancia() {
        rutas.sort((r1, r2) -> {
            float d1 = distanciaARuta(r1);
            float d2 = distanciaARuta(r2);
            return Float.compare(d1, d2);
        });
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Rutas ordenadas por distancia al usuario");
    }


    /**
     * Calcula la distancia en metros desde la ubicacion del usuario hasta el primer POI
     * de una ruta.
     *
     * @param ruta la ruta a la que calcular la distancia
     * @return distancia en metros al primer POI, o Float.MAX_VALUE si no tiene POIs
     */
    private float distanciaARuta(RutaData ruta) {
        ArrayList<POIData> pois = ruta.getPois();
        if (pois == null || pois.isEmpty()) {
            return Float.MAX_VALUE; // Sin POIs, va al final de la lista
        }
        POIData primerPOI = pois.get(0);
        float[] results = new float[1];
        // distanceBetween calcula la distancia entre dos puntos y la guarda en el array
        Location.distanceBetween(
                ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude(),
                primerPOI.getLatitud(), primerPOI.getLongitud(),
                results);
        return results[0];
    }


    /* **********************************************************************************************
     * TUTORIAL DE PRIMER USO
     **********************************************************************************************/

    /**
     * Comprueba si el tutorial de primer uso ya se ha mostrado. Si no, lo muestra
     */
    private void verificarTutorial() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("tutorial_recycler", false)) {
            // Encadenamos 3 dialogos: aviso legal -> bienvenida -> explorar rutas
            mostrarDialogoTutorial(
                    getString(R.string.aviso_legal_titulo),
                    getString(R.string.aviso_legal_mensaje),
                    getString(R.string.aceptar),
                    () -> mostrarDialogoTutorial(
                            getString(R.string.tutorial_bienvenida_titulo),
                            getString(R.string.tutorial_bienvenida_mensaje),
                            getString(R.string.tutorial_siguiente),
                            () -> mostrarDialogoTutorial(
                                    getString(R.string.tutorial_recycler_titulo),
                                    getString(R.string.tutorial_recycler_mensaje),
                                    getString(R.string.tutorial_aceptar),
                                    // Cuando el usuario acepta el último diálogo, guardamos que ya lo vio
                                    () -> prefs.edit().putBoolean("tutorial_recycler", true).apply()
                            )
                    )
            );
        }
    }


    /**
     * Muestra el diálogo de tutorial reutilizable
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

        ScrollView scrollTutorial = dialogView.findViewById(R.id.scrollTutorial);
        scrollTutorial.post(() -> {
            if (!isAdded()) return;
            int maxScrollHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.55f);
            View contenido = scrollTutorial.getChildAt(0);
            if (contenido != null && contenido.getHeight() > maxScrollHeight) {
                android.view.ViewGroup.LayoutParams params = scrollTutorial.getLayoutParams();
                params.height = maxScrollHeight;
                scrollTutorial.setLayoutParams(params);
            }
        });
    }


    /**
     * Carga la puntuacion media de una ruta desde la coleccion "puntuaciones" de Firestore.
     *
     * @param ruta la ruta a la que cargar la puntuacion
     */
    @SuppressLint("NotifyDataSetChanged")
    private void cargarPuntuacion(RutaData ruta) {
        if (ruta.getRutaId() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference rutaRef = db.collection("rutas").document(ruta.getRutaId());

        db.collection("puntuaciones")
                .whereEqualTo("ruta", rutaRef)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Double puntuacion = querySnapshot.getDocuments().get(0).getDouble("puntuacion");
                        Long numValoraciones = querySnapshot.getDocuments().get(0).getLong("numero_valoraciones");

                        if (puntuacion != null) {
                            ruta.setPuntuacion(puntuacion);
                        }
                        if (numValoraciones != null) {
                            ruta.setNumValoraciones(numValoraciones.intValue());
                        }

                        Log.d(TAG, "Puntuacion cargada para " + ruta.getNombreRuta() + ": " + puntuacion + " (" + numValoraciones + " valoraciones)");

                        // Actualizar el RecyclerView para que se vea la puntuacion
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando puntuacion para " + ruta.getNombreRuta(), e));
    }
}
