
package matchmaking;

import domain.Scrim;

import java.util.Comparator;
import java.util.List;

public class ByLatencyStrategy implements MatchmakingStrategy {

    private final int latenciaMaxima;

    public ByLatencyStrategy(int latenciaMaxima) {

        if (latenciaMaxima < 0) {
            throw new IllegalArgumentException(
                    "La latencia maxima no puede ser negativa"
            );
        }

        this.latenciaMaxima = latenciaMaxima;
    }

    @Override
    public List<Candidato> seleccionar(
            List<Candidato> candidatos,
            Scrim scrim
    ) {

        // descarta conexiones lentas y ordena por menor ping
        return candidatos.stream()
                .filter(c -> c.usuario().getRegion()
                        .equalsIgnoreCase(scrim.getRegion()))
                .filter(c -> c.latencia() <= latenciaMaxima)
                .sorted(
                        Comparator.comparingInt(
                                Candidato::latencia
                        )
                )
                .toList();
    }
}
