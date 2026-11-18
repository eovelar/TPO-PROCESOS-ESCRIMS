
package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Registra cada cambio de estado en la tabla de auditoría.
// Se invoca dentro de la misma transacción que modifica scrims.estado,
// para que el log sea atómico con la transición.
public class AuditoriaRepository {

    private static final String INSERTAR = """
            INSERT INTO public.auditoria_estados
                (scrim_id, estado_anterior, estado_nuevo, origen)
            VALUES (?, ?, ?, ?)
            """;

    // registra el cambio usando la conexión de la transacción activa
    public static void registrar(
            Connection conexion,
            int scrimId,
            String estadoAnterior,
            String estadoNuevo,
            String origen
    ) throws SQLException {

        try (PreparedStatement consulta =
                     conexion.prepareStatement(INSERTAR)) {

            consulta.setInt(1, scrimId);
            consulta.setString(2, estadoAnterior);
            consulta.setString(3, estadoNuevo);
            consulta.setString(4, origen);
            consulta.executeUpdate();
        }
    }
}
