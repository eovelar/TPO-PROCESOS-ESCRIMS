
package pruebas;

import domain.Postulacion;
import domain.Scrim;
import domain.Usuario;
import service.PostulacionService;

import java.time.LocalDateTime;

public class TestPostulaciones {

    public static void main(String[] args) {

        probarPostulacionValida();
        probarRangoInvalido();
        probarPostulacionDuplicada();
        probarAceptacionDuplicada();
        probarControlDeCupos();
        probarLobbyCompleto();
        probarRangoPorJuego();

        System.out.println("\nTodas las pruebas de postulaciones pasaron correctamente");
    }

    private static void probarPostulacionValida() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(1);
        Usuario jugador = crearJugador(1, 5);

        Postulacion postulacion = servicio.crearPostulacion(
                1, jugador, scrim, "Duelist"
        );

        verificar(postulacion.getEstado().equals("Pendiente"),
                "La postulacion debe comenzar pendiente");

        servicio.aceptarPostulacion(postulacion);

        verificar(postulacion.getEstado().equals("Aceptada"),
                "La postulacion deberia estar aceptada");

        verificar(scrim.getJugadoresAceptados() == 1,
                "El scrim deberia tener un jugador");

        System.out.println("Postulacion valida: OK");
    }

    private static void probarRangoInvalido() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(2);
        Usuario jugador = crearJugador(2, 2);

        boolean rechazado = false;

        try {
            servicio.crearPostulacion(
                    2, jugador, scrim, "Duelist"
            );
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "Un jugador fuera de rango no debe postularse");

        verificar(servicio.listarPostulaciones().isEmpty(),
                "No se debe registrar una postulacion invalida");

        System.out.println("Rango invalido: OK");
    }

    private static void probarPostulacionDuplicada() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(3);
        Usuario jugador = crearJugador(3, 5);

        servicio.crearPostulacion(
                3, jugador, scrim, "Duelist"
        );

        boolean rechazado = false;

        try {
            servicio.crearPostulacion(
                    4, jugador, scrim, "Controller"
            );
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se deben permitir postulaciones duplicadas");

        verificar(servicio.listarPostulaciones().size() == 1,
                "Solo debe existir una postulacion");

        System.out.println("Postulacion duplicada: OK");
    }

    private static void probarAceptacionDuplicada() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(4);
        Usuario jugador = crearJugador(4, 5);

        Postulacion postulacion = servicio.crearPostulacion(
                5, jugador, scrim, "Duelist"
        );

        servicio.aceptarPostulacion(postulacion);

        boolean rechazado = false;

        try {
            servicio.aceptarPostulacion(postulacion);
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se debe aceptar dos veces una postulacion");

        verificar(scrim.getJugadoresAceptados() == 1,
                "El jugador debe ocupar un solo cupo");

        System.out.println("Aceptacion duplicada: OK");
    }

    private static void probarControlDeCupos() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(5);

        Usuario jugador1 = crearJugador(5, 5);
        Usuario jugador2 = crearJugador(6, 6);

        Postulacion p1 = servicio.crearPostulacion(
                6, jugador1, scrim, "Duelist"
        );

        Postulacion p2 = servicio.crearPostulacion(
                7, jugador2, scrim, "Controller"
        );

        servicio.aceptarPostulacion(p1);

        verificar(scrim.getCuposDisponibles() == 1,
                "Deberia quedar un cupo disponible");

        verificar(scrim.getEstadoActual().equals("Buscando jugadores"),
                "El scrim debe seguir buscando jugadores");

        servicio.aceptarPostulacion(p2);

        verificar(scrim.getCuposDisponibles() == 0,
                "No deberian quedar cupos");

        verificar(scrim.getEstadoActual().equals("Lobby armado"),
                "El scrim debe pasar a Lobby armado");

        System.out.println("Control de cupos: OK");
    }

    private static void probarLobbyCompleto() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(6);

        Usuario jugador1 = crearJugador(7, 5);
        Usuario jugador2 = crearJugador(8, 6);
        Usuario jugador3 = crearJugador(9, 5);

        Postulacion p1 = servicio.crearPostulacion(
                8, jugador1, scrim, "Duelist"
        );

        Postulacion p2 = servicio.crearPostulacion(
                9, jugador2, scrim, "Controller"
        );

        servicio.aceptarPostulacion(p1);
        servicio.aceptarPostulacion(p2);

        boolean rechazado = false;

        try {
            servicio.crearPostulacion(
                    10, jugador3, scrim, "Sentinel"
            );
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se deben aceptar postulaciones con lobby completo");

        verificar(scrim.getJugadoresAceptados() == 2,
                "El scrim no debe superar sus cupos");

        System.out.println("Lobby completo: OK");
    }

    private static void probarRangoPorJuego() {

        PostulacionService servicio = new PostulacionService();
        Scrim scrim = crearScrim(7);

        Usuario jugador = crearJugador(10, 2);
        jugador.agregarRango("Valorant", 5);

        verificar(servicio.cumpleRequisitos(jugador, scrim),
                "Debe utilizar el rango de Valorant");

        Postulacion postulacion = servicio.crearPostulacion(
                11, jugador, scrim, "Duelist"
        );

        verificar(postulacion.getEstado().equals("Pendiente"),
                "La postulacion debe ser valida por el rango del juego");

        System.out.println("Rango por juego: OK");
    }

    private static Scrim crearScrim(int id) {

        return new Scrim(
                id, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.now().plusDays(1),
                30, 2
        );
    }

    private static Usuario crearJugador(int id, int rango) {

        return new Usuario(
                id,
                "Jugador" + id,
                "jugador" + id + "@mail.com",
                "hash123",
                "LAS",
                rango
        );
    }

    private static void verificar(boolean condicion, String mensaje) {

        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
