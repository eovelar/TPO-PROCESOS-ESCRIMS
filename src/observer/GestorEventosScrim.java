
package observer;

import java.util.concurrent.CopyOnWriteArrayList;

public class GestorEventosScrim {

    private final CopyOnWriteArrayList<ObservadorScrim> observadores =
            new CopyOnWriteArrayList<>();

    public void suscribir(ObservadorScrim observador) {

        if (observador == null) {
            throw new IllegalArgumentException(
                    "El observador es obligatorio"
            );
        }

        // evita registrar dos veces al mismo observador
        observadores.addIfAbsent(observador);
    }

    public void desuscribir(ObservadorScrim observador) {
        observadores.remove(observador);
    }

    // avisa a los observadores cuando ocurre un evento
    public void notificar(EventoScrim evento) {

        for (ObservadorScrim observador : observadores) {

            try {
                observador.actualizar(evento);

            } catch (RuntimeException e) {

                // un error no debe interrumpir las demás notificaciones
                System.err.println(
                        "Error al notificar: " + e.getMessage()
                );
            }
        }
    }
}
