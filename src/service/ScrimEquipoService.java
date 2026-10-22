
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
}
