
package matchmaking;

import domain.Scrim;

import java.util.List;

public interface MatchmakingStrategy {

    List<Candidato> seleccionar(
            List<Candidato> candidatos,
            Scrim scrim
    );
}
