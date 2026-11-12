
package service;

import factory.FabricaDesarrollo;
import factory.FabricaNotificadores;
import factory.FabricaProduccion;
import observer.EventoScrim;
import observer.GestorEventosScrim;

public class NotificacionScrimService {

    private final GestorEventosScrim gestor =
            new GestorEventosScrim();

    private final FabricaNotificadores fabrica;
    private final ColaNotificaciones cola;

    public NotificacionScrimService(boolean produccion) {

        fabrica = produccion
                ? new FabricaProduccion()
                : new FabricaDesarrollo();

        cola = new ColaNotificaciones(
                gestor::notificar,
                3
        );
    }

    public void configurar(
            String jugador,
            String organizador
    ) {

        gestor.suscribir(
                fabrica.crearNotificadorJugador(jugador)
        );

        gestor.suscribir(
                fabrica.crearNotificadorOrganizador(organizador)
        );
    }

    public void notificarCambio(
            int scrimId,
            String juego,
            String estadoAnterior,
            String estadoNuevo
    ) {

        if (estadoAnterior.equals(estadoNuevo)) {
            return;
        }

        EventoScrim evento = new EventoScrim(
                scrimId,
                juego,
                estadoAnterior,
                estadoNuevo
        );

        cola.agregar(evento);

        // intenta enviar los avisos pendientes
        for (int i = 0; i < 3
                && cola.cantidadPendientes() > 0; i++) {

            cola.procesarPendientes();
        }
    }

    public int notificacionesPendientes() {
        return cola.cantidadPendientes();
    }

    public int notificacionesEnviadas() {
        return cola.cantidadEnviadas();
    }

    public int notificacionesFallidas() {
        return cola.cantidadFallidas();
    }
}
