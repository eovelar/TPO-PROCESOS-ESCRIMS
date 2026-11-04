package pruebas;

import domain.Usuario;

import java.time.LocalTime;

public class PruebaUsuarios {

    public static void main(String[] args) {

        Usuario jugador = new Usuario(
                1, "Jugador1", "jugador1@mail.com",
                "hash123", "LAS", 5
        );

        System.out.println("--- Perfil inicial ---");

        System.out.println("Usuario: " + jugador.getUsername());
        System.out.println("Email: " + jugador.getEmail());
        System.out.println("Region: " + jugador.getRegion());
        System.out.println("Email verificado: "
                + jugador.isEmailVerificado());

        jugador.setJuegoPrincipal("Valorant");

        jugador.agregarRango("Valorant", 6);
        jugador.agregarRango("LoL", 4);
        jugador.agregarRango("CS2", 5);

        jugador.agregarRolPreferido("Duelist");
        jugador.agregarRolPreferido("Controller");
        jugador.agregarRolPreferido("Duelist");

        jugador.establecerDisponibilidad(
                LocalTime.of(18, 0),
                LocalTime.of(23, 0)
        );

        jugador.cambiarRegion("BR");

        System.out.println("\n--- Perfil actualizado ---");

        System.out.println("Juego principal: "
                + jugador.getJuegoPrincipal());

        System.out.println("Rango Valorant: "
                + jugador.getRangoPorJuego("Valorant"));

        System.out.println("Rango LoL: "
                + jugador.getRangoPorJuego("LoL"));

        System.out.println("Rango CS2: "
                + jugador.getRangoPorJuego("CS2"));

        System.out.println("Roles: "
                + jugador.getRolesPreferidos());

        System.out.println("Disponibilidad: "
                + jugador.getDisponibleDesde()
                + " a " + jugador.getDisponibleHasta());

        System.out.println("Region: "
                + jugador.getRegion());

        jugador.verificarEmail();

        System.out.println("Email verificado: "
                + jugador.isEmailVerificado());

        System.out.println("\n--- Prueba de validaciones ---");

        try {
            jugador.establecerDisponibilidad(
                    LocalTime.of(23, 0),
                    LocalTime.of(18, 0)
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Horario rechazado: "
                    + e.getMessage());
        }

        System.out.println("Cantidad de roles preferidos: "
                + jugador.getRolesPreferidos().size());
    }
}
