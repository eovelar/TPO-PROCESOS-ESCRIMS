
import domain.Usuario;
import domain.Scrim;
import domain.Postulacion;
import service.PostulacionService;

import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        Usuario jugador1 = new Usuario(
                1, "Jugador1", "jugador1@mail.com",
                "hash123", "LAS", 5
        );

        Usuario jugador2 = new Usuario(
                2, "Jugador2", "jugador2@mail.com",
                "hash456", "LAS", 2
        );

        Scrim scrim = new Scrim(
                1, "Valorant", "5v5", "LAS",
                4, 7, 80,
                LocalDateTime.of(2026, 11, 5, 20, 0),
                60, 10
        );

        PostulacionService servicio = new PostulacionService();

        System.out.println("Jugador 1 cumple requisitos: "
                + servicio.cumpleRequisitos(jugador1, scrim));

        System.out.println("Jugador 2 cumple requisitos: "
                + servicio.cumpleRequisitos(jugador2, scrim));

        Postulacion postulacion = servicio.crearPostulacion(
                1, jugador1, scrim, "Duelist"
        );

        System.out.println("Postulacion creada para: "
                + postulacion.getUsuario().getUsername());

        System.out.println("Estado: " + postulacion.getEstado());
    }
}
