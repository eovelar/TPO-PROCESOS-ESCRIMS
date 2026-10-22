
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
                LocalDateTime.now().plusDays(1),
                30, 2
        );

        PostulacionService postulacionService = new PostulacionService();

        Postulacion postulacion1 = postulacionService.crearPostulacion(
                1, jugador1, scrim, "Duelist"
        );

        Postulacion postulacion2 = postulacionService.crearPostulacion(
                2, jugador2, scrim, "Controller"
        );

        postulacionService.aceptarPostulacion(postulacion1);
        postulacionService.aceptarPostulacion(postulacion2);

        System.out.println("--- Prueba de equipos del scrim ---");
        System.out.println("Estado: " + scrim.getEstadoActual());
        System.out.println("Jugadores aceptados: "
                + scrim.getJugadoresAceptados());

        ScrimEquipoService equipoService = new ScrimEquipoService();

        List<Equipo> equipos = equipoService.armarEquipos(scrim);

        for (Equipo equipo : equipos) {
            System.out.println("\nEquipo " + equipo.getLado());

            for (Usuario jugador : equipo.getJugadores()) {
                System.out.println("- " + jugador.getUsername());
            }
        }

        System.out.println("\nCantidad de equipos: " + equipos.size());
    }
}
