package services;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import models.Usuario;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthRestService {

    private static final String API_KEY = "Disponible solo en local ;)";
    private Usuario usuarioActual;
    private Firestore db;
    private HttpClient client;

    public AuthRestService() {
        this.usuarioActual = null;
        this.db = FirestoreClient.getFirestore();
        this.client = HttpClient.newHttpClient();
    }

    public Usuario registrarUsuario(String email, String password, String nombre) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            body.put("returnSecureToken", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            if (response.statusCode() == 200) {
                String uid = jsonResponse.getString("localId");

                Map<String, Object> usuarioData = new HashMap<>();
                usuarioData.put("email", email);
                usuarioData.put("nombre", nombre);
                usuarioData.put("createdAt", System.currentTimeMillis());

                // Guardado síncrono para asegurar la creación antes de devolver el usuario
                db.collection("usuarios").document(uid).set(usuarioData).get();

                usuarioActual = new Usuario(uid, email, nombre);
                return usuarioActual;
            } else {
                System.err.println("Error en registro: " + jsonResponse.getJSONObject("error").getString("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Usuario login(String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            body.put("returnSecureToken", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            if (response.statusCode() == 200) {
                String uid = jsonResponse.getString("localId");
                String emailResp = jsonResponse.getString("email");

                // Recuperar datos adicionales de Firestore
                DocumentSnapshot doc = db.collection("usuarios").document(uid).get().get();

                String nombre = emailResp.split("@")[0];
                if (doc.exists() && doc.getString("nombre") != null) {
                    nombre = doc.getString("nombre");
                }

                usuarioActual = new Usuario(uid, emailResp, nombre);
                return usuarioActual;
            } else {
                System.err.println("Error en login: " + jsonResponse.getJSONObject("error").getString("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logout() {
        this.usuarioActual = null;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean isLoggedIn() {
        return usuarioActual != null;
    }
}