
package pruebas;

import domain.Scrim;
import domain.Usuario;
import matchmaking.Candidato;
import matchmaking.HybridStrategy;
import matchmaking.MatchmakingService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PruebaRendimientoMatchmaking {

    private static final int CANTIDAD_CANDIDATOS = 500;
    private static final int REPETICIONES = 20;
    private static final double LIMITE_MS = 2000;

    public static void main(String[] args) {

        Scrim scrim = new Scrim(
                1,
                "Valorant",
                "5v5",
                "LAS",
                3,
                7,
                80,
                LocalDateTime.now().plusDays(2),
                60,
                10
        );

        List<Candidato> candidatos = new ArrayList<>();

        for (int i = 1; i <= CANTIDAD_CANDIDATOS; i++) {

            String region = i % 7 == 0 ? "BR" : "LAS";
            int rango = (i % 10) + 1;
            int latencia = 20 + (i % 100);
            int compatibilidad = 50 + (i % 51);
            int abandonos = i % 5;

            Usuario usuario = new Usuario(
                    i,
                    "jugador" + i,
                    "jugador" + i + "@test.com",
                    "hash-prueba",
                    region,
                    rango
            );

            candidatos.add(
                    new Candidato(
                            usuario,
                            rango,
                            latencia,
                            compatibilidad,
                            abandonos
                    )
            );
        }

        MatchmakingService servicio = new MatchmakingService(
                new HybridStrategy(3, 7, 80, 70)
        );

        // ejecuta algunas búsquedas antes de medir
        for (int i = 0; i < 10; i++) {
            servicio.buscarJugadores(candidatos, scrim);
        }

        double tiempoTotal = 0;
        double tiempoMaximo = 0;
        int seleccionados = 0;

        for (int i = 0; i < REPETICIONES; i++) {

            long inicio = System.nanoTime();

            List<Candidato> resultado =
                    servicio.buscarJugadores(candidatos, scrim);

            long fin = System.nanoTime();

            double tiempoMs = (fin - inicio) / 1_000_000.0;

            tiempoTotal += tiempoMs;
            tiempoMaximo = Math.max(tiempoMaximo, tiempoMs);
            seleccionados = resultado.size();

            if (tiempoMs >= LIMITE_MS) {
                throw new AssertionError(
                        "El matchmaking supero los 2 segundos"
                );
            }

            for (Candidato candidato : resultado) {

                if (!candidato.usuario().getRegion().equals("LAS")
                        || candidato.rango() < 3
                        || candidato.rango() > 7
                        || candidato.latencia() > 80
                        || candidato.compatibilidad() < 70) {

                    throw new AssertionError(
                            "Se selecciono un jugador incompatible"
                    );
                }
            }
        }

        if (seleccionados > 10) {
            throw new AssertionError(
                    "Se supero la cantidad de cupos"
            );
        }

        double promedio = tiempoTotal / REPETICIONES;

        System.out.println("\n=== PRUEBA DE RENDIMIENTO ===");
        System.out.println("Candidatos evaluados: " + candidatos.size());
        System.out.println("Repeticiones: " + REPETICIONES);
        System.out.println("Jugadores seleccionados: " + seleccionados);

        System.out.printf(
                "Tiempo promedio: %.3f ms%n",
                promedio
        );

        System.out.printf(
                "Tiempo maximo: %.3f ms%n",
                tiempoMaximo
        );

        System.out.println("Limite permitido: 2000 ms");

        System.out.println(
                "\nPrueba de rendimiento finalizada correctamente"
        );
    }
}
