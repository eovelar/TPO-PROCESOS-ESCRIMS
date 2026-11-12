
package domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Usuario {

    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String region;
    private int rango;

    private String juegoPrincipal;
    private final Map<String, Integer> rangosPorJuego;
    private final List<String> rolesPreferidos;
    private LocalTime disponibleDesde;
    private LocalTime disponibleHasta;
    private boolean emailVerificado;

    public Usuario(int id, String username, String email,
                   String passwordHash, String region, int rango) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser mayor a cero"
            );
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio"
            );
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException(
                    "El email no es valido"
            );
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria"
            );
        }

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException(
                    "La region es obligatoria"
            );
        }

        if (rango < 0) {
            throw new IllegalArgumentException(
                    "El rango no puede ser negativo"
            );
        }

        this.id = id;
        this.username = username.trim();
        this.email = email.trim();
        this.passwordHash = passwordHash;
        this.region = region.trim();
        this.rango = rango;

        this.rangosPorJuego = new HashMap<>();
        this.rolesPreferidos = new ArrayList<>();
        this.emailVerificado = false;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRegion() {
        return region;
    }

    public int getRango() {
        return rango;
    }

    public String getJuegoPrincipal() {
        return juegoPrincipal;
    }

    public Map<String, Integer> getRangosPorJuego() {
        return new HashMap<>(rangosPorJuego);
    }

    public List<String> getRolesPreferidos() {
        return new ArrayList<>(rolesPreferidos);
    }

    public LocalTime getDisponibleDesde() {
        return disponibleDesde;
    }

    public LocalTime getDisponibleHasta() {
        return disponibleHasta;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void setJuegoPrincipal(String juegoPrincipal) {

        if (juegoPrincipal == null || juegoPrincipal.isBlank()) {
            throw new IllegalArgumentException(
                    "El juego principal es obligatorio"
            );
        }

        this.juegoPrincipal = juegoPrincipal.trim();
    }

    public void agregarRango(String juego, int rango) {

        if (juego == null || juego.isBlank()) {
            throw new IllegalArgumentException(
                    "El juego es obligatorio"
            );
        }

        if (rango < 0) {
            throw new IllegalArgumentException(
                    "El rango no puede ser negativo"
            );
        }

        // guarda un rango diferente para cada videojuego
        rangosPorJuego.put(
                juego.trim().toLowerCase(Locale.ROOT), rango
        );
    }

    public Integer getRangoPorJuego(String juego) {

        if (juego == null || juego.isBlank()) {
            return null;
        }

        return rangosPorJuego.get(
                juego.trim().toLowerCase(Locale.ROOT)
        );
    }

    public void agregarRolPreferido(String rol) {

        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException(
                    "El rol no puede estar vacio"
            );
        }

        String nuevoRol = rol.trim();

        // evita guardar dos veces el mismo rol
        for (String existente : rolesPreferidos) {
            if (existente.equalsIgnoreCase(nuevoRol)) {
                return;
            }
        }

        rolesPreferidos.add(nuevoRol);
    }

    public void establecerDisponibilidad(LocalTime desde,
                                         LocalTime hasta) {

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "El horario de disponibilidad es obligatorio"
            );
        }

        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException(
                    "El horario de inicio debe ser anterior al de fin"
            );
        }

        this.disponibleDesde = desde;
        this.disponibleHasta = hasta;
    }

    public void verificarEmail() {
        this.emailVerificado = true;
    }

    public void cambiarRegion(String region) {

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException(
                    "La region no puede estar vacia"
            );
        }

        this.region = region.trim();
    }
}
