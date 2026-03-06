package com.menkaura.vistadrive.actividades;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.menkaura.vistadrive.BuildConfig;
import com.menkaura.vistadrive.R;
import com.menkaura.vistadrive.databinding.LoginActivityBinding;
import android.util.Patterns;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;


/**
 * Actividad de login y registro de la app.
 * Se encarga de toda la autenticacion: login con email/password, registro de nuevos
 * usuarios, inicio de sesion con Google via Credential Manager, recuperacion de contrasena
 * y comprobacion de sí el usuario está bloqueado.
 *
 * <p>Si el usuario ya está autenticado (getCurrentUser != null), se salta directamente
 * a MainActivity, pero antes comprueba que no este bloqueado en Firestore.</p> *
 */
public class LoginActivity extends AppCompatActivity {

    /** ViewBinding para acceder a las vistas del layout login_activity.xml */
    private LoginActivityBinding binding;

    /** Instancia de FirebaseAuth para autenticar usuarios con Firebase Auth. */
    private FirebaseAuth auth;

    /** Tag para los logs de debug */
    private final String TAG = "Debug/122312LoginActivity";


    /**
     * Se ejecuta al crear la actividad. Inicializa Firebase Auth, infla el layout
     * con ViewBinding y comprueba si ya hay un usuario logueado.
     *
     * @param savedInstanceState datos de la instancia anterior
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance();

        binding = LoginActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Si hay un usuario logueado, comprobamos si está bloqueado
        if(auth.getCurrentUser() != null) {
            comprobarUsuarioBloqueado();
            return;
        }

        startSignIn();
        startRegister();
        startGoogleSignIn();
        startForgotPassword();
    }


    /**
     * Configura el listener del boton de login. Recoge email y password de los campos,
     * valida que no esten vacios, que el email tenga formato correcto y que la contrasena
     * cumpla los requisitos. Luego intenta iniciar sesion
     */
    private void startSignIn() {
        binding.loginButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(binding.email.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.password.getText()).toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, R.string.introduce_correo_electronico, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validarEmail(email)) return;
            if (!validarContrasena(password)) return;

