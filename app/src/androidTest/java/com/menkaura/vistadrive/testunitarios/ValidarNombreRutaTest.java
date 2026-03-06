package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.CreateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ValidarNombreRutaTest {

    // Nombre de ruta correcto
    @Test
    public void testNombreRutaValido() {
        assertTrue(CreateFragment.TestValidarNombreRuta("Ruta del Románico"));
    }

    // Nombre de ruta vacío
    @Test
    public void testNombreRutaVacio() {
        assertFalse(CreateFragment.TestValidarNombreRuta(""));
    }

    // Nombre de ruta de 25 caracteres
    @Test
    public void testNombreRuta25Caracteres() {
        assertFalse(CreateFragment.TestValidarNombreRuta("RutaConVeinticincoCharsXX"));
    }
}
