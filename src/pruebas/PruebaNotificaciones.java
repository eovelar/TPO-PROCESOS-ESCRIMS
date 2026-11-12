
package pruebas;

import factory.FabricaDesarrollo;
import factory.FabricaNotificadores;
import factory.Notificador;
import observer.EventoScrim;
import observer.GestorEventosScrim;

public class PruebaNotificaciones {

    public static void main(String[] args) {

        // crea la fabrica de notificaciones
        FabricaNotificadores fabrica = new FabricaDesarrollo();

        Notificador jugador =
                fabrica.crearNotificadorJugador("eva");

        Notificador organizador =
                fabrica.crearNotificadorOrganizador("organizador");

        // registra los observadores
        GestorEventosScrim gestor = new GestorEventosScrim();

        gestor.suscribir(jugador);
        gestor.suscribir(organizador);

        // simula que se completo el lobby
        EventoScrim evento = new EventoScrim(
                1,
                "Valorant",
                "Buscando jugadores",
                "Lobby armado"
        );

        gestor.notificar(evento);

        // el organizador deja de recibir notificaciones
        gestor.desuscribir(organizador);

        gestor.notificar(new EventoScrim(
                1,
                "Valorant",
                "Lobby armado",
                "Confirmado"
        ));
    }
}
