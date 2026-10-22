package service;

import domain.Equipo;
import domain.Scrim;
import domain.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ScrimEquipoService {

    public List<Equipo> armarEquipos(Scrim scrim) {

        if (scrim == null) {
            throw new IllegalArgumentException(
                    "El scrim no puede ser nulo"
            );
        }

        if (!scrim.getEstadoActual().equals("Lobby armado")) {
            throw new IllegalStateException(
                    "El lobby debe estar completo para armar los equipos"
            );
        }

        if (scrim.tieneEquiposArmados()) {
            throw new IllegalStateException(
                    "Los equipos ya fueron armados"
            );
        }

        int cupos = scrim.getCupos();

        if (cupos % 2 != 0) {
            throw new IllegalArgumentException(
                    "La cantidad de jugadores debe ser par"
            );
        }

        int jugadoresPorEquipo = cupos / 2;

        Equipo equipo1 = new Equipo(
                scrim.getId() * 10 + 1, 1, jugadoresPorEquipo
        );

        Equipo equipo2 = new Equipo(
                scrim.getId() * 10 + 2, 2, jugadoresPorEquipo
        );

        List<Usuario> jugadores = scrim.getJugadores();

        for (int i = 0; i < jugadores.size(); i++) {
            if (i < jugadoresPorEquipo) {
                equipo1.agregarJugador(jugadores.get(i));
            } else {
                equipo2.agregarJugador(jugadores.get(i));
            }
        }

        List<Equipo> equipos = new ArrayList<>();
        equipos.add(equipo1);
        equipos.add(equipo2);

        scrim.guardarEquipos(equipos);

        return scrim.getEquipos();
    }

    public void asignarRol(Scrim scrim, Equipo equipo,
                           Usuario jugador, String rol) {

        validarEquipo(scrim, equipo);

        scrim.asignarRol(
                equipo.getLado(), jugador, rol
        );
    }

    public void intercambiarJugadores(Scrim scrim,
                                      Equipo equipo1, Equipo equipo2,
                                      Usuario jugador1, Usuario jugador2) {

        validarEquipo(scrim, equipo1);
        validarEquipo(scrim, equipo2);

        if (equipo1.getLado() == equipo2.getLado()) {
            throw new IllegalArgumentException(
                    "Se deben seleccionar dos equipos diferentes"
            );
        }

        scrim.intercambiarJugadores(
                equipo1.getLado(),
                equipo2.getLado(),
                jugador1,
                jugador2
        );
    }

    private void validarEquipo(Scrim scrim, Equipo equipo) {

        if (scrim == null || equipo == null) {
            throw new IllegalArgumentException(
                    "El scrim y el equipo son obligatorios"
            );
        }

        boolean pertenece = false;

        for (Equipo guardado : scrim.getEquipos()) {
            if (guardado.getId() == equipo.getId()
                    && guardado.getLado() == equipo.getLado()) {
                pertenece = true;
                break;
            }
        }

        if (!pertenece) {
            throw new IllegalArgumentException(
                    "El equipo no pertenece a este scrim"
            );
        }
    }
}
