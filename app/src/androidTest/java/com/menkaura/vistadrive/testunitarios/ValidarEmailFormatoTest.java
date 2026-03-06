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
public class ValidarEmailFormatoTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    // Email con subdominio
    @Test
    public void testEmailConSubdominio() {
        activityRule.getScenario().onActivity(activity -> assertTrue(activity.testValidarEmail("usuario@correo.empresa.com")));
    }

    // Email sin dominio tras @
    @Test
    public void testEmailSinDominio() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarEmail("usuario@")));
    }

    // Email con espacio en el nombre
    @Test
    public void testEmailConEspacio() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarEmail("usuario correo@gmail.com")));
    }

    // Email sin usuario antes de @
    @Test
    public void testEmailSoloArroba() {
        activityRule.getScenario().onActivity(activity -> assertFalse(activity.testValidarEmail("@gmail.com")));
    }
}
