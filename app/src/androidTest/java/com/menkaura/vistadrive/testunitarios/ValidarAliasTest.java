package com.menkaura.vistadrive.testunitarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.menkaura.vistadrive.actividades.LoginActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ValidarAliasTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    // Alias con longitud correcta
    @Test
    public void testAliasValido() {
        activityRule.getScenario().onActivity(activity -> assertTrue(activity.testValidarAlias("ViajeroMadrid")));
    }

    // Alias vacío
    @Test
    public void testAliasVacio() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarAlias("")));
    }

    // Alias de 25 caracteres
    @Test
    public void testAlias25Caracteres() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarAlias("AliasConVeinticincoCharsX")));
    }

    // Alias de 24 caracteres
    @Test
    public void testAlias24Caracteres() {
        activityRule.getScenario().onActivity(activity -> assertTrue(activity.testValidarAlias("AliasConVeinticuatroChar")));
    }
}
