package services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import models.EstadoJuego;
import models.Juego;
import models.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JuegoService {

    private Firestore db;

    public JuegoService() {
        this.db = FirestoreClient.getFirestore();
    }

    public void añadirJuego(Usuario usuario, Juego juego) {
        if (usuario == null) return;

        try {
            Map<String, Object> datos = prepararMapaDatos(juego);

            db.collection("usuarios")
                    .document(usuario.getId())
                    .collection("juegos")
                    .document(juego.getIgdbId())
                    .set(datos)
                    .get();

        } catch (Exception e) {
            System.err.println("Error al guardar juego: " + e.getMessage());
        }
    }

    public void actualizarJuego(Usuario usuario, Juego juego) throws Exception {
        Map<String, Object> datos = prepararMapaDatos(juego);

        db.collection("usuarios")
                .document(usuario.getId())
                .collection("juegos")
                .document(juego.getIgdbId())
                .set(datos)
                .get();
    }

    public void eliminarJuego(Usuario usuario, String igdbId) throws Exception {
        db.collection("usuarios")
                .document(usuario.getId())
                .collection("juegos")
                .document(igdbId)
                .delete()
                .get();
    }

    public List<Juego> obtenerJuegosLista(Usuario usuario) throws Exception {
        List<Juego> juegos = new ArrayList<>();

        QuerySnapshot snapshot = db.collection("usuarios")
                .document(usuario.getId())
                .collection("juegos")
                .get()
                .get();

        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            Juego juego = new Juego();
            juego.setIgdbId(doc.getId());
            juego.setNombre(doc.getString("nombre"));
            juego.setNotaPersonal(doc.getDouble("notaPersonal"));
            juego.setComentario(doc.getString("comentario"));
            juego.setEsFavorito(doc.getBoolean("esFavorito"));
            juego.setPortadaUrl(doc.getString("portadaUrl"));
            juego.setNotaMediaIGDB(doc.getDouble("notaMediaIGDB"));
            juego.setGenero(doc.getString("genero"));
            if (doc.contains("anyoLanzamiento")) {
                Long yr = doc.getLong("anyoLanzamiento");
                if (yr != null) juego.setAnyoLanzamiento(yr.intValue());
            }

            String estadoStr = doc.getString("estado");
            if (estadoStr != null) {
                juego.setEstado(EstadoJuego.valueOf(estadoStr));
            }

            juegos.add(juego);
        }

        return juegos;
    }

    /**
     * Método auxiliar para evitar repetir la creación del Map en añadir y actualizar
     */
    private Map<String, Object> prepararMapaDatos(Juego juego) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("igdbId", juego.getIgdbId());
        datos.put("nombre", juego.getNombre());
        datos.put("notaPersonal", juego.getNotaPersonal());
        datos.put("comentario", juego.getComentario());
        datos.put("esFavorito", juego.getEsFavorito());
        datos.put("estado", juego.getEstado().toString());
        datos.put("portadaUrl", juego.getPortadaUrl());
        datos.put("notaMediaIGDB", juego.getNotaMediaIGDB());
        datos.put("anyoLanzamiento", juego.getAnyoLanzamiento());
        datos.put("genero", juego.getGenero());
        return datos;
    }
}