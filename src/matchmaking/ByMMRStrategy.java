
package matchmaking;

import domain.Scrim;

import java.util.Comparator;
import java.util.List;

public class ByMMRStrategy implements MatchmakingStrategy {

    private final int rangoMinimo;
    private final int rangoMaximo;
    private final int rangoObjetivo;

    public ByMMRStrategy(
            int rangoMinimo,
            int rangoMaximo
    ) {

        if (rangoMinimo < 1 ||
                rangoMaximo > 10 ||
                rangoMinimo > rangoMaximo) {
            throw new IllegalArgumentException(
                    "Rangos invalidos"
            );
        }

        this.rangoMinimo = rangoMinimo;
        this.rangoMaximo = rangoMaximo;
        this.rangoObjetivo =
                (rangoMinimo + rangoMaximo) / 2;
    }

    @Override
    public List<Candidato> seleccionar(
            List<Candidato> candidatos,
            Scrim scrim
    ) {

        // prioriza a los jugadores más cercanos al rango buscado
        return candidatos.stream()
                .filter(c -> c.usuario().getRegion()
                        .equalsIgnoreCase(scrim.getRegion()))
                .filter(c -> c.rango() >= rangoMinimo)
                .filter(c -> c.rango() <= rangoMaximo)
                .sorted(
                        Comparator.comparingInt(
                                c -> Math.abs(
                                        c.rango() - rangoObjetivo
                                )
                        )
                )
                .toList();
    }
}
