
package pruebas;

import domain.Scrim;
import domain.Usuario;
import service.ListaEsperaService;
import service.PostulacionService;
import service.ScrimEquipoService;

import java.time.LocalDateTime;

public class TestListaEspera {

    public static void main(String[] args) {

        probarRegistroSuplentes();
        probarSuplenteDuplicado();
        probarOrdenListaEspera();
        probarReemplazoTitular();
        probarReconstruccionEquipos();
        probarBajaDurantePartida();
        probarReemplazoSinSuplentes();

        System.out.println(
                "\nTodas las pruebas de lista de espera pasaron correctamente"
        );
    }

    private static void probarRegistroSuplentes() {

        DatosPrueba datos = crearDatos(1);

        datos.listaEspera.agregarSuplente(
                datos.scrim, crearJugador(3)
        );

        verificar(datos.listaEspera.getCantidadSuplentes(datos.scrim) == 1,
                "Debe registrarse un suplente");

        System.out.println("Registro de suplentes: OK");
    }

    private static void probarSuplenteDuplicado() {

        DatosPrueba datos = crearDatos(2);
        Usuario suplente = crearJugador(6);

        datos.listaEspera.agregarSuplente(datos.scrim, suplente);

        boolean rechazado = false;

        try {
            datos.listaEspera.agregarSuplente(
                    datos.scrim, crearJugador(6)
            );
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se debe registrar dos veces al mismo suplente");

        verificar(datos.listaEspera.getCantidadSuplentes(datos.scrim) == 1,
                "La lista debe conservar un solo suplente");

        System.out.println("Suplente duplicado: OK");
    }

    private static void probarOrdenListaEspera() {

        DatosPrueba datos = crearDatos(3);

        Usuario suplente1 = crearJugador(9);
        Usuario suplente2 = crearJugador(10);

        datos.listaEspera.agregarSuplente(datos.scrim, suplente1);
        datos.listaEspera.agregarSuplente(datos.scrim, suplente2);

        verificar(
                datos.listaEspera.obtenerPrimerSuplente(datos.scrim)
                        .getId() == suplente1.getId(),
                "Debe respetarse el orden de inscripcion"
        );

        datos.listaEspera.quitarSuplente(datos.scrim, suplente1);

        verificar(
                datos.listaEspera.obtenerPrimerSuplente(datos.scrim)
                        .getId() == suplente2.getId(),
                "El siguiente suplente debe quedar primero"
        );

        System.out.println("Orden de lista de espera: OK");
    }

    private static void probarReemplazoTitular() {

        DatosPrueba datos = crearDatos(4);
        Usuario suplente = crearJugador(13);

        datos.listaEspera.agregarSuplente(datos.scrim, suplente);

        datos.scrim.confirmarJugador(datos.titular1);
        datos.scrim.confirmarJugador(datos.titular2);

        verificarEstado(datos.scrim, "Confirmado");

        Usuario reemplazo = datos.listaEspera.reemplazarJugador(
                datos.scrim, datos.titular2
        );

        verificar(reemplazo.getId() == suplente.getId(),
                "Debe ingresar el suplente correspondiente");

        verificarEstado(datos.scrim, "Lobby armado");

        verificar(datos.scrim.getJugadoresAceptados() == 2,
                "El scrim debe conservar los dos jugadores");

        verificar(datos.scrim.getCantidadConfirmados() == 0,
                "Las confirmaciones deben reiniciarse");

        verificar(datos.listaEspera.getCantidadSuplentes(datos.scrim) == 0,
                "El suplente debe salir de la lista de espera");

        datos.scrim.confirmarJugador(datos.titular1);
        verificarEstado(datos.scrim, "Lobby armado");

        datos.scrim.confirmarJugador(suplente);
        verificarEstado(datos.scrim, "Confirmado");

        System.out.println("Reemplazo de titular: OK");
    }

    private static void probarReconstruccionEquipos() {

        DatosPrueba datos = crearDatos(5);
        Usuario suplente = crearJugador(16);

        ScrimEquipoService servicioEquipos = new ScrimEquipoService();

        servicioEquipos.armarEquipos(datos.scrim);

        verificar(datos.scrim.getEquipos().size() == 2,
                "Deben existir dos equipos inicialmente");

        datos.listaEspera.agregarSuplente(datos.scrim, suplente);

        datos.listaEspera.reemplazarJugador(
                datos.scrim, datos.titular2
        );

        verificar(datos.scrim.getEquipos().isEmpty(),
                "Los equipos anteriores deben eliminarse");

        servicioEquipos.armarEquipos(datos.scrim);

        verificar(datos.scrim.getEquipos().size() == 2,
                "Deben reconstruirse los dos equipos");

        boolean suplenteAsignado = false;
        boolean titularAnterior = false;

        for (var equipo : datos.scrim.getEquipos()) {

            if (equipo.contieneJugador(suplente)) {
                suplenteAsignado = true;
            }

            if (equipo.contieneJugador(datos.titular2)) {
                titularAnterior = true;
            }
        }

        verificar(suplenteAsignado,
                "El suplente debe pertenecer a un equipo");

        verificar(!titularAnterior,
                "El titular anterior debe quedar fuera de los equipos");

        System.out.println("Reconstruccion de equipos: OK");
    }

    private static void probarBajaDurantePartida() {

        DatosPrueba datos = crearDatos(6);

        datos.scrim.confirmarJugador(datos.titular1);
        datos.scrim.confirmarJugador(datos.titular2);
        datos.scrim.iniciar();

        verificarEstado(datos.scrim, "En juego");

        boolean rechazado = false;

        try {
            datos.scrim.registrarBaja(datos.titular1);
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No se deben registrar bajas durante la partida");

        verificar(datos.scrim.getJugadoresAceptados() == 2,
                "Los participantes no deben cambiar");

        System.out.println("Baja durante partida: OK");
    }

    private static void probarReemplazoSinSuplentes() {

        DatosPrueba datos = crearDatos(7);

        boolean rechazado = false;

        try {
            datos.listaEspera.reemplazarJugador(
                    datos.scrim, datos.titular1
            );
        } catch (IllegalStateException e) {
            rechazado = true;
        }

        verificar(rechazado,
                "No debe permitirse reemplazar sin suplentes");

        verificar(datos.scrim.getJugadoresAceptados() == 2,
                "Los titulares deben conservarse");

        System.out.println("Reemplazo sin suplentes: OK");
    }

    private static DatosPrueba crearDatos(int numero) {

        int base = numero * 3;

        Usuario titular1 = crearJugador(base - 2);
        Usuario titular2 = crearJugador(base - 1);

        Scrim scrim = new Scrim(
                numero,
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

        PostulacionService postulacionService =
                new PostulacionService();

        ListaEsperaService listaEspera =
                new ListaEsperaService(postulacionService);

        scrim.registrarJugadorAceptado(titular1);
        scrim.registrarJugadorAceptado(titular2);

        return new DatosPrueba(
                scrim, titular1, titular2, listaEspera
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

    private static class DatosPrueba {

        private final Scrim scrim;
        private final Usuario titular1;
        private final Usuario titular2;
        private final ListaEsperaService listaEspera;

        private DatosPrueba(Scrim scrim, Usuario titular1,
                            Usuario titular2, ListaEsperaService listaEspera) {

            this.scrim = scrim;
            this.titular1 = titular1;
            this.titular2 = titular2;
            this.listaEspera = listaEspera;
        }
    }
}
