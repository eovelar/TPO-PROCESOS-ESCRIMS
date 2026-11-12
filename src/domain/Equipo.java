
package domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Equipo {

    private final int id;
    private final int lado;
    private final int capacidadMaxima;
    private final List<Usuario> jugadores;
    private final Map<Integer, String> roles;

    public Equipo(int id, int lado) {
        this(id, lado, 5);
    }

    public Equipo(int id, int lado, int capacidadMaxima) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del equipo debe ser mayor a cero"
            );
        }

        if (lado <= 0) {
            throw new IllegalArgumentException(
                    "El lado del equipo debe ser mayor a cero"
            );
        }

        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor a cero"
            );
        }

        this.id = id;
        this.lado = lado;
        this.capacidadMaxima = capacidadMaxima;
        this.jugadores = new ArrayList<>();
        this.roles = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public int getLado() {
        return lado;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public List<Usuario> getJugadores() {
        return new ArrayList<>(jugadores);
    }

    public int getCantidadJugadores() {
        return jugadores.size();
    }

    public int getCuposDisponibles() {
        return capacidadMaxima - jugadores.size();
    }

    public boolean contieneJugador(Usuario usuario) {

        if (usuario == null) {
            return false;
        }

        // compara por id para evitar jugadores repetidos
        for (Usuario jugador : jugadores) {
            if (jugador.getId() == usuario.getId()) {
                return true;
            }
        }

        return false;
    }

    public void agregarJugador(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo"
            );
        }

        if (contieneJugador(usuario)) {
            throw new IllegalArgumentException(
                    "El jugador ya pertenece al equipo"
            );
        }

        if (getCuposDisponibles() == 0) {
            throw new IllegalStateException(
                    "El equipo esta completo"
            );
        }

        jugadores.add(usuario);
    }

    public void eliminarJugador(Usuario usuario) {

        if (!contieneJugador(usuario)) {
            throw new IllegalArgumentException(
                    "El jugador no pertenece al equipo"
            );
        }

        // elimina al jugador y su rol asignado
        jugadores.removeIf(jugador ->
                jugador.getId() == usuario.getId()
        );

        roles.remove(usuario.getId());
    }

    public void asignarRol(Usuario usuario, String rol) {

        if (!contieneJugador(usuario)) {
            throw new IllegalArgumentException(
                    "El jugador no pertenece al equipo"
            );
        }

        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException(
                    "El rol no puede estar vacio"
            );
        }

        roles.put(usuario.getId(), rol.trim());
    }

    public String getRolJugador(Usuario usuario) {

        if (!contieneJugador(usuario)) {
            throw new IllegalArgumentException(
                    "El jugador no pertenece al equipo"
            );
        }

        return roles.get(usuario.getId());
    }

    public void intercambiarJugadores(Equipo otroEquipo,
                                      Usuario jugadorPropio,
                                      Usuario jugadorOtro) {

        if (otroEquipo == null || otroEquipo == this) {
            throw new IllegalArgumentException(
                    "Debe seleccionarse otro equipo"
            );
        }

        if (jugadorPropio == null || jugadorOtro == null) {
            throw new IllegalArgumentException(
                    "Los jugadores son obligatorios"
            );
        }

        if (!contieneJugador(jugadorPropio)
                || !otroEquipo.contieneJugador(jugadorOtro)) {
            throw new IllegalArgumentException(
                    "Los jugadores deben pertenecer a sus equipos"
            );
        }

        if (otroEquipo.contieneJugador(jugadorPropio)
                || contieneJugador(jugadorOtro)) {
            throw new IllegalArgumentException(
                    "El intercambio generaria jugadores duplicados"
            );
        }

        // guarda los roles antes de mover los jugadores
        String rolPropio = roles.get(jugadorPropio.getId());
        String rolOtro = otroEquipo.roles.get(jugadorOtro.getId());

        eliminarJugador(jugadorPropio);
        otroEquipo.eliminarJugador(jugadorOtro);

        agregarJugador(jugadorOtro);
        otroEquipo.agregarJugador(jugadorPropio);

        // cada jugador conserva su rol despues del intercambio
        if (rolOtro != null) {
            asignarRol(jugadorOtro, rolOtro);
        }

        if (rolPropio != null) {
            otroEquipo.asignarRol(jugadorPropio, rolPropio);
        }
    }

    public Equipo copiar() {

        Equipo copia = new Equipo(id, lado, capacidadMaxima);

        // crea listas independientes para no modificar el original
        copia.jugadores.addAll(jugadores);
        copia.roles.putAll(roles);

        return copia;
    }
}
