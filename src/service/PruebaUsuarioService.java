
package pruebas;

import domain.Usuario;
import service.UsuarioService;

public class PruebaUsuarioService {

    public static void main(String[] args) {

        UsuarioService servicio = new UsuarioService();

        System.out.println("--- Registro de usuarios ---");

        Usuario jugador1 = servicio.registrarUsuario(
                1, "Jugador1", "jugador1@mail.com",
                "claveSegura123", "LAS", 5
        );

        Usuario jugador2 = servicio.registrarUsuario(
                2, "Jugador2", "jugador2@mail.com",
                "otraClave456", "BR", 6
        );

        System.out.println("Usuario registrado: "
                + jugador1.getUsername());

        System.out.println("Usuario registrado: "
                + jugador2.getUsername());

        System.out.println("Total de usuarios: "
                + servicio.listarUsuarios().size());

        System.out.println("\n--- Contraseñas ---");

        System.out.println("Contraseña almacenada como hash: "
                + jugador1.getPasswordHash().startsWith("pbkdf2-sha256:"));

        System.out.println("Contraseñas distintas: "
                + !jugador1.getPasswordHash()
                .equals(jugador2.getPasswordHash()));

        System.out.println("\n--- Inicio de sesión ---");

        Usuario autenticado = servicio.iniciarSesion(
                "jugador1@mail.com", "claveSegura123"
        );

        System.out.println("Inicio correcto: "
                + autenticado.getUsername());

        try {
            servicio.iniciarSesion(
                    "jugador1@mail.com", "claveIncorrecta"
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Acceso rechazado: "
                    + e.getMessage());
        }

        System.out.println("\n--- Usuarios duplicados ---");

        try {
            servicio.registrarUsuario(
                    3, "Jugador1", "nuevo@mail.com",
                    "claveSegura789", "LAS", 5
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Duplicado rechazado: "
                    + e.getMessage());
        }

        System.out.println("\n--- Contraseña corta ---");

        try {
            servicio.registrarUsuario(
                    4, "Jugador4", "jugador4@mail.com",
                    "1234", "LAS", 4
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Registro rechazado: "
                    + e.getMessage());
        }
    }
}
