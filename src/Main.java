
import domain.Usuario;
import domain.Scrim;
import domain.Postulacion;
import service.PostulacionService;
import service.ScrimService;
import service.ScrimScheduler;
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

        Usuario jugador3 = new Usuario(
                3, "Jugador3", "jugador3@mail.com",
                "hash789", "LAS", 6
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

        System.out.println("\n--- Prueba de cupos ---");

        Scrim scrimPrueba = new Scrim(
                2, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.of(2026, 11, 6, 20, 0),
                30, 2
        );

        System.out.println("Estado inicial: "
                + scrimPrueba.getEstadoActual());

        scrimPrueba.registrarJugadorAceptado(jugador1);

        System.out.println("Jugadores aceptados: "
                + scrimPrueba.getJugadoresAceptados());

        System.out.println("Estado con un jugador: "
                + scrimPrueba.getEstadoActual());

        try {
            scrimPrueba.registrarJugadorAceptado(jugador1);
        } catch (IllegalArgumentException e) {
            System.out.println("Jugador duplicado: "
                    + e.getMessage());
        }

        scrimPrueba.registrarJugadorAceptado(jugador3);

        System.out.println("Jugadores aceptados: "
                + scrimPrueba.getJugadoresAceptados());

        System.out.println("Cupos disponibles: "
                + scrimPrueba.getCuposDisponibles());

        System.out.println("Estado final: "
                + scrimPrueba.getEstadoActual());

        System.out.println("Jugadores del scrim:");

        for (Usuario jugador : scrimPrueba.getJugadores()) {
            System.out.println("- " + jugador.getUsername());
        }

        System.out.println("\n--- Prueba de aceptacion de postulaciones ---");

        Scrim scrimPostulaciones = new Scrim(
                3, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.of(2026, 11, 7, 20, 0),
                30, 2
        );

        Postulacion nuevaPostulacion = servicio.crearPostulacion(
                2, jugador1, scrimPostulaciones, "Duelist"
        );

        System.out.println("Estado inicial: "
                + nuevaPostulacion.getEstado());

        System.out.println("Cupos disponibles antes: "
                + scrimPostulaciones.getCuposDisponibles());

        servicio.aceptarPostulacion(nuevaPostulacion);

        System.out.println("Estado final: "
                + nuevaPostulacion.getEstado());

        System.out.println("Cupos disponibles despues: "
                + scrimPostulaciones.getCuposDisponibles());

        System.out.println("Jugadores aceptados: "
                + scrimPostulaciones.getJugadoresAceptados());

        System.out.println("\n--- Prueba de confirmaciones ---");

        Scrim scrimConfirmaciones = new Scrim(
                4, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.of(2026, 11, 8, 20, 0),
                30, 2
        );

        Postulacion postulacion1 = servicio.crearPostulacion(
                3, jugador1, scrimConfirmaciones, "Duelist"
        );

        Postulacion postulacion2 = servicio.crearPostulacion(
                4, jugador3, scrimConfirmaciones, "Controller"
        );

        servicio.aceptarPostulacion(postulacion1);
        servicio.aceptarPostulacion(postulacion2);

        System.out.println("Estado con cupos completos: "
                + scrimConfirmaciones.getEstadoActual());

        scrimConfirmaciones.confirmarJugador(jugador1);

        System.out.println("Confirmados: "
                + scrimConfirmaciones.getCantidadConfirmados());

        System.out.println("Estado con una confirmacion: "
                + scrimConfirmaciones.getEstadoActual());

        scrimConfirmaciones.confirmarJugador(jugador3);

        System.out.println("Confirmados: "
                + scrimConfirmaciones.getCantidadConfirmados());

        System.out.println("Estado final: "
                + scrimConfirmaciones.getEstadoActual());

        System.out.println("\n--- Prueba de inicio de partida ---");

        try {
            scrimConfirmaciones.iniciar();

            System.out.println("Estado: "
                    + scrimConfirmaciones.getEstadoActual());

        } catch (IllegalStateException e) {
            System.out.println("Inicio rechazado: "
                    + e.getMessage());
        }

        System.out.println("\n--- Prueba de inicio permitido ---");

        Scrim scrimPasado = new Scrim(
                5, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.now().minusMinutes(5),
                30, 2
        );

        Postulacion p1 = servicio.crearPostulacion(
                5, jugador1, scrimPasado, "Duelist"
        );

        Postulacion p2 = servicio.crearPostulacion(
                6, jugador3, scrimPasado, "Controller"
        );

        servicio.aceptarPostulacion(p1);
        servicio.aceptarPostulacion(p2);

        scrimPasado.confirmarJugador(jugador1);
        scrimPasado.confirmarJugador(jugador3);

        System.out.println("Estado antes de iniciar: "
                + scrimPasado.getEstadoActual());

        scrimPasado.iniciar();

        System.out.println("Estado despues de iniciar: "
                + scrimPasado.getEstadoActual());

        System.out.println("\n--- Prueba del scheduler ---");

        ScrimService servicioAutomatico = new ScrimService();

        Scrim scrimAutomatico = new Scrim(
                6, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.now().minusMinutes(1),
                30, 2
        );

        Postulacion auto1 = servicio.crearPostulacion(
                7, jugador1, scrimAutomatico, "Duelist"
        );

        Postulacion auto2 = servicio.crearPostulacion(
                8, jugador3, scrimAutomatico, "Controller"
        );

        servicio.aceptarPostulacion(auto1);
        servicio.aceptarPostulacion(auto2);

        scrimAutomatico.confirmarJugador(jugador1);
        scrimAutomatico.confirmarJugador(jugador3);

        servicioAutomatico.agregarScrim(scrimAutomatico);

        System.out.println("Estado antes del scheduler: "
                + scrimAutomatico.getEstadoActual());

        ScrimScheduler scheduler = new ScrimScheduler(servicioAutomatico);

        try {
            scheduler.iniciarScheduler();

            Thread.sleep(1500);

            System.out.println("Estado despues del scheduler: "
                    + scrimAutomatico.getEstadoActual());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Prueba interrumpida");

        } finally {
            scheduler.detenerScheduler();
        }
    }
}
