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

        System.out.println("--- Prueba de equipos ---");

        System.out.println("Estado: " + scrim.getEstadoActual());

        ScrimEquipoService equipoService =
                new ScrimEquipoService();

        equipoService.armarEquipos(scrim);

        System.out.println("Equipos guardados: "
                + scrim.getEquipos().size());

        List<Equipo> equipos = scrim.getEquipos();

        for (Equipo equipo : equipos) {
            System.out.println("\nEquipo " + equipo.getLado());

            for (Usuario jugador : equipo.getJugadores()) {
                System.out.println("- " + jugador.getUsername());
            }
        }

        System.out.println("\n--- Prueba de duplicacion ---");

        try {
            equipoService.armarEquipos(scrim);
        } catch (IllegalStateException e) {
            System.out.println("Operacion rechazada: "
                    + e.getMessage());
        }

        System.out.println("Cantidad final de equipos: "
                + scrim.getEquipos().size());
    }
}
