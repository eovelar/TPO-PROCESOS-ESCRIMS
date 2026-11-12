
package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ScrimRepository {

    public record ScrimGuardado(
            int id,
            String juego,
            String formato,
            String region,
            int rangoMin,
            int rangoMax,
            int latenciaMax,
            String fecha,
            int duracion,
            int cuposTotales,
            int cuposDisponibles,
            String estado
    ) {}

    // obtiene las partidas y calcula los cupos disponibles
    public List<ScrimGuardado> listar() throws SQLException {

        String sql = """
                SELECT s.id, s.juego, s.formato, s.region,
                       s.rango_min, s.rango_max, s.latencia_max,
                       s.fecha_hora, s.duracion, s.cupos, s.estado,
                       s.cupos - (
                           SELECT COUNT(*)
                           FROM public.postulaciones p
                           WHERE p.scrim_id = s.id
                             AND p.estado = 'Aceptada'
                       ) AS disponibles
                FROM public.scrims s
                ORDER BY s.id
                """;

        List<ScrimGuardado> partidas = new ArrayList<>();

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {

            while (resultado.next()) {
                partidas.add(convertir(resultado));
            }
        }

        return partidas;
    }

    // convierte cada fila de la base en un registro de java
    private ScrimGuardado convertir(ResultSet rs)
            throws SQLException {

        Timestamp fecha = rs.getTimestamp("fecha_hora");

        return new ScrimGuardado(
                rs.getInt("id"),
                rs.getString("juego"),
                rs.getString("formato"),
                rs.getString("region"),
                rs.getInt("rango_min"),
                rs.getInt("rango_max"),
                rs.getInt("latencia_max"),
                fecha == null
                        ? ""
                        : fecha.toLocalDateTime().toString(),
                rs.getInt("duracion"),
                rs.getInt("cupos"),
                Math.max(0, rs.getInt("disponibles")),
                rs.getString("estado")
        );
    }
}
