
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

    public NotificacionScrimService(boolean produccion) {

        fabrica = produccion
                ? new FabricaProduccion()
                : new FabricaDesarrollo();
    }

    // registra los canales que reciben los avisos
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

    // avisa cuando una partida cambia de estado
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

        gestor.notificar(evento);
    }
}
