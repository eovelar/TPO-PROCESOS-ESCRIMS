
package domain;

import java.util.ArrayList;
import java.util.List;

public class Equipo {

    private int id;
    private int lado;
    private List<Usuario> jugadores;

    public Equipo(int id, int lado) {
        this.id = id;
        this.lado = lado;
        this.jugadores = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getLado() {
        return lado;
    }

    public List<Usuario> getJugadores() {
        return new ArrayList<>(jugadores);
    }

    public void agregarJugador(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        if (!jugadores.contains(usuario)) {
            jugadores.add(usuario);
        }
    }

    public void eliminarJugador(Usuario usuario) {
        jugadores.remove(usuario);
    }

    public int getCantidadJugadores() {
        return jugadores.size();
    }
}
