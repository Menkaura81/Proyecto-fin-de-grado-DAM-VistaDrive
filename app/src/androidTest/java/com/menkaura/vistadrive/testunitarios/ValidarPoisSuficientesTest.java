package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.CreateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ValidarPoisSuficientesTest {

    // Sin ningún POI añadido
    @Test
    public void testSinPois() {
        assertFalse(CreateFragment.TestHayPoisSuficientes(0));
    }

    // Con un solo POI
    @Test
    public void testUnPoi() {
        assertTrue(CreateFragment.TestHayPoisSuficientes(1));
    }

    // Con varios POIs
    @Test
    public void testVariosPois() {
        assertTrue(CreateFragment.TestHayPoisSuficientes(5));
    }
}
