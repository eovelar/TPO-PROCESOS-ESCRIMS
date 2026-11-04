
package pruebas;

import domain.Postulacion;
import domain.Scrim;
import domain.Usuario;
import service.PostulacionService;

import java.time.LocalDateTime;

public class TestScrimState {

    public static void main(String[] args) {

        probarCicloCompleto();
        probarCancelacion();
        probarInicioAnticipado();
        probarJugadorDuplicado();

        System.out.println("\nTodas las pruebas de State pasaron correctamente");
    }

    private static void probarCicloCompleto() {

        Scrim scrim = crearScrim(1, -5);

        Usuario jugador1 = crearJugador(1);
        Usuario jugador2 = crearJugador(2);

        PostulacionService servicio = new PostulacionService();

        verificarEstado(scrim, "Buscando jugadores");

        Postulacion p1 = servicio.crearPostulacion(
                1, jugador1, scrim, "Duelist"
        );

        Postulacion p2 = servicio.crearPostulacion(
                2, jugador2, scrim, "Controller"
        );

        servicio.aceptarPostulacion(p1);
        verificarEstado(scrim, "Buscando jugadores");

        servicio.aceptarPostulacion(p2);
        verificarEstado(scrim, "Lobby armado");

        scrim.confirmarJugador(jugador1);
        verificarEstado(scrim, "Lobby armado");

        scrim.confirmarJugador(jugador2);
        verificarEstado(scrim, "Confirmado");

        scrim.iniciar();
        verificarEstado(scrim, "En juego");

        scrim.finalizar();
        verificarEstado(scrim, "Finalizado");

        System.out.println("Ciclo completo: OK");
    }

    private static void probarCancelacion() {

        Scrim scrim = crearScrim(2, 10);

        scrim.cancelar();

        verificarEstado(scrim, "Cancelado");

        boolean rechazado = false;

        try {
            scrim.iniciar();
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "Una partida cancelada no debe poder iniciarse");

        System.out.println("Cancelacion: OK");
    }

    private static void probarInicioAnticipado() {

        Scrim scrim = crearScrim(3, 60);

        Usuario jugador1 = crearJugador(3);
        Usuario jugador2 = crearJugador(4);

        PostulacionService servicio = new PostulacionService();

        Postulacion p1 = servicio.crearPostulacion(
                3, jugador1, scrim, "Duelist"
        );

        Postulacion p2 = servicio.crearPostulacion(
                4, jugador2, scrim, "Controller"
        );

        servicio.aceptarPostulacion(p1);
        servicio.aceptarPostulacion(p2);

        scrim.confirmarJugador(jugador1);
        scrim.confirmarJugador(jugador2);

        verificarEstado(scrim, "Confirmado");

        boolean rechazado = false;

        try {
            scrim.iniciar();
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se debe iniciar antes de la fecha programada");

        verificarEstado(scrim, "Confirmado");

        System.out.println("Inicio anticipado: OK");
    }

    private static void probarJugadorDuplicado() {

        Scrim scrim = crearScrim(4, 30);

        Usuario jugador = crearJugador(5);

        scrim.registrarJugadorAceptado(jugador);

        boolean rechazado = false;

        try {
            scrim.registrarJugadorAceptado(jugador);
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se debe aceptar dos veces al mismo jugador");

        verificar(scrim.getJugadoresAceptados() == 1,
                "La cantidad de jugadores no debe aumentar");

        System.out.println("Jugadores duplicados: OK");
    }

    private static Scrim crearScrim(int id, int minutos) {

        return new Scrim(
                id, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.now().plusMinutes(minutos),
                30, 2
        );
    }

    private static Usuario crearJugador(int id) {

        return new Usuario(
                id,
                "Jugador" + id,
                "jugador" + id + "@mail.com",
                "hash123",
                "LAS",
                5
        );
    }

    private static void verificarEstado(Scrim scrim, String esperado) {

        verificar(
                scrim.getEstadoActual().equals(esperado),
                "Se esperaba " + esperado
                        + " pero se obtuvo " + scrim.getEstadoActual()
        );
    }

    private static void verificar(boolean condicion, String mensaje) {

        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
