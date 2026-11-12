
package pruebas;

import domain.Scrim;
import domain.Usuario;
import matchmaking.Candidato;
import matchmaking.HybridStrategy;
import matchmaking.MatchmakingService;

import java.time.LocalDateTime;
import java.util.List;

public class PruebaHybridStrategy {

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

        Usuario jugador5 = new Usuario(
                5, "jugador5", "jugador5@test.com",
                "hash-prueba", "LAS", 5
        );

        Usuario jugador6 = new Usuario(
                6, "jugador6", "jugador6@test.com",
                "hash-prueba", "LAS", 4
        );

        List<Candidato> candidatos = List.of(
                new Candidato(eva, 5, 45, 90, 0),
                new Candidato(jugador2, 7, 25, 75, 1),
                new Candidato(jugador3, 3, 70, 95, 0),
                new Candidato(jugador4, 5, 20, 100, 0),
                new Candidato(jugador5, 5, 95, 90, 0),
                new Candidato(jugador6, 4, 50, 50, 2)
        );

        MatchmakingService servicio = new MatchmakingService(
                new HybridStrategy(3, 7, 80, 70)
        );

        List<Candidato> resultado =
                servicio.buscarJugadores(candidatos, scrim);

        System.out.println("\n=== MATCHMAKING HIBRIDO ===");

        for (Candidato candidato : resultado) {
            System.out.println(
                    candidato.usuario().getUsername()
                            + " | rango: " + candidato.rango()
                            + " | ping: " + candidato.latencia()
                            + " | compatibilidad: "
                            + candidato.compatibilidad()
                            + " | abandonos: "
                            + candidato.abandonos()
            );
        }

        // comprueba que los filtros se apliquen correctamente
        comprobar(
                resultado.size() == 3,
                "Deben quedar tres candidatos compatibles"
        );

        comprobar(
                resultado.get(0).usuario().getId() == 1,
                "Eva debe quedar primera"
        );

        comprobar(
                resultado.stream().noneMatch(
                        c -> c.usuario().getId() == 4
                ),
                "El jugador de otra region debe quedar afuera"
        );

        comprobar(
                resultado.stream().noneMatch(
                        c -> c.usuario().getId() == 5
                ),
                "El jugador con ping alto debe quedar afuera"
        );

        comprobar(
                resultado.stream().noneMatch(
                        c -> c.usuario().getId() == 6
                ),
                "El jugador con baja compatibilidad debe quedar afuera"
        );

        System.out.println(
                "\nPrueba de matchmaking hibrido finalizada correctamente"
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
