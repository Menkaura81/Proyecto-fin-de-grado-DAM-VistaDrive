package com.menkaura.vistadrive.testintegracion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Test de integración con Firestore.
 * <p>
 * Guarda una puntuación en la colección "test_integracion", la recupera
 * y comprueba que los datos coinciden.  *
 */
@RunWith(AndroidJUnit4.class)
public class TestIntegracionFirestore {

    private static final String COLECCION_TEST = "test_integracion";
    private static final double PUNTUACION_ESPERADA = 4.0;
    private static final long NUM_VALORACIONES_ESPERADO = 1L;
    private static final String COMENTARIO_ESPERADO = "Ruta de prueba de integración";

    private FirebaseFirestore db;
    private String documentoId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Limpia el documento de test creado durante la prueba.
     */
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (documentoId != null) {
            Tasks.await(db.collection(COLECCION_TEST).document(documentoId).delete());
        }
    }

    /**
     * Guarda una puntuación de prueba en Firestore y almacena el ID del documento creado.
     */
    private void guardarPuntuacion() throws ExecutionException, InterruptedException {
        Map<String, Object> datos = new HashMap<>();
        datos.put("puntuacion", PUNTUACION_ESPERADA);
        datos.put("numero_valoraciones", NUM_VALORACIONES_ESPERADO);
        datos.put("comentario", COMENTARIO_ESPERADO);

        DocumentReference docRef = Tasks.await(db.collection(COLECCION_TEST).add(datos));
        documentoId = docRef.getId();
    }

    /**
     * Recupera de Firestore el documento guardado por {@link #guardarPuntuacion()}.
     *
     * @return documento con los datos
     */
    private DocumentSnapshot recuperarPuntuacion() throws ExecutionException, InterruptedException {
        return Tasks.await(db.collection(COLECCION_TEST).document(documentoId).get());
    }

    /**
     * Test principal: guarda una puntuación, la recupera y comprueba que
     * los campos coinciden con los valores guardados.
     */
    @Test
    public void testGuardarYRecuperarPuntuacion() throws ExecutionException, InterruptedException {
        guardarPuntuacion();

        DocumentSnapshot doc = recuperarPuntuacion();

        assertNotNull("El documento recuperado no debe ser null", doc);

        Double puntuacion = doc.getDouble("puntuacion");
        Long numValoraciones = doc.getLong("numero_valoraciones");
        assertNotNull("El campo puntuacion no debe ser null", puntuacion);
        assertNotNull("El campo numero_valoraciones no debe ser null", numValoraciones);

        assertEquals("La puntuacion debe coincidir",
                PUNTUACION_ESPERADA, puntuacion, 0.001);
        assertEquals("El número de valoraciones debe coincidir",
                NUM_VALORACIONES_ESPERADO, (long) numValoraciones);
        assertEquals("El comentario debe coincidir",
                COMENTARIO_ESPERADO, doc.getString("comentario"));
    }
}
