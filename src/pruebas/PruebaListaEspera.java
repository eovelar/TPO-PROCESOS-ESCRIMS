
package pruebas;

import domain.Equipo;
import domain.Scrim;
import domain.Usuario;
import service.ListaEsperaService;
import service.PostulacionService;
import service.ScrimEquipoService;

import java.time.LocalDateTime;

public class PruebaListaEspera {

    public static void main(String[] args) {

        Usuario jugador1 = new Usuario(
                1, "Jugador1", "jugador1@mail.com",
                "hash123", "LAS", 5
        );

        Usuario jugador2 = new Usuario(
                2, "Jugador2", "jugador2@mail.com",
                "hash456", "LAS", 6
        );

        Usuario suplente1 = new Usuario(
                3, "Suplente1", "suplente1@mail.com",
                "hash789", "LAS", 5
        );

        Usuario suplente2 = new Usuario(
                4, "Suplente2", "suplente2@mail.com",
                "hash999", "LAS", 6
        );

        Scrim scrim = new Scrim(
                20, "Valorant", "1v1", "LAS",
                4, 7, 80,
                LocalDateTime.now().plusDays(1),
                30, 2
        );

        PostulacionService postulacionService =
                new PostulacionService();

        ListaEsperaService listaEspera =
                new ListaEsperaService(postulacionService);

        ScrimEquipoService equipoService =
                new ScrimEquipoService();

        System.out.println("--- Registro de titulares ---");

        scrim.registrarJugadorAceptado(jugador1);
        scrim.registrarJugadorAceptado(jugador2);

        System.out.println("Estado: "
                + scrim.getEstadoActual());

        System.out.println("Titulares: "
                + scrim.getJugadoresAceptados());

        equipoService.armarEquipos(scrim);

        System.out.println("Equipos armados: "
                + scrim.getEquipos().size());

        System.out.println("\n--- Lista de espera ---");

        listaEspera.agregarSuplente(scrim, suplente1);
        listaEspera.agregarSuplente(scrim, suplente2);

        System.out.println("Suplentes registrados: "
                + listaEspera.getCantidadSuplentes(scrim));

        System.out.println("Primer suplente: "
                + listaEspera.obtenerPrimerSuplente(scrim).getUsername());

        System.out.println("\n--- Prueba de duplicados ---");

        try {
            listaEspera.agregarSuplente(scrim, suplente1);
        } catch (IllegalArgumentException e) {
            System.out.println("Duplicado rechazado: "
                    + e.getMessage());
        }

        System.out.println("\n--- Confirmacion inicial ---");

        scrim.confirmarJugador(jugador1);
        scrim.confirmarJugador(jugador2);

        System.out.println("Estado: "
                + scrim.getEstadoActual());

        System.out.println("Confirmados: "
                + scrim.getCantidadConfirmados());

        System.out.println("\n--- Reemplazo del titular ---");

        Usuario reemplazo = listaEspera.reemplazarJugador(
                scrim, jugador2
        );

        System.out.println("Jugador que ingreso: "
                + reemplazo.getUsername());

        System.out.println("Estado despues del reemplazo: "
                + scrim.getEstadoActual());

        System.out.println("Titulares actuales:");

        for (Usuario jugador : scrim.getJugadores()) {
            System.out.println("- " + jugador.getUsername());
        }

        System.out.println("Confirmados despues de la baja: "
                + scrim.getCantidadConfirmados());

        System.out.println("Equipos armados despues de la baja: "
                + scrim.getEquipos().size());

        System.out.println("Suplentes restantes: "
                + listaEspera.getCantidadSuplentes(scrim));

        System.out.println("\n--- Reconstruccion de equipos ---");

        equipoService.armarEquipos(scrim);

        for (Equipo equipo : scrim.getEquipos()) {
            System.out.println("Equipo " + equipo.getLado());

            for (Usuario jugador : equipo.getJugadores()) {
                System.out.println("- " + jugador.getUsername());
            }
        }

        System.out.println("\n--- Nuevas confirmaciones ---");

        scrim.confirmarJugador(jugador1);

        System.out.println("Estado con una confirmacion: "
                + scrim.getEstadoActual());

        scrim.confirmarJugador(suplente1);

        System.out.println("Estado final: "
                + scrim.getEstadoActual());

        System.out.println("Confirmados: "
                + scrim.getCantidadConfirmados());

        System.out.println("\n--- Prueba de baja de un usuario ajeno ---");

        try {
            scrim.registrarBaja(jugador2);
        } catch (IllegalArgumentException e) {
            System.out.println("Baja rechazada: "
                    + e.getMessage());
        }
    }
}
