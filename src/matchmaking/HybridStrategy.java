
package matchmaking;

import domain.Scrim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HybridStrategy implements MatchmakingStrategy {

    private static final double PESO_MMR = 0.50;
    private static final double PESO_LATENCIA = 0.30;
    private static final double PESO_HISTORIAL = 0.20;

    private final MatchmakingStrategy estrategiaMMR;
    private final MatchmakingStrategy estrategiaLatencia;
    private final MatchmakingStrategy estrategiaHistorial;

    public HybridStrategy(
            int rangoMinimo,
            int rangoMaximo,
            int latenciaMaxima,
            int compatibilidadMinima
    ) {
        this.estrategiaMMR = new ByMMRStrategy(
                rangoMinimo,
                rangoMaximo
        );

        this.estrategiaLatencia = new ByLatencyStrategy(
                latenciaMaxima
        );

        this.estrategiaHistorial = new ByHistoryStrategy(
                compatibilidadMinima
        );
    }

    @Override
    public List<Candidato> seleccionar(
            List<Candidato> candidatos,
            Scrim scrim
    ) {

        List<Candidato> porMMR =
                estrategiaMMR.seleccionar(candidatos, scrim);

        List<Candidato> porLatencia =
                estrategiaLatencia.seleccionar(candidatos, scrim);

        List<Candidato> porHistorial =
                estrategiaHistorial.seleccionar(candidatos, scrim);

        // conserva los candidatos que cumplen los tres filtros
        Set<Candidato> validosLatencia =
                new HashSet<>(porLatencia);

        Set<Candidato> validosHistorial =
                new HashSet<>(porHistorial);

        List<Candidato> compatibles = porMMR.stream()
                .filter(validosLatencia::contains)
                .filter(validosHistorial::contains)
                .toList();

        Map<Candidato, Integer> posicionesMMR =
                calcularPosiciones(porMMR);

        Map<Candidato, Integer> posicionesLatencia =
                calcularPosiciones(porLatencia);

        Map<Candidato, Integer> posicionesHistorial =
                calcularPosiciones(porHistorial);

        List<Candidato> resultado =
                new ArrayList<>(compatibles);

        // combina mmr, latencia e historial
        resultado.sort(
                Comparator.comparingDouble(
                        (Candidato candidato) -> calcularPuntaje(
                                candidato,
                                posicionesMMR,
                                posicionesLatencia,
                                posicionesHistorial
                        )
                ).thenComparing(
                        c -> c.usuario().getId()
                )
        );

        return resultado;
    }

    private Map<Candidato, Integer> calcularPosiciones(
            List<Candidato> candidatos
    ) {

        Map<Candidato, Integer> posiciones =
                new HashMap<>();

        for (int i = 0; i < candidatos.size(); i++) {
            posiciones.put(candidatos.get(i), i);
        }

        return posiciones;
    }

    private double calcularPuntaje(
            Candidato candidato,
            Map<Candidato, Integer> posicionesMMR,
            Map<Candidato, Integer> posicionesLatencia,
            Map<Candidato, Integer> posicionesHistorial
    ) {

        int mmr = posicionesMMR.get(candidato);
        int latencia = posicionesLatencia.get(candidato);
        int historial = posicionesHistorial.get(candidato);

        return mmr * PESO_MMR
                + latencia * PESO_LATENCIA
                + historial * PESO_HISTORIAL;
    }
}