            Log.d(TAG, "Intentando iniciar sesion con email: " + email);

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, getString(R.string.inicio_de_sesion_exitoso));
                            comprobarUsuarioBloqueado();
                        } else {
                            Toast.makeText(this, getString(R.string.error) + Objects.requireNonNull(task.getException()).getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }


    /**
     * Configura el enlace de recuperar contraseña. Coge el email del campo de texto
     * y envia un correo de restablecimiento usando Firebase Auth.
     */
    private void startForgotPassword() {
        binding.forgotPasswordText.setOnClickListener(v -> {
            String email = Objects.requireNonNull(binding.email.getText()).toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, R.string.introduce_correo_para_restablecer, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validarEmail(email)) return;

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, R.string.correo_restablecimiento_enviado, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, R.string.error_restablecer_contrasena, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }


    /**
     * Configura el listener del boton de registro. Valida email y contrasena igual que
     * en login, y luego crea el usuario en Firebase Auth.
     */
    private void startRegister() {
        binding.registerButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(binding.email.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.password.getText()).toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, R.string.completa_todos_los_campos, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validarEmail(email)) return;
            if (!validarContrasena(password)) return;

            // Crear usuario con FirebaseAuth
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            mostrarDialogoAlias();
                        } else {
                            Toast.makeText(this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }


    /**
     * Configura el boton de Google Sign-In.
     */
    private void startGoogleSignIn() {
        binding.googleButton.setOnClickListener(v -> lanzarGoogleSignIn(false));
    }


    /**
     * Lanza el flujo de Google Sign-In. Primero lo intenta con cuentas ya autorizadas;
     * si no hay ninguna, usa GetSignInWithGoogleOption para mostrar el selector completo.
     *
     * @param usarSelectorCompleto false para intentar con cuentas previas, true para selector completo
     */
    private void lanzarGoogleSignIn(boolean usarSelectorCompleto) {
        GetCredentialRequest request;

        if (usarSelectorCompleto) {
            // Selector completo: muestra todas las cuentas Google del dispositivo
            GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption
                    .Builder(BuildConfig.WEB_CLIENT_ID)
                    .build();
            request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build();
        } else {
            // Intento rápido: solo cuentas ya autorizadas previamente
            GetGoogleIdOption googleOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build();
            request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build();
        }

        CredentialManager credentialManager = CredentialManager.create(this);
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (!usarSelectorCompleto) {
                            // Reintenta con el selector completo de cuentas
                            Log.d(TAG, "Reintentando Google Sign-In con selector completo: " + e.getMessage());
                            lanzarGoogleSignIn(true);
                        } else {
                            Log.e(TAG, "Error Google Sign-In", e);
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            R.string.error_google, Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                }
        );
    }


    /**
     * Procesa la credencial que devuelve Google. Comprueba que sea del tipo correcto
     * extrae el ID token y lo usa para autenticarse en Firebase.
     * Si es la primera vez del usuario, se comprueba si ya existe en Firestore.
     *
     * @param response credencial de Google
     */
    private void handleGoogleCredential(GetCredentialResponse response) {
        Credential credential = response.getCredential();
        // Verificar que la credencial sea del tipo esperado
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        .equals(credential.getType())) {
            Log.e(TAG, "Tipo de credencial inesperado");
            return;
        }

        // Extraer el token de Google y crear credencial de Firebase
        GoogleIdTokenCredential googleCredential =
                GoogleIdTokenCredential.createFrom(credential.getData());
        String idToken = googleCredential.getIdToken();

        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            comprobarYCrearUsuario(user);
                        }
                    } else {
                        Log.e(TAG, "Error Firebase auth con Google", task.getException());
                        runOnUiThread(() ->
                                Toast.makeText(this, R.string.error_google,
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }


    /**
     * Comprueba si el usuario de Google ya existe en Firestore. Si no existe, le pedimos
     * que elija un alias con el diálogo. Si ya existe, comprobamos que no este bloqueado
     * y lo dejamos pasar.
     *
     * @param user el usuario de Firebase recien autenticado con Google
     */
    private void comprobarYCrearUsuario(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Primera vez que entra con Google, pedir alias
                        mostrarDialogoAlias();
                    } else {
                        // Ya existe, comprobar que no este bloqueado
                        Boolean bloqueado = doc.getBoolean("bloqueado");
                        if (bloqueado != null && bloqueado) {
                            auth.signOut();
                            mostrarDialogoBloqueado();
                        } else {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al comprobar usuario", e);
                    auth.signOut();
                    Toast.makeText(this, R.string.error_conexion, Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Comprueba en Firestore si el usuario actual está bloqueado.
     * Si está bloqueado, cierra sesion y muestra diálogo. Si no, navega a MainActivity.
     */
    private void comprobarUsuarioBloqueado() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean bloqueado = doc.getBoolean("bloqueado");
                        if (bloqueado != null && bloqueado) {
                            auth.signOut();
                            mostrarDialogoBloqueado();
                            return;
                        }
                    }
                    // Si no esta bloqueado, navegar a MainActivity
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al comprobar bloqueo", e);
                    auth.signOut();
                    Toast.makeText(this, R.string.error_conexion, Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Muestra un diálogo informando que el usuario está bloqueado por un admin.
     */
    private void mostrarDialogoBloqueado() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.usuario_bloqueado)
                .setMessage(R.string.usuario_bloqueado_mensaje)
                .setCancelable(false)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> dialog.dismiss())
                .show();
    }


    /**
     * Muestra un diálogo para que el usuario elija su alias.Después comprobamos en Firestore que
     * el alias no esté ya pillado.
     */
    private void mostrarDialogoAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.elige_alias);

        final EditText input = new EditText(this);
        input.setHint(R.string.tu_alias);
        builder.setView(input);

        // Pasamos null como listener para evitar que el dialogo se cierre automaticamente
        builder.setPositiveButton(R.string.aceptar, null);
        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> {
            dialog.cancel();
            Toast.makeText(this, R.string.registro_cancelado, Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Sobreescribimos el listener del boton para controlar cuando se cierra
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String alias = input.getText().toString().trim();
            if (!validarAlias(alias)) {
                int mensajeError = alias.isEmpty() ? R.string.introduce_alias : R.string.error_alias_largo;
                Toast.makeText(this, mensajeError, Toast.LENGTH_SHORT).show();
                return;
            }

            comprobarAliasDisponible(alias, dialog);
        });
    }


    /**
     * Consulta Firestore para ver si ya hay alguien con ese alias.
     * Si esta libre, guarda el usuario y navega a MainActivity.
     * Si esta pillado, muestra un Toast y deja el dialogo abierto.
     *
     * @param alias   el alias que quiere usar el usuario
     * @param dialog  el diálogo de alias (para cerrarlo)
     */
    private void comprobarAliasDisponible(String alias, AlertDialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios")
                .whereEqualTo("alias", alias)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Ya hay alguien con ese alias
                        Toast.makeText(this, R.string.alias_en_uso, Toast.LENGTH_SHORT).show();
                    } else {
                        // Alias libre, guardar y entrar
                        guardarUsuarioEnFirestore(alias);
                        dialog.dismiss();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al comprobar alias", e);
                    Toast.makeText(this, R.string.error_conexion, Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Valida que el email tenga un formato correcto usando Patterns.EMAIL_ADDRESS.
     *
     * @param email el email a validar
     * @return true si el formato es válido, false si no lo es
     */
    private boolean validarEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.error_formato_correo, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    /**
     * Valida que la contrasena contenga al menos una mayuscula, una minuscula y un simbolo.
     *
     * @param password la contrasena a validar
     * @return true si cumple los tres requisitos, false si no los cumple
     */
    private boolean validarContrasena(String password) {
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[^a-zA-Z0-9].*")) {
            Toast.makeText(this, R.string.error_formato_contrasena, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    /**
     * Valida que el alias no tenga más de 25 caracteres.
     *
     * @param alias el alias a validar
     * @return true si es valido, false si no lo es
     */
    private boolean validarAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) return false;
        return alias.length() < 25;
    }


    /**
     * Guarda un nuevo documento de usuario en la coleccion "usuarios" de Firestore.
     * El documento usa el UID de Firebase como ID y guarda el alias, admin=false
     * y bloqueado=false por defecto.
     *
     * @param alias el alias elegido por el usuario
     */
    private void guardarUsuarioEnFirestore(String alias) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("alias", alias);
        usuario.put("admin", false);
        usuario.put("bloqueado", false);

        db.collection("usuarios").document(user.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario agregado"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al agregar usuario", e));
    }


    /* *********************************************************************************************
     * PUNTOS DE ENTRADA PARA TEST
     **********************************************************************************************/

    /** Punto de entrada público para tests: delega en el método privado validarContrasena. */
    public boolean testValidarContrasena(String password) {
        return validarContrasena(password);
    }


    /** Punto de entrada público para tests: delega en el método privado validarEmail. */
    public boolean testValidarEmail(String email) {
        return validarEmail(email);
    }


    /** Punto de entrada público para tests: delega en el método privado validarAliasImpl. */
    public boolean testValidarAlias(String alias) {
        return validarAlias(alias);
    }
}
