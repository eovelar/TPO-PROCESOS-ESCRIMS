
package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
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

    // obtiene las partidas y calcula sus cupos disponibles
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

    // registra una partida y devuelve su id
    public int crear(
            Integer creadorId,
            String juego,
            String formato,
            String region,
            int rangoMin,
            int rangoMax,
            int latenciaMax,
            LocalDateTime fechaHora,
            int duracion,
            int cupos
    ) throws SQLException {

        if (juego == null || juego.isBlank()
                || formato == null || formato.isBlank()
                || region == null || region.isBlank()) {

            throw new IllegalArgumentException(
                    "Completa los datos de la partida"
            );
        }

        if (rangoMin < 1 || rangoMax > 10
                || rangoMin > rangoMax) {

            throw new IllegalArgumentException(
                    "Los rangos no son validos"
            );
        }

        if (latenciaMax <= 0 || duracion <= 0 || cupos <= 0) {

            throw new IllegalArgumentException(
                    "Los valores deben ser positivos"
            );
        }

        if (fechaHora == null
                || !fechaHora.isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "La fecha de la partida debe ser futura"
            );
        }

        String sql = """
                INSERT INTO public.scrims (
                    creador_id,
                    juego,
                    formato,
                    region,
                    rango_min,
                    rango_max,
                    latencia_max,
                    fecha_hora,
                    duracion,
                    cupos,
                    estado
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            // la cuenta de prueba no tiene un registro en supabase
            if (creadorId == null) {
                consulta.setNull(1, Types.INTEGER);
            } else {
                consulta.setInt(1, creadorId);
            }

            consulta.setString(2, juego.trim());
            consulta.setString(3, formato.trim());
            consulta.setString(4, region.trim());

            consulta.setInt(5, rangoMin);
            consulta.setInt(6, rangoMax);
            consulta.setInt(7, latenciaMax);

            consulta.setTimestamp(
                    8,
                    Timestamp.valueOf(fechaHora)
            );

            consulta.setInt(9, duracion);
            consulta.setInt(10, cupos);
            consulta.setString(11, "Buscando jugadores");

            // postgres asigna el id de la nueva partida
            try (ResultSet resultado = consulta.executeQuery()) {

                if (resultado.next()) {
                    return resultado.getInt("id");
                }
            }

            throw new SQLException(
                    "No se pudo crear la partida"
            );
        }
    }

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
