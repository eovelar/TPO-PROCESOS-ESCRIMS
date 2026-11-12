
package observer;

import java.time.LocalDateTime;

public record EventoScrim(
        int scrimId,
        String juego,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fecha
) {

    public EventoScrim(
            int scrimId,
            String juego,
            String estadoAnterior,
            String estadoNuevo
    ) {
        this(
                scrimId,
                juego,
                estadoAnterior,
                estadoNuevo,
                LocalDateTime.now()
        );
    }
}
