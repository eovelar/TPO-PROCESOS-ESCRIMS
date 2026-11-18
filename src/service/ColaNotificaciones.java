
package service;

import observer.EventoScrim;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

public class ColaNotificaciones {

    private record Pendiente(
            EventoScrim evento,
            int intentos,
            long noAntesDeMs
    ) {}

    private final Queue<Pendiente> cola = new ArrayDeque<>();
    private final Consumer<EventoScrim> enviar;
    private final int maxIntentos;

    // base del retroceso exponencial en milisegundos
    private static final long BASE_ESPERA_MS = 1000;

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

        cola.add(new Pendiente(evento, 0, 0));
    }

    public synchronized void procesarPendientes() {

        // procesa una vez cada aviso pendiente
        int cantidad = cola.size();

        long ahora = System.currentTimeMillis();

        for (int i = 0; i < cantidad; i++) {

            Pendiente pendiente = cola.poll();

            if (pendiente == null) {
                break;
            }

            // reintento exponencial: si todavia no llego el momento,
            // vuelve al final de la cola
            if (pendiente.noAntesDeMs() > ahora) {
                cola.add(pendiente);
                continue;
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

                    // espera exponencial: 1s, 2s, 4s, ...
                    long espera = BASE_ESPERA_MS * (1L << (intentos - 1));

                    cola.add(
                            new Pendiente(
                                    pendiente.evento(),
                                    intentos,
                                    System.currentTimeMillis() + espera
                            )
                    );

                    System.out.println(
                            "Notificacion pendiente de reintento "
                                    + intentos + "/" + maxIntentos
                                    + " (espera " + espera + " ms)"
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
