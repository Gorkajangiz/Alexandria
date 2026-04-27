package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import main.App;
import models.Usuario;
import services.AuthRestService;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    private App app;
    private AuthRestService authService;

    public LoginController() {
        this.authService = new AuthRestService();
    }

    public void setApp(App app) {
        this.app = app;
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Por favor, completa todos los campos.");
            return;
        }

        try {
            Usuario usuario = authService.login(email, password);

            if (usuario != null) {
                Preferences prefs = Preferences.userNodeForPackage(App.class);
                prefs.put("alexandria_email", email);
                prefs.put("alexandria_password", password);
                
                app.mostrarMain(usuario);
            } else {
                messageLabel.setText("Email o contraseña incorrectos.");
            }
        } catch (Exception e) {
            messageLabel.setText("Error durante el inicio de sesión.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Por favor, completa todos los campos.");
            return;
        }

        if (password.length() < 6) {
            messageLabel.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        try {
            // Generamos un nombre por defecto a partir del email
            String nombre = email.contains("@") ? email.split("@")[0] : email;
            Usuario usuario = authService.registrarUsuario(email, password, nombre);

            if (usuario != null) {
                Preferences prefs = Preferences.userNodeForPackage(App.class);
                prefs.put("alexandria_email", email);
                prefs.put("alexandria_password", password);
                
                app.mostrarMain(usuario);
            } else {
                messageLabel.setText("Error: El usuario ya existe o los datos son inválidos.");
            }
        } catch (Exception e) {
            messageLabel.setText("Error durante el registro de usuario.");
            e.printStackTrace();
        }
    }
}