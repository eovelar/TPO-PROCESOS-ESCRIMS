
package pruebas;

import persistence.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PruebaConexionBD {

    public static void main(String[] args) {

        System.out.println("Probando conexion con Supabase...");

        try (Connection conexion = ConexionBD.conectar()) {

            System.out.println("Conexion establecida: OK");

            comprobarTabla(conexion, "usuarios");
            comprobarTabla(conexion, "scrims");
            comprobarTabla(conexion, "postulaciones");

            System.out.println(
                    "\nTodas las comprobaciones pasaron correctamente"
            );

        } catch (SQLException | IllegalStateException e) {

            System.err.println(
                    "Error de conexion: " + e.getMessage()
            );
        }
    }

    private static void comprobarTabla(
            Connection conexion,
            String tabla
    ) throws SQLException {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                    AND table_name = ?
                )
                """;

        try (PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, tabla);

            try (ResultSet resultado = consulta.executeQuery()) {

                resultado.next();

                if (!resultado.getBoolean(1)) {
                    throw new SQLException(
                            "No se encontro la tabla: " + tabla
                    );
                }

                System.out.println("Tabla " + tabla + ": OK");
            }
        }
    }
}
