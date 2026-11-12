
package pruebas;

import domain.Scrim;
import domain.Usuario;
import matchmaking.ByHistoryStrategy;
import matchmaking.ByLatencyStrategy;
import matchmaking.ByMMRStrategy;
import matchmaking.Candidato;
import matchmaking.MatchmakingService;

import java.time.LocalDateTime;
import java.util.List;

public class PruebaMatchmaking {

    public static void main(String[] args) {

        Scrim scrim = new Scrim(
                1, "Valorant", "5v5", "LAS",
                3, 7, 80,
                LocalDateTime.now().plusDays(2),
                60, 10
        );

        Usuario eva = new Usuario(
                1, "eva", "eva@test.com",
                "hash-prueba", "LAS", 5
        );

        Usuario jugador2 = new Usuario(
                2, "jugador2", "jugador2@test.com",
                "hash-prueba", "LAS", 7
        );

        Usuario jugador3 = new Usuario(
                3, "jugador3", "jugador3@test.com",
                "hash-prueba", "LAS", 3
        );

        Usuario jugador4 = new Usuario(
                4, "jugador4", "jugador4@test.com",
                "hash-prueba", "BR", 5
        );

        List<Candidato> candidatos = List.of(
                new Candidato(eva, 5, 45, 90, 0),
                new Candidato(jugador2, 7, 25, 65, 1),
                new Candidato(jugador3, 3, 70, 95, 0),
                new Candidato(jugador4, 5, 20, 100, 0)
        );

        MatchmakingService servicio =
                new MatchmakingService(
                        new ByMMRStrategy(3, 7)
                );

        // prueba el emparejamiento por rango
        mostrar(
                "MATCHMAKING POR RANGO",
                servicio.buscarJugadores(candidatos, scrim)
        );

        // cambia la estrategia para priorizar el ping
        servicio.cambiarEstrategia(
                new ByLatencyStrategy(80)
        );

        mostrar(
                "MATCHMAKING POR LATENCIA",
                servicio.buscarJugadores(candidatos, scrim)
        );

        // cambia la estrategia para evaluar el historial
        servicio.cambiarEstrategia(
                new ByHistoryStrategy(70)
        );

        mostrar(
                "MATCHMAKING POR HISTORIAL",
                servicio.buscarJugadores(candidatos, scrim)
        );

        System.out.println(
                "\nPrueba de Strategy finalizada"
        );
    }

    private static void mostrar(
            String titulo,
            List<Candidato> candidatos
    ) {

        System.out.println("\n=== " + titulo + " ===");

        for (Candidato candidato : candidatos) {

            System.out.println(
                    candidato.usuario().getUsername()
                            + " | rango: " + candidato.rango()
                            + " | ping: " + candidato.latencia()
                            + " | compatibilidad: "
                            + candidato.compatibilidad()
            );
        }
    }
}
