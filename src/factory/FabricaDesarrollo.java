
package factory;

public class FabricaDesarrollo implements FabricaNotificadores {

    @Override
    public Notificador crearNotificadorJugador(
            String destinatario
    ) {

        // simula las notificaciones para jugadores
        return new NotificadorConsola(
                destinatario,
                "DEV - JUGADOR"
        );
    }

    @Override
    public Notificador crearNotificadorOrganizador(
            String destinatario
    ) {

        // simula los avisos para organizadores
        return new NotificadorConsola(
                destinatario,
                "DEV - ORGANIZADOR"
        );
    }
}
