
package domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Equipo {

    private int id;
    private int lado;
    private int capacidadMaxima;
    private List<Usuario> jugadores;
    private Map<Integer, String> roles;

    public Equipo(int id, int lado) {
        this(id, lado, 5);
    }

    public Equipo(int id, int lado, int capacidadMaxima) {
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
        if (usuario == null || !contieneJugador(usuario)) {
            throw new IllegalArgumentException(
                    "El jugador no pertenece al equipo"
            );
        }

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

        roles.put(usuario.getId(), rol);
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

        if (!contieneJugador(jugadorPropio) ||
                !otroEquipo.contieneJugador(jugadorOtro)) {
            throw new IllegalArgumentException(
                    "Los jugadores deben pertenecer a sus equipos"
            );
        }

        if (otroEquipo.contieneJugador(jugadorPropio) ||
                contieneJugador(jugadorOtro)) {
            throw new IllegalArgumentException(
                    "El intercambio generaria jugadores duplicados"
            );
        }

        String rolPropio = roles.get(jugadorPropio.getId());
        String rolOtro = otroEquipo.roles.get(jugadorOtro.getId());

        eliminarJugador(jugadorPropio);
        otroEquipo.eliminarJugador(jugadorOtro);

        agregarJugador(jugadorOtro);
        otroEquipo.agregarJugador(jugadorPropio);

        if (rolOtro != null) {
            asignarRol(jugadorOtro, rolOtro);
        }

        if (rolPropio != null) {
            otroEquipo.asignarRol(jugadorPropio, rolPropio);
        }
    }
}
