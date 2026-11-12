
package factory;

public class FabricaProduccion implements FabricaNotificadores {

    @Override
    public Notificador crearNotificadorJugador(
            String destinatario
    ) {

        // representa el canal de avisos para jugadores
        return new NotificadorConsola(
                destinatario,
                "PROD - JUGADOR"
        );
    }

    @Override
    public Notificador crearNotificadorOrganizador(
            String destinatario
    ) {

        // representa el canal de avisos para organizadores
        return new NotificadorConsola(
                destinatario,
                "PROD - ORGANIZADOR"
        );
    }
}
