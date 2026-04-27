package config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.InputStream;

public class FirebaseInit {

    private static boolean initialized = false;

    public static void inicializar() {
        if (initialized) {
            return;
        }

        try {
            InputStream serviceAccount = FirebaseInit.class.getClassLoader()
                    .getResourceAsStream("firebase-config.json");

            if (serviceAccount == null) {
                System.err.println("Error: No se encontró el archivo de configuración.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("alexandria-1312")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            initialized = true;
            System.out.println("Firebase se ha conectado con éxito.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}