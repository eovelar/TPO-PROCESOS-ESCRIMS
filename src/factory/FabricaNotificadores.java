
package factory;

public interface FabricaNotificadores {

    Notificador crearNotificadorJugador(String destinatario);

    Notificador crearNotificadorOrganizador(String destinatario);
}
