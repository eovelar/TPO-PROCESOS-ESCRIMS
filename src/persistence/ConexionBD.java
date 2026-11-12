
package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    public static Connection conectar() throws SQLException {

        String host = obtenerVariable("DB_HOST");
        String puerto = obtenerVariable("DB_PORT");
        String base = obtenerVariable("DB_NAME");
        String usuario = obtenerVariable("DB_USER");
        String contrasena = obtenerVariable("DB_PASSWORD");

        String url = "jdbc:postgresql://"
                + host + ":" + puerto + "/" + base;

        Properties propiedades = new Properties();

        propiedades.setProperty("user", usuario);
        propiedades.setProperty("password", contrasena);
        propiedades.setProperty("sslmode", "require");
        propiedades.setProperty("connectTimeout", "10");
        propiedades.setProperty("socketTimeout", "15");

        return DriverManager.getConnection(url, propiedades);
    }

    private static String obtenerVariable(String nombre) {

        String valor = System.getenv(nombre);

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la variable: " + nombre
            );
        }

        return valor.trim();
    }
}
