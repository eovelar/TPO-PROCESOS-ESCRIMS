
package service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LimitadorIntentos {

    private static final int MAX_INTENTOS = 5;

    private static final Duration DURACION =
            Duration.ofMinutes(15);

    private static final int MAX_REGISTROS = 10000;

    private static class Registro {

        private int intentos;
        private Instant inicio;

        public Registro(Instant inicio) {
            this.inicio = inicio;
            this.intentos = 0;
        }
    }

    private final Map<String, Registro> registros =
            new HashMap<>();

    private final Clock reloj;

    private int operaciones;

    public LimitadorIntentos() {
        this(Clock.systemUTC());
    }

    public LimitadorIntentos(Clock reloj) {

        if (reloj == null) {
            throw new IllegalArgumentException(
                    "El reloj no puede ser nulo"
            );
        }

        this.reloj = reloj;
    }

    public synchronized boolean permitirIntento(String clave) {

        if (clave == null || clave.isBlank()) {
            return false;
        }

        Instant ahora = Instant.now(reloj);

        operaciones++;

        if (operaciones % 100 == 0) {
            limpiarVencidos(ahora);
        }

        Registro registro = registros.get(clave);

        if (registro != null &&
                !ahora.isBefore(registro.inicio.plus(DURACION))) {

            registros.remove(clave);
            registro = null;
        }

        if (registro == null) {

            if (registros.size() >= MAX_REGISTROS) {
                limpiarVencidos(ahora);
            }

            if (registros.size() >= MAX_REGISTROS) {
                return false;
            }

            registro = new Registro(ahora);
            registros.put(clave, registro);
        }

        if (registro.intentos >= MAX_INTENTOS) {
            return false;
        }

        registro.intentos++;

        return true;
    }

    public synchronized void reiniciar(String clave) {

        if (clave != null) {
            registros.remove(clave);
        }
    }

    public synchronized int intentosRestantes(String clave) {

        if (clave == null || clave.isBlank()) {
            return 0;
        }

        Registro registro = registros.get(clave);

        if (registro == null) {
            return MAX_INTENTOS;
        }

        Instant ahora = Instant.now(reloj);

        if (!ahora.isBefore(registro.inicio.plus(DURACION))) {
            registros.remove(clave);
            return MAX_INTENTOS;
        }

        return Math.max(
                0,
                MAX_INTENTOS - registro.intentos
        );
    }

    private void limpiarVencidos(Instant ahora) {

        Iterator<Map.Entry<String, Registro>> iterador =
                registros.entrySet().iterator();

        while (iterador.hasNext()) {

            Registro registro = iterador.next().getValue();

            if (!ahora.isBefore(
                    registro.inicio.plus(DURACION))) {

                iterador.remove();
            }
        }
    }
}
