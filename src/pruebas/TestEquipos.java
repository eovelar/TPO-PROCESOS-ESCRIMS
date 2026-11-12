
package pruebas;

import domain.Equipo;
import domain.Scrim;
import domain.Usuario;
import service.ScrimEquipoService;

import java.time.LocalDateTime;
import java.util.List;

public class TestEquipos {

    public static void main(String[] args) {

        probarCapacidadMaxima();
        probarJugadorDuplicado();
        probarAsignacionRoles();
        probarIntercambioJugadores();
        probarEquiposDuplicados();
        probarCopiasEquipos();
        probarModificacionDurantePartida();

        System.out.println("\nTodas las pruebas de equipos pasaron correctamente");
    }

    private static void probarCapacidadMaxima() {

        Equipo equipo = new Equipo(1, 1, 2);

        equipo.agregarJugador(crearJugador(1));
        equipo.agregarJugador(crearJugador(2));

        verificar(equipo.getCantidadJugadores() == 2,
                "El equipo debe tener dos jugadores");

        verificar(equipo.getCuposDisponibles() == 0,
                "El equipo debe estar completo");

        boolean rechazado = false;

        try {
            equipo.agregarJugador(crearJugador(3));
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se debe superar la capacidad del equipo");

        System.out.println("Capacidad maxima: OK");
    }

    private static void probarJugadorDuplicado() {

        Equipo equipo = new Equipo(2, 1, 2);
        Usuario jugador = crearJugador(4);

        equipo.agregarJugador(jugador);

        boolean rechazado = false;

        try {
            equipo.agregarJugador(crearJugador(4));
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se debe agregar dos veces al mismo jugador");

        verificar(equipo.getCantidadJugadores() == 1,
                "El equipo debe conservar un solo jugador");

        System.out.println("Jugador duplicado: OK");
    }

    private static void probarAsignacionRoles() {

        Equipo equipo = new Equipo(3, 1, 2);
        Usuario jugador = crearJugador(5);

        equipo.agregarJugador(jugador);
        equipo.asignarRol(jugador, "Duelist");

        verificar("Duelist".equals(equipo.getRolJugador(jugador)),
                "El rol debe quedar asignado");

        boolean rechazado = false;

        try {
            equipo.asignarRol(crearJugador(6), "Controller");
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se deben asignar roles a jugadores ajenos");

        System.out.println("Asignacion de roles: OK");
    }

    private static void probarIntercambioJugadores() {

        Equipo equipo1 = new Equipo(4, 1, 1);
        Equipo equipo2 = new Equipo(5, 2, 1);

        Usuario jugador1 = crearJugador(7);
        Usuario jugador2 = crearJugador(8);

        equipo1.agregarJugador(jugador1);
        equipo2.agregarJugador(jugador2);

        equipo1.asignarRol(jugador1, "Duelist");
        equipo2.asignarRol(jugador2, "Controller");

        equipo1.intercambiarJugadores(
                equipo2, jugador1, jugador2
        );

        verificar(equipo1.contieneJugador(jugador2),
                "El jugador 2 debe estar en el equipo 1");

        verificar(equipo2.contieneJugador(jugador1),
                "El jugador 1 debe estar en el equipo 2");

        verificar("Controller".equals(equipo1.getRolJugador(jugador2)),
                "El jugador 2 debe conservar su rol");

        verificar("Duelist".equals(equipo2.getRolJugador(jugador1)),
                "El jugador 1 debe conservar su rol");

        System.out.println("Intercambio de jugadores: OK");
    }

    private static void probarEquiposDuplicados() {

        Scrim scrim = crearScrim(10);

        scrim.registrarJugadorAceptado(crearJugador(9));
        scrim.registrarJugadorAceptado(crearJugador(10));

        ScrimEquipoService servicio = new ScrimEquipoService();

        servicio.armarEquipos(scrim);

        verificar(scrim.getEquipos().size() == 2,
                "El scrim debe tener dos equipos");

        boolean rechazado = false;

        try {
            servicio.armarEquipos(scrim);
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se deben armar los equipos dos veces");

        System.out.println("Equipos duplicados: OK");
    }

    private static void probarCopiasEquipos() {

        Scrim scrim = crearScrim(11);

        Usuario jugador1 = crearJugador(11);
        Usuario jugador2 = crearJugador(12);

        scrim.registrarJugadorAceptado(jugador1);
        scrim.registrarJugadorAceptado(jugador2);

        ScrimEquipoService servicio = new ScrimEquipoService();
        servicio.armarEquipos(scrim);

        scrim.asignarRol(1, jugador1, "Duelist");

        Equipo copia = scrim.getEquipos().get(0);

        copia.asignarRol(jugador1, "Sentinel");

        String rolOriginal = scrim.getEquipos().get(0)
                .getRolJugador(jugador1);

        verificar("Duelist".equals(rolOriginal),
                "Modificar una copia no debe afectar al equipo original");

        System.out.println("Copias de equipos: OK");
    }

    private static void probarModificacionDurantePartida() {

        Scrim scrim = crearScrim(12);

        Usuario jugador1 = crearJugador(13);
        Usuario jugador2 = crearJugador(14);

        scrim.registrarJugadorAceptado(jugador1);
        scrim.registrarJugadorAceptado(jugador2);

        ScrimEquipoService servicio = new ScrimEquipoService();

        List<Equipo> equipos = servicio.armarEquipos(scrim);

        scrim.confirmarJugador(jugador1);
        scrim.confirmarJugador(jugador2);

        verificar("Confirmado".equals(scrim.getEstadoActual()),
                "El scrim debe estar confirmado");

        scrim.iniciar();

        verificar("En juego".equals(scrim.getEstadoActual()),
                "El scrim debe estar en juego");

        boolean rolRechazado = false;

        try {
            servicio.asignarRol(
                    scrim, equipos.get(0), jugador1, "Sentinel"
            );
        } catch (IllegalStateException e) {
            rolRechazado = true;
        }

        verificar(rolRechazado,
                "No se deben modificar roles durante la partida");

        boolean intercambioRechazado = false;

        try {
            servicio.intercambiarJugadores(
                    scrim,
                    equipos.get(0),
                    equipos.get(1),
                    jugador1,
                    jugador2
            );
        } catch (IllegalStateException e) {
            intercambioRechazado = true;
        }

        verificar(intercambioRechazado,
                "No se deben intercambiar jugadores durante la partida");

        System.out.println("Modificaciones durante partida: OK");
    }

    private static Scrim crearScrim(int id) {

        return new Scrim(
                id,
                "Valorant",
                "1v1",
                "LAS",
                4,
                7,
                80,
                LocalDateTime.now().minusMinutes(5),
                30,
                2
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

    private static void verificar(boolean condicion, String mensaje) {

        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
