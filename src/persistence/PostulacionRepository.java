
package persistence;

import domain.Scrim;
import domain.Usuario;
import matchmaking.Candidato;
import matchmaking.HybridStrategy;
import matchmaking.MatchmakingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostulacionRepository {

    public record PostulacionGuardada(
            int id,
            int usuarioId,
            int scrimId,
            String usuario,
            String juego,
            String formato,
            String region,
            String fecha,
            String rolDeseado,
            String estado
    ) {}

    private static final String LISTADO = """
            SELECT p.id, p.usuario_id, p.scrim_id, u.username,
                   s.juego, s.formato, s.region, s.fecha_hora,
                   p.rol_deseado, p.estado
            FROM public.postulaciones p
            JOIN public.usuarios u ON u.id = p.usuario_id
            JOIN public.scrims s ON s.id = p.scrim_id
            """;

    public int guardar(
            int usuarioId,
            int scrimId,
            String rolDeseado
    ) throws SQLException {

        try (Connection conexion = ConexionBD.conectar()) {

            conexion.setAutoCommit(false);

            try {
                String partidaSql = """
                        SELECT id, region, rango_min, rango_max,
                               cupos, estado
                        FROM public.scrims
                        WHERE id = ?
                        FOR UPDATE
                        """;

                String region;
                int rangoMin;
                int rangoMax;
                int cupos;
                String estado;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(partidaSql)) {

                    consulta.setInt(1, scrimId);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Partida no encontrada"
                            );
                        }

                        region = rs.getString("region");
                        rangoMin = rs.getInt("rango_min");
                        rangoMax = rs.getInt("rango_max");
                        cupos = rs.getInt("cupos");
                        estado = rs.getString("estado");
                    }
                }

                if (!"Buscando jugadores".equals(estado)) {
                    throw new IllegalStateException(
                            "La partida no admite postulaciones"
                    );
                }

                String usuarioSql = """
                        SELECT region, rango
                        FROM public.usuarios
                        WHERE id = ?
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(usuarioSql)) {

                    consulta.setInt(1, usuarioId);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Usuario no encontrado"
                            );
                        }

                        if (!region.equalsIgnoreCase(
                                rs.getString("region"))) {

                            throw new IllegalArgumentException(
                                    "La region no coincide con la partida"
                            );
                        }

                        int rango = rs.getInt("rango");

                        if (rango < rangoMin || rango > rangoMax) {
                            throw new IllegalArgumentException(
                                    "Tu rango no corresponde a esta partida"
                            );
                        }
                    }
                }

                String duplicadaSql = """
                        SELECT 1
                        FROM public.postulaciones
                        WHERE usuario_id = ?
                          AND scrim_id = ?
                        LIMIT 1
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(duplicadaSql)) {

                    consulta.setInt(1, usuarioId);
                    consulta.setInt(2, scrimId);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (rs.next()) {
                            throw new IllegalStateException(
                                    "Ya te postulaste a esta partida"
                            );
                        }
                    }
                }

                if (cantidadAceptadas(conexion, scrimId) >= cupos) {
                    throw new IllegalStateException(
                            "No quedan cupos disponibles"
                    );
                }

                String insertar = """
                        INSERT INTO public.postulaciones
                            (usuario_id, scrim_id, rol_deseado, estado)
                        VALUES (?, ?, ?, 'Pendiente')
                        RETURNING id
                        """;

                int id;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(insertar)) {

                    consulta.setInt(1, usuarioId);
                    consulta.setInt(2, scrimId);
                    consulta.setString(3, rolDeseado);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se pudo guardar la postulacion"
                            );
                        }

                        id = rs.getInt("id");
                    }
                }

                conexion.commit();
                return id;

            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    public List<PostulacionGuardada> listarPorUsuario(
            int usuarioId
    ) throws SQLException {

        List<PostulacionGuardada> lista = new ArrayList<>();

        String sql = LISTADO
                + " WHERE p.usuario_id = ? ORDER BY p.id";

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, usuarioId);

            try (ResultSet rs = consulta.executeQuery()) {

                while (rs.next()) {
                    lista.add(convertir(rs));
                }
            }
        }

        return lista;
    }

    public List<PostulacionGuardada> listarTodas()
            throws SQLException {

        List<PostulacionGuardada> lista = new ArrayList<>();

        String sql = LISTADO + " ORDER BY p.id";

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql);
             ResultSet rs = consulta.executeQuery()) {

            while (rs.next()) {
                lista.add(convertir(rs));
            }
        }

        return lista;
    }

    // busca postulantes pendientes y los ordena con strategy
    public List<Candidato> buscarCandidatosCompatibles(
            int scrimId,
            int compatibilidadMinima
    ) throws SQLException {

        if (compatibilidadMinima < 0 ||
                compatibilidadMinima > 100) {
            throw new IllegalArgumentException(
                    "La compatibilidad debe estar entre 0 y 100"
            );
        }

        try (Connection conexion = ConexionBD.conectar()) {

            String scrimSql = """
                    SELECT id, juego, formato, region,
                           rango_min, rango_max, latencia_max,
                           fecha_hora, duracion, cupos, estado
                    FROM public.scrims
                    WHERE id = ?
                    """;

            Scrim scrim;
            int rangoMin;
            int rangoMax;
            int latenciaMax;

            try (PreparedStatement consulta =
                         conexion.prepareStatement(scrimSql)) {

                consulta.setInt(1, scrimId);

                try (ResultSet rs = consulta.executeQuery()) {

                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Partida no encontrada"
                        );
                    }

                    if (!"Buscando jugadores".equals(
                            rs.getString("estado"))) {
                        throw new IllegalStateException(
                                "La partida ya no admite jugadores"
                        );
                    }

                    rangoMin = rs.getInt("rango_min");
                    rangoMax = rs.getInt("rango_max");
                    latenciaMax = rs.getInt("latencia_max");

                    Timestamp fecha = rs.getTimestamp("fecha_hora");

                    if (fecha == null) {
                        throw new IllegalStateException(
                                "La partida no tiene fecha"
                        );
                    }

                    scrim = new Scrim(
                            rs.getInt("id"),
                            rs.getString("juego"),
                            rs.getString("formato"),
                            rs.getString("region"),
                            rangoMin,
                            rangoMax,
                            latenciaMax,
                            fecha.toLocalDateTime(),
                            rs.getInt("duracion"),
                            rs.getInt("cupos")
                    );
                }
            }

            String candidatosSql = """
                    SELECT u.id, u.username, u.email,
                           u.password_hash, u.region, u.rango,
                           u.latencia_ms, u.compatibilidad,
                           u.abandonos
                    FROM public.postulaciones p
                    JOIN public.usuarios u
                      ON u.id = p.usuario_id
                    WHERE p.scrim_id = ?
                      AND p.estado = 'Pendiente'
                      AND u.latencia_ms IS NOT NULL
                      AND u.compatibilidad IS NOT NULL
                      AND u.abandonos IS NOT NULL
                    ORDER BY p.id
                    """;

            List<Candidato> candidatos = new ArrayList<>();

            try (PreparedStatement consulta =
                         conexion.prepareStatement(candidatosSql)) {

                consulta.setInt(1, scrimId);

                try (ResultSet rs = consulta.executeQuery()) {

                    while (rs.next()) {

                        int latencia = rs.getInt("latencia_ms");
                        int compatibilidad =
                                rs.getInt("compatibilidad");
                        int abandonos = rs.getInt("abandonos");

                        if (latencia < 0 ||
                                compatibilidad < 0 ||
                                compatibilidad > 100 ||
                                abandonos < 0) {
                            continue;
                        }

                        Usuario jugador = new Usuario(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("email"),
                                rs.getString("password_hash"),
                                rs.getString("region"),
                                rs.getInt("rango")
                        );

                        candidatos.add(
                                new Candidato(
                                        jugador,
                                        rs.getInt("rango"),
                                        latencia,
                                        compatibilidad,
                                        abandonos
                                )
                        );
                    }
                }
            }

            MatchmakingService servicio = new MatchmakingService(
                    new HybridStrategy(
                            rangoMin,
                            rangoMax,
                            latenciaMax,
                            compatibilidadMinima
                    )
            );

            return servicio.buscarJugadores(
                    candidatos,
                    scrim
            );
        }
    }

    public String procesar(
            int postulacionId,
            String accion
    ) throws SQLException {

        if (!"aceptar".equals(accion) &&
                !"rechazar".equals(accion)) {

            throw new IllegalArgumentException(
                    "Accion invalida"
            );
        }

        try (Connection conexion = ConexionBD.conectar()) {

            conexion.setAutoCommit(false);

            try {
                int scrimId;

                String buscarId = """
                        SELECT scrim_id
                        FROM public.postulaciones
                        WHERE id = ?
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(buscarId)) {

                    consulta.setInt(1, postulacionId);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Postulacion no encontrada"
                            );
                        }

                        scrimId = rs.getInt("scrim_id");
                    }
                }

                int cupos;
                String estadoPartida;

                String partidaSql = """
                        SELECT cupos, estado
                        FROM public.scrims
                        WHERE id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(partidaSql)) {

                    consulta.setInt(1, scrimId);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Partida no encontrada"
                            );
                        }

                        cupos = rs.getInt("cupos");
                        estadoPartida = rs.getString("estado");
                    }
                }

                String solicitudSql = """
                        SELECT estado
                        FROM public.postulaciones
                        WHERE id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(solicitudSql)) {

                    consulta.setInt(1, postulacionId);

                    try (ResultSet rs = consulta.executeQuery()) {

                        if (!rs.next() ||
                                !"Pendiente".equals(
                                        rs.getString("estado"))) {

                            throw new IllegalStateException(
                                    "La solicitud ya fue procesada"
                            );
                        }
                    }
                }

                String estadoNuevo = "aceptar".equals(accion)
                        ? "Aceptada"
                        : "Rechazada";

                if ("aceptar".equals(accion)) {

                    if (!"Buscando jugadores".equals(estadoPartida)) {
                        throw new IllegalStateException(
                                "La partida ya no admite jugadores"
                        );
                    }

                    if (cantidadAceptadas(conexion, scrimId) >= cupos) {
                        throw new IllegalStateException(
                                "No quedan cupos disponibles"
                        );
                    }
                }

                String actualizarSql = """
                        UPDATE public.postulaciones
                        SET estado = ?
                        WHERE id = ?
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(actualizarSql)) {

                    consulta.setString(1, estadoNuevo);
                    consulta.setInt(2, postulacionId);
                    consulta.executeUpdate();
                }

                if ("aceptar".equals(accion) &&
                        cantidadAceptadas(conexion, scrimId) >= cupos) {

                    String actualizarPartida = """
                            UPDATE public.scrims
                            SET estado = 'Lobby armado'
                            WHERE id = ?
                            """;

                    try (PreparedStatement consulta =
                                 conexion.prepareStatement(
                                         actualizarPartida)) {

                        consulta.setInt(1, scrimId);
                        consulta.executeUpdate();
                    }
                }

                conexion.commit();
                return estadoNuevo;

            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    private int cantidadAceptadas(
            Connection conexion,
            int scrimId
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM public.postulaciones
                WHERE scrim_id = ?
                  AND estado = 'Aceptada'
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, scrimId);

            try (ResultSet rs = consulta.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private PostulacionGuardada convertir(ResultSet rs)
            throws SQLException {

        Timestamp fecha = rs.getTimestamp("fecha_hora");

        return new PostulacionGuardada(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getInt("scrim_id"),
                rs.getString("username"),
                rs.getString("juego"),
                rs.getString("formato"),
                rs.getString("region"),
                fecha == null
                        ? ""
                        : fecha.toLocalDateTime().toString(),
                rs.getString("rol_deseado"),
                rs.getString("estado")
        );
    }
}
