package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.CreateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ValidarDescripcionPoiTest {

    // Descripción corta
    @Test
    public void testDescripcionPoiValida() {
        assertTrue(CreateFragment.TestValidarDescripcionPoi("Vista panorámica desde el mirador."));
    }

    // Descripción vacía
    @Test
    public void testDescripcionPoiVacia() {
        assertTrue(CreateFragment.TestValidarDescripcionPoi(""));
    }

    // Descripción de 499 caracteres
    @Test
    public void testDescripcionPoi499Caracteres() {
        assertTrue(CreateFragment.TestValidarDescripcionPoi("A".repeat(499)));
    }

    // Descripción de 500 caracteres
    @Test
    public void testDescripcionPoi500Caracteres() {
        assertFalse(CreateFragment.TestValidarDescripcionPoi("A".repeat(500)));
    }
}
