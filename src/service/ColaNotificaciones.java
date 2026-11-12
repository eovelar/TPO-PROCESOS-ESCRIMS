
package service;

import observer.EventoScrim;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

public class ColaNotificaciones {

    private record Pendiente(
            EventoScrim evento,
            int intentos
    ) {}

    private final Queue<Pendiente> cola = new ArrayDeque<>();
    private final Consumer<EventoScrim> enviar;
    private final int maxIntentos;

    private int enviadas;
    private int fallidas;

    public ColaNotificaciones(
            Consumer<EventoScrim> enviar,
            int maxIntentos
    ) {

        if (enviar == null) {
            throw new IllegalArgumentException(
                    "El envio no puede ser nulo"
            );
        }

        if (maxIntentos < 1) {
            throw new IllegalArgumentException(
                    "Debe permitirse al menos un intento"
            );
        }

        this.enviar = enviar;
        this.maxIntentos = maxIntentos;
    }

    public synchronized void agregar(EventoScrim evento) {

        if (evento == null) {
            throw new IllegalArgumentException(
                    "El evento no puede ser nulo"
            );
        }

        cola.add(new Pendiente(evento, 0));
    }

    public synchronized void procesarPendientes() {

        // procesa una vez cada aviso pendiente
        int cantidad = cola.size();

        for (int i = 0; i < cantidad; i++) {

            Pendiente pendiente = cola.poll();

            if (pendiente == null) {
                break;
            }

            try {

                enviar.accept(pendiente.evento());
                enviadas++;

                System.out.println(
                        "Notificacion enviada para scrim #"
                                + pendiente.evento().scrimId()
                );

            } catch (RuntimeException e) {

                int intentos = pendiente.intentos() + 1;

                if (intentos < maxIntentos) {

                    cola.add(
                            new Pendiente(
                                    pendiente.evento(),
                                    intentos
                            )
                    );

                    System.out.println(
                            "Notificacion pendiente de reintento "
                                    + intentos + "/" + maxIntentos
                    );

                } else {

                    fallidas++;

                    System.err.println(
                            "No se pudo enviar la notificacion "
                                    + "de la scrim #"
                                    + pendiente.evento().scrimId()
                    );
                }
            }
        }
    }

    public synchronized int cantidadPendientes() {
        return cola.size();
    }

    public synchronized int cantidadEnviadas() {
        return enviadas;
    }

    public synchronized int cantidadFallidas() {
        return fallidas;
    }
}
