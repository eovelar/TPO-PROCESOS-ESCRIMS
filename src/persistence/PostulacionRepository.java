
package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

    // guarda una nueva postulación y valida los requisitos
    public int guardar(
            int usuarioId,
            int scrimId,
            String rolDeseado
    ) throws SQLException {

        try (Connection conexion = ConexionBD.conectar()) {

            conexion.setAutoCommit(false);

            try {
                // bloquea la partida mientras se registra la solicitud
                String partidaSql = """
                        SELECT id, region, rango_min, rango_max,
                               cupos, estado
                        FROM public.scrims
                        WHERE id = ? FOR UPDATE
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

                // comprueba que el jugador cumpla con región y rango
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

                // evita que el mismo usuario se postule dos veces
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

                // supabase genera el id de la nueva postulación
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

                // revierte los cambios si falla alguna validación
                conexion.rollback();
                throw e;
            }
        }
    }

    // recupera las solicitudes de un jugador
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

    // recupera todas las solicitudes para el organizador
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

    // acepta o rechaza una solicitud pendiente
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

                // bloquea la partida para evitar sobrepasar los cupos
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

                // cuando se completan los cupos, se arma el lobby
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

                // mantiene los datos consistentes ante un error
                conexion.rollback();
                throw e;
            }
        }
    }

    // cuenta los jugadores que ya fueron aceptados
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
