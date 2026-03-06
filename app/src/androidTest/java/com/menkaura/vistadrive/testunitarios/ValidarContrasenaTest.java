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
public class ValidarContrasenaTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    // Contraseña con mayúscula, minúscula y símbolo
    @Test
    public void testContrasenaValida() {
        activityRule.getScenario().onActivity(activity -> assertTrue(activity.testValidarContrasena("Passw0rd!")));
    }

    // Sin símbolo
    @Test
    public void testContrasenaSinSimbolo() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarContrasena("Password1")));
    }

    // Sin minúscula
    @Test
    public void testContrasenaSinMinuscula() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarContrasena("PASSWORD1!")));
    }
}
