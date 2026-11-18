
package pruebas;

import service.LimitadorIntentos;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class PruebaLimitadorIntentos {

    private static class RelojPrueba extends Clock {

        private Instant instante;

        public RelojPrueba(Instant instante) {
            this.instante = instante;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(instante, zona);
        }

        @Override
        public Instant instant() {
            return instante;
        }

        public void avanzarSegundos(long segundos) {
            instante = instante.plusSeconds(segundos);
        }
    }

    public static void main(String[] args) {

        RelojPrueba reloj = new RelojPrueba(
                Instant.parse("2026-11-17T20:00:00Z")
        );

        LimitadorIntentos limitador =
                new LimitadorIntentos(reloj);

        String usuario = "jugador@test.com";

        for (int i = 1; i <= 5; i++) {

            comprobar(
                    limitador.permitirIntento(usuario),
                    "El intento " + i + " debe estar permitido"
            );

            comprobar(
                    limitador.intentosRestantes(usuario) == 5 - i,
                    "El contador de intentos es incorrecto"
            );
        }

        comprobar(
                !limitador.permitirIntento(usuario),
                "El sexto intento debe ser bloqueado"
        );

        comprobar(
                limitador.permitirIntento("otro@test.com"),
                "Otra cuenta debe conservar sus intentos"
        );

        reloj.avanzarSegundos(15 * 60);

        comprobar(
                limitador.permitirIntento(usuario),
                "La cuenta debe desbloquearse al vencer el plazo"
        );

        comprobar(
                limitador.intentosRestantes(usuario) == 4,
                "El contador debe reiniciarse"
        );

        limitador.reiniciar(usuario);

        comprobar(
                limitador.intentosRestantes(usuario) == 5,
                "El reinicio debe restaurar los intentos"
        );

        System.out.println(
                "\nPrueba de rate limiting finalizada correctamente"
        );
    }

    private static void comprobar(
            boolean condicion,
            String mensaje
    ) {

        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
