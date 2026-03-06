package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.CreateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ValidarNombrePoiTest {

    // Nombre de POI correcto
    @Test
    public void testNombrePoiValido() {
        assertTrue(CreateFragment.TestValidarNombrePoi("Catedral de Burgos"));
    }

    // Nombre de POI vacío
    @Test
    public void testNombrePoiVacio() {
        assertFalse(CreateFragment.TestValidarNombrePoi(""));
    }

    // Nombre de POI de 25 caracteres
    @Test
    public void testNombrePoiDemasiadoLargo() {
        assertFalse(CreateFragment.TestValidarNombrePoi("NombrePoiDemasiadoLargoXX"));
    }
}
