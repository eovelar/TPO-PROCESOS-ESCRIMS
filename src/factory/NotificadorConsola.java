
package factory;

import observer.EventoScrim;

public class NotificadorConsola implements Notificador {

    private final String destinatario;
    private final String canal;

    public NotificadorConsola(
            String destinatario,
            String canal
    ) {
        this.destinatario = destinatario;
        this.canal = canal;
    }

    @Override
    public String getCanal() {
        return canal;
    }

    @Override
    public void actualizar(EventoScrim evento) {

        // muestra la notificación cuando cambia el estado
        System.out.println(
                "[" + canal + "] " + destinatario
                        + " - " + evento.juego()
                        + " (#" + evento.scrimId() + ")"
                        + ": " + evento.estadoAnterior()
                        + " -> " + evento.estadoNuevo()
        );
    }
}
