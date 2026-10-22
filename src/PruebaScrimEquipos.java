
import domain.Equipo;
import domain.Postulacion;
import domain.Scrim;
import domain.Usuario;
import service.PostulacionService;
import service.ScrimEquipoService;

import java.time.LocalDateTime;
import java.util.List;

public class PruebaScrimEquipos {

    public static void main(String[] args) {

        Usuario jugador1 = new Usuario(
                1, "Jugador1", "jugador1@mail.com",
                "hash123", "LAS", 5
        );

        Usuario jugador2 = new Usuario(
                2, "Jugador2", "jugador2@mail.com",
                "hash456", "LAS", 6
        );

        Scrim scrim = new Scrim(
                10, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.now().minusMinutes(5),
                30, 2
        );

        PostulacionService postulacionService =
                new PostulacionService();

        Postulacion postulacion1 =
                postulacionService.crearPostulacion(
                        1, jugador1, scrim, "Duelist"
                );

        Postulacion postulacion2 =
                postulacionService.crearPostulacion(
                        2, jugador2, scrim, "Controller"
                );

        postulacionService.aceptarPostulacion(postulacion1);
        postulacionService.aceptarPostulacion(postulacion2);

        ScrimEquipoService equipoService = new ScrimEquipoService();

        List<Equipo> equipos = equipoService.armarEquipos(scrim);

        Equipo equipo1 = equipos.get(0);
        Equipo equipo2 = equipos.get(1);

        System.out.println("--- Equipos iniciales ---");
        mostrarEquipos(scrim);

        System.out.println("\n--- Asignacion de roles ---");

        equipoService.asignarRol(
                scrim, equipo1, jugador1, "Duelist"
        );

        equipoService.asignarRol(
                scrim, equipo2, jugador2, "Controller"
        );

        mostrarEquipos(scrim);

        System.out.println("\n--- Intercambio de jugadores ---");

        equipoService.intercambiarJugadores(
                scrim, equipo1, equipo2, jugador1, jugador2
        );

        mostrarEquipos(scrim);

        System.out.println("\n--- Prueba de copias ---");

        Equipo copia = scrim.getEquipos().get(0);

        copia.asignarRol(jugador2, "Sentinel");

        System.out.println("Rol en la copia: "
                + copia.getRolJugador(jugador2));

        System.out.println("Rol original: "
                + scrim.getEquipos().get(0).getRolJugador(jugador2));

        System.out.println("\n--- Confirmacion e inicio ---");

        scrim.confirmarJugador(jugador1);
        scrim.confirmarJugador(jugador2);

        System.out.println("Estado confirmado: "
                + scrim.getEstadoActual());

        scrim.iniciar();

        System.out.println("Estado actual: "
                + scrim.getEstadoActual());

        System.out.println("\n--- Modificacion durante la partida ---");

        try {
            equipoService.asignarRol(
                    scrim, equipo1, jugador2, "Sentinel"
            );
        } catch (IllegalStateException e) {
            System.out.println("Cambio rechazado: "
                    + e.getMessage());
        }
    }

    private static void mostrarEquipos(Scrim scrim) {

        for (Equipo equipo : scrim.getEquipos()) {

            System.out.println("Equipo " + equipo.getLado());

            for (Usuario jugador : equipo.getJugadores()) {
                System.out.println("- " + jugador.getUsername()
                        + " | Rol: "
                        + equipo.getRolJugador(jugador));
            }
        }
    }
}
