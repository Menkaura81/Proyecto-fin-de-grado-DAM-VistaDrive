package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.CreateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ValidarDescripcionRutaTest {

    // Descripción corta
    @Test
    public void testDescripcionRutaValida() {
        assertTrue(CreateFragment.TestvalidarDescripcionRuta("Recorrido por los principales monumentos del casco histórico."));
    }

    // Descripción vacía
    @Test
    public void testDescripcionRutaVacia() {
        assertTrue(CreateFragment.TestvalidarDescripcionRuta(""));
    }

    // Descripción de 99 caracteres
    @Test
    public void testDescripcionRuta99Caracteres() {
        assertTrue(CreateFragment.TestvalidarDescripcionRuta("A".repeat(99)));
    }

    // Descripción de 100 caracteres
    @Test
    public void testDescripcionRuta100Caracteres() {
        assertFalse(CreateFragment.TestvalidarDescripcionRuta("A".repeat(100)));
    }
}
