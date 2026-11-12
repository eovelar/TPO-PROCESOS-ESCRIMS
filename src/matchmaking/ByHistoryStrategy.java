
package matchmaking;

import domain.Scrim;

import java.util.Comparator;
import java.util.List;

public class ByHistoryStrategy implements MatchmakingStrategy {

    private final int compatibilidadMinima;

    public ByHistoryStrategy(int compatibilidadMinima) {

        if (compatibilidadMinima < 0 ||
                compatibilidadMinima > 100) {
            throw new IllegalArgumentException(
                    "La compatibilidad debe estar entre 0 y 100"
            );
        }

        this.compatibilidadMinima = compatibilidadMinima;
    }

    @Override
    public List<Candidato> seleccionar(
            List<Candidato> candidatos,
            Scrim scrim
    ) {

        // prioriza el buen historial y la compatibilidad
        return candidatos.stream()
                .filter(c -> c.usuario().getRegion()
                        .equalsIgnoreCase(scrim.getRegion()))
                .filter(c -> c.compatibilidad()
                        >= compatibilidadMinima)
                .sorted(
                        Comparator.comparingInt(
                                Candidato::abandonos
                        ).thenComparing(
                                Comparator.comparingInt(
                                        Candidato::compatibilidad
                                ).reversed()
                        )
                )
                .toList();
    }
}
