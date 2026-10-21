
import domain.Usuario;
import domain.Scrim;
import domain.Postulacion;
import service.PostulacionService;
import service.ScrimService;
import state.ScrimContext;

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

        System.out.println("--- Prueba de postulaciones ---");

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

        System.out.println("\n--- Prueba de ScrimService ---");

        ScrimService scrimService = new ScrimService();
        scrimService.agregarScrim(scrim);

        System.out.println("Cantidad de scrims: "
                + scrimService.listarScrims().size());

        Scrim encontrado = scrimService.buscarPorId(1);

        if (encontrado != null) {
            System.out.println("Scrim encontrado: "
                    + encontrado.getJuego());
            System.out.println("Formato: "
                    + encontrado.getFormato());
        }

        System.out.println("\n--- Prueba del patron State ---");

        ScrimContext contexto = new ScrimContext();

        System.out.println(contexto.getEstadoActual());

        contexto.completarCupos();
        System.out.println(contexto.getEstadoActual());

        contexto.confirmarJugadores();
        System.out.println(contexto.getEstadoActual());

        contexto.iniciar();
        System.out.println(contexto.getEstadoActual());

        contexto.finalizar();
        System.out.println(contexto.getEstadoActual());

        System.out.println("\n--- Prueba de cancelacion ---");

        ScrimContext cancelado = new ScrimContext();

        System.out.println("Estado inicial: "
                + cancelado.getEstadoActual());

        cancelado.cancelar();

        System.out.println("Estado final: "
                + cancelado.getEstadoActual());

        try {
            cancelado.iniciar();
        } catch (IllegalStateException e) {
            System.out.println("Operacion rechazada: "
                    + e.getMessage());
        }
    }
}
