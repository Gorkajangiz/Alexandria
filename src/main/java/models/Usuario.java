package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Usuario {
    private String id;
    private String email;
    private String nombre;
    private List<String> juegosIds;
    private Map<String, Object> preferencias;

    public Usuario() {
        this.juegosIds = new ArrayList<>();
        this.preferencias = new HashMap<>();
    }

    public Usuario(String id, String email, String nombre) {
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.juegosIds = new ArrayList<>();
        this.preferencias = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<String> getJuegosIds() {
        return juegosIds;
    }

    public void setJuegosIds(List<String> juegosIds) {
        this.juegosIds = juegosIds;
    }

    public Map<String, Object> getPreferencias() {
        return preferencias;
    }

    public void setPreferencias(Map<String, Object> preferencias) {
        this.preferencias = preferencias;
    }

    public void agregarJuego(String juegoId) {
        if (juegoId != null && !juegosIds.contains(juegoId)) {
            this.juegosIds.add(juegoId);
        }
    }

    public void eliminarJuego(String juegoId) {
        this.juegosIds.remove(juegoId);
    }
}