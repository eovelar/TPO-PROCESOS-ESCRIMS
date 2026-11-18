
package persistence;

import state.ScrimContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LobbyRepository {

    public record Transicion(
            int scrimId,
            String juego,
            String estadoAnterior,
            String estadoNuevo
    ) {}

    public record JugadorLobby(
            int usuarioId,
            String username,
            boolean confirmado
    ) {}

    public record EstadoLobby(
            int scrimId,
            String juego,
            String estado,
            int cupos,
            int confirmados,
            List<JugadorLobby> jugadores
    ) {}

    // resultado de confirmar: progreso y, si hubo, el cambio de estado
    public record ResultadoConfirmacion(
            int confirmados,
            int cupos,
            Transicion transicion
    ) {}

    private record Partida(String juego, int cupos, String estado) {}

    // registra la confirmacion de un jugador aceptado
    public ResultadoConfirmacion confirmar(int scrimId, int usuarioId)
            throws SQLException {

        try (Connection conexion = ConexionBD.conectar()) {

            conexion.setAutoCommit(false);

            try {
                // la fila queda bloqueada hasta el commit, asi dos
                // confirmaciones simultaneas se procesan en orden
                Partida partida = bloquearPartida(conexion, scrimId);

                ScrimContext contexto =
                        ScrimContext.desdeNombre(partida.estado());

                if (!contexto.getEstadoActual().equals("Lobby armado")) {
                    throw new IllegalStateException(
                            "La partida no esta esperando confirmaciones"
                    );
                }

                if (!estaAceptado(conexion, scrimId, usuarioId)) {
                    throw new IllegalArgumentException(
                            "No participas en esta partida"
                    );
                }

                String insertar = """
                        INSERT INTO public.confirmaciones
                            (scrim_id, usuario_id)
                        VALUES (?, ?)
                        ON CONFLICT DO NOTHING
                        """;

                try (PreparedStatement consulta =
                             conexion.prepareStatement(insertar)) {

                    consulta.setInt(1, scrimId);
                    consulta.setInt(2, usuarioId);

                    if (consulta.executeUpdate() == 0) {
                        throw new IllegalStateException(
                                "Ya confirmaste tu participacion"
                        );
                    }
                }

                int confirmados = contarConfirmaciones(conexion, scrimId);
                Transicion transicion = null;

                // con todos confirmados, el State decide el siguiente estado
                if (confirmados >= partida.cupos()) {

                    contexto.confirmarJugadores();

                    transicion = guardarEstado(
                            conexion,
                            scrimId,
                            partida.juego(),
                            partida.estado(),
                            contexto.getEstadoActual(),
                            "confirmacion-jugador"
                    );
                }

                conexion.commit();

                return new ResultadoConfirmacion(
                        confirmados,
                        partida.cupos(),
                        transicion
                );

            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    // aplica iniciar, finalizar o cancelar usando el patron State
    public Transicion transicionar(int scrimId, String accion)
            throws SQLException {

        if (!"iniciar".equals(accion)
                && !"finalizar".equals(accion)
                && !"cancelar".equals(accion)) {

            throw new IllegalArgumentException("Accion invalida");
        }

        try (Connection conexion = ConexionBD.conectar()) {

            conexion.setAutoCommit(false);

            try {
                Transicion transicion =
                        transicionar(conexion, scrimId, accion);

                conexion.commit();
                return transicion;

            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    // inicia las partidas confirmadas cuyo horario ya llego
    public List<Transicion> iniciarProgramadas() throws SQLException {

        List<Integer> ids = new ArrayList<>();

        String sql = """
                SELECT id
                FROM public.scrims
                WHERE estado = 'Confirmado'
                  AND fecha_hora <= ?
                ORDER BY id
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setTimestamp(
                    1,
                    Timestamp.valueOf(LocalDateTime.now())
            );

            try (ResultSet rs = consulta.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }

        List<Transicion> iniciadas = new ArrayList<>();

        for (int id : ids) {
            try {
                iniciadas.add(transicionar(id, "iniciar"));

            } catch (IllegalStateException e) {
                // otro proceso ya cambio el estado de esta partida
            }
        }

        return iniciadas;
    }

    // finaliza las partidas cuyo horario + duracion ya paso
    public List<Transicion> finalizarVencidas() throws SQLException {

        List<Integer> ids = new ArrayList<>();

        String sql = """
                SELECT id
                FROM public.scrims
                WHERE estado = 'En juego'
                  AND fecha_hora + (duracion || ' minutes')::interval <= ?
                ORDER BY id
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setTimestamp(
                    1,
                    Timestamp.valueOf(LocalDateTime.now())
            );

            try (ResultSet rs = consulta.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }

        List<Transicion> finalizadas = new ArrayList<>();

        for (int id : ids) {
            try {
                finalizadas.add(transicionar(id, "finalizar"));

            } catch (IllegalStateException e) {
                // otro proceso ya cambio el estado
            }
        }

        return finalizadas;
    }

    // devuelve el estado de la partida y el progreso de confirmaciones
    public EstadoLobby consultar(int scrimId) throws SQLException {

        try (Connection conexion = ConexionBD.conectar()) {

            String partidaSql = """
                    SELECT juego, cupos, estado
                    FROM public.scrims
                    WHERE id = ?
                    """;

            String juego;
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

                    juego = rs.getString("juego");
                    cupos = rs.getInt("cupos");
                    estado = rs.getString("estado");
                }
            }

            String jugadoresSql = """
                    SELECT u.id, u.username,
                           (c.usuario_id IS NOT NULL) AS confirmado
                    FROM public.postulaciones p
                    JOIN public.usuarios u ON u.id = p.usuario_id
                    LEFT JOIN public.confirmaciones c
                           ON c.scrim_id = p.scrim_id
                          AND c.usuario_id = p.usuario_id
                    WHERE p.scrim_id = ?
                      AND p.estado = 'Aceptada'
                    ORDER BY p.id
                    """;

            List<JugadorLobby> jugadores = new ArrayList<>();
            int confirmados = 0;

            try (PreparedStatement consulta =
                         conexion.prepareStatement(jugadoresSql)) {

                consulta.setInt(1, scrimId);

                try (ResultSet rs = consulta.executeQuery()) {

                    while (rs.next()) {

                        boolean confirmado = rs.getBoolean("confirmado");

                        if (confirmado) {
                            confirmados++;
                        }

                        jugadores.add(new JugadorLobby(
                                rs.getInt("id"),
                                rs.getString("username"),
                                confirmado
                        ));
                    }
                }
            }

            return new EstadoLobby(
                    scrimId,
                    juego,
                    estado,
                    cupos,
                    confirmados,
                    jugadores
            );
        }
    }

    private Transicion transicionar(
            Connection conexion,
            int scrimId,
            String accion
    ) throws SQLException {

        Partida partida = bloquearPartida(conexion, scrimId);

        ScrimContext contexto =
                ScrimContext.desdeNombre(partida.estado());

        switch (accion) {
            case "iniciar" -> contexto.iniciar();
            case "finalizar" -> contexto.finalizar();
            default -> contexto.cancelar();
        }

        return guardarEstado(
                conexion,
                scrimId,
                partida.juego(),
                partida.estado(),
                contexto.getEstadoActual(),
                "organizador-" + accion
        );
    }

    private Partida bloquearPartida(Connection conexion, int scrimId)
            throws SQLException {

        String sql = """
                SELECT juego, cupos, estado
                FROM public.scrims
                WHERE id = ?
                FOR UPDATE
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, scrimId);

            try (ResultSet rs = consulta.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "Partida no encontrada"
                    );
                }

                return new Partida(
                        rs.getString("juego"),
                        rs.getInt("cupos"),
                        rs.getString("estado")
                );
            }
        }
    }

    private boolean estaAceptado(
            Connection conexion,
            int scrimId,
            int usuarioId
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM public.postulaciones
                WHERE scrim_id = ?
                  AND usuario_id = ?
                  AND estado = 'Aceptada'
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, scrimId);
            consulta.setInt(2, usuarioId);

            try (ResultSet rs = consulta.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int contarConfirmaciones(Connection conexion, int scrimId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM public.confirmaciones
                WHERE scrim_id = ?
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

    private Transicion guardarEstado(
            Connection conexion,
            int scrimId,
            String juego,
            String anterior,
            String nuevo,
            String origen
    ) throws SQLException {

        String sql = """
                UPDATE public.scrims
                SET estado = ?
                WHERE id = ?
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, nuevo);
            consulta.setInt(2, scrimId);
            consulta.executeUpdate();
        }

        // registro de auditoría en la misma transacción
        AuditoriaRepository.registrar(
                conexion,
                scrimId,
                anterior,
                nuevo,
                origen
        );

        return new Transicion(scrimId, juego, anterior, nuevo);
    }
}
