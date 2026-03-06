package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.fragmentos.NavigateFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NavigateFragmentTest {

    // Todos los POIs visitados
    @Test
    public void testRutaCompletada() {
        assertTrue(NavigateFragment.TestRutaCompletada(5, 5));
    }

    // POIs visitados superan el total
    @Test
    public void testRutaCompletadaSuperada() {
        assertTrue(NavigateFragment.TestRutaCompletada(6, 5));
    }

    // Quedan POIs por visitar
    @Test
    public void testRutaNoCompletada() {
        assertFalse(NavigateFragment.TestRutaCompletada(3, 5));
    }

    // Quedan POIs por visitar pero hayPoisPendientes
    @Test
    public void testHayPoisPendientes() {
        assertTrue(NavigateFragment.TesthayPoisPendientes(2, 5));
    }

    // Todos los POIs visitados y no hayPoisPendientes
    @Test
    public void testNoHayPoisPendientes() {
        assertFalse(NavigateFragment.TesthayPoisPendientes(5, 5));
    }
}
