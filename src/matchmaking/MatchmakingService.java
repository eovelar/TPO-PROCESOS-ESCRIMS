
package matchmaking;

import domain.Scrim;

import java.util.List;

public class MatchmakingService {

    private MatchmakingStrategy estrategia;

    public MatchmakingService(MatchmakingStrategy estrategia) {
        cambiarEstrategia(estrategia);
    }

    // permite cambiar el criterio sin modificar el servicio
    public void cambiarEstrategia(MatchmakingStrategy estrategia) {

        if (estrategia == null) {
            throw new IllegalArgumentException(
                    "La estrategia es obligatoria"
            );
        }

        this.estrategia = estrategia;
    }

    public List<Candidato> buscarJugadores(
            List<Candidato> candidatos,
            Scrim scrim
    ) {

        if (candidatos == null || scrim == null) {
            throw new IllegalArgumentException(
                    "Faltan datos para realizar el matchmaking"
            );
        }

        // devuelve tantos candidatos como cupos disponibles
        int cupos = Math.max(
                0,
                scrim.getCuposDisponibles()
        );

        return estrategia.seleccionar(candidatos, scrim)
                .stream()
                .limit(cupos)
                .toList();
    }
}
