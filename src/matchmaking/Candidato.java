
package matchmaking;

import domain.Usuario;

public record Candidato(
        Usuario usuario,
        int rango,
        int latencia,
        int compatibilidad,
        int abandonos
) {

    public Candidato {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario es obligatorio"
            );
        }

        if (rango < 1 || rango > 10) {
            throw new IllegalArgumentException(
                    "El rango debe estar entre 1 y 10"
            );
        }

        if (latencia < 0 || compatibilidad < 0 ||
                compatibilidad > 100 || abandonos < 0) {
            throw new IllegalArgumentException(
                    "Datos del candidato invalidos"
            );
        }
    }
}
