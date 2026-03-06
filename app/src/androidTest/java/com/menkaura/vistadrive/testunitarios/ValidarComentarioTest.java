package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.PuntuateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(AndroidJUnit4.class)

public class ValidarComentarioTest {

    // Comentario corto
    @Test
    public void testComentarioValido() {
        assertTrue(PuntuateFragment.TestValidarComentario("Ruta fantástica, muy bien señalizada."));
    }

    // Comentario vacío
    @Test
    public void testComentarioVacio() {
        assertTrue(PuntuateFragment.TestValidarComentario(""));
    }

    // Comentario de 99 caracteres
    @Test
    public void testComentario99Caracteres() {
        assertTrue(PuntuateFragment.TestValidarComentario("A".repeat(99)));
    }

    // Comentario de 100 caracteres
    @Test
    public void testComentario100Caracteres() {
        assertFalse(PuntuateFragment.TestValidarComentario("A".repeat(100)));
    }
}
