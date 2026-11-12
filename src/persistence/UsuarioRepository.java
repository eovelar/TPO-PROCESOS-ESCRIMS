
package persistence;

import domain.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UsuarioRepository {

    public record UsuarioGuardado(
            Usuario usuario,
            String rol,
            int rango
    ) {}

    public UsuarioGuardado guardar(
            String username,
            String email,
            String passwordHash,
            String region,
            int rango,
            String rol
    ) throws SQLException {

        if (username == null || username.isBlank()
                || email == null || email.isBlank()
                || passwordHash == null || passwordHash.isBlank()
                || region == null || region.isBlank()
                || rol == null || rol.isBlank()) {

            throw new IllegalArgumentException(
                    "Los datos del usuario son obligatorios"
            );
        }

        if (rango < 1 || rango > 10) {
            throw new IllegalArgumentException(
                    "El rango debe estar entre 1 y 10"
            );
        }

        String sql = """
                INSERT INTO usuarios (
                    username,
                    email,
                    password_hash,
                    region,
                    rango,
                    rol
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            consulta.setString(1, username.trim());
            consulta.setString(2, email.trim().toLowerCase());
            consulta.setString(3, passwordHash);
            consulta.setString(4, region);
            consulta.setInt(5, rango);
            consulta.setString(6, rol);

            int filas = consulta.executeUpdate();

            if (filas != 1) {
                throw new SQLException(
                        "No se pudo guardar el usuario"
                );
            }

            try (ResultSet claves = consulta.getGeneratedKeys()) {

                if (!claves.next()) {
                    throw new SQLException(
                            "No se obtuvo el ID del usuario"
                    );
                }

                int id = claves.getInt("id");

                Usuario usuario = new Usuario(
                        id,
                        username.trim(),
                        email.trim().toLowerCase(),
                        passwordHash,
                        region,
                        rango
                );

                return new UsuarioGuardado(
                        usuario,
                        rol,
                        rango
                );
            }
        }
    }

    public UsuarioGuardado buscarPorEmail(
            String email
    ) throws SQLException {

        if (email == null || email.isBlank()) {
            return null;
        }

        String sql = """
                SELECT
                    id,
                    username,
                    email,
                    password_hash,
                    region,
                    rango,
                    rol
                FROM usuarios
                WHERE LOWER(email) = LOWER(?)
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, email.trim());

            try (ResultSet resultado = consulta.executeQuery()) {

                if (resultado.next()) {
                    return convertirUsuario(resultado);
                }
            }
        }

        return null;
    }

    public UsuarioGuardado buscarPorId(
            int id
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    username,
                    email,
                    password_hash,
                    region,
                    rango,
                    rol
                FROM usuarios
                WHERE id = ?
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setInt(1, id);

            try (ResultSet resultado = consulta.executeQuery()) {

                if (resultado.next()) {
                    return convertirUsuario(resultado);
                }
            }
        }

        return null;
    }

    public boolean existeEmail(
            String email
    ) throws SQLException {

        String sql = """
                SELECT 1
                FROM usuarios
                WHERE LOWER(email) = LOWER(?)
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, email);

            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private UsuarioGuardado convertirUsuario(
            ResultSet resultado
    ) throws SQLException {

        int id = resultado.getInt("id");
        String username = resultado.getString("username");
        String email = resultado.getString("email");
        String passwordHash = resultado.getString("password_hash");
        String region = resultado.getString("region");
        int rango = resultado.getInt("rango");
        String rol = resultado.getString("rol");

        Usuario usuario = new Usuario(
                id,
                username,
                email,
                passwordHash,
                region,
                rango
        );

        return new UsuarioGuardado(
                usuario,
                rol,
                rango
        );
    }
}
