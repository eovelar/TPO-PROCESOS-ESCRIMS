
import domain.Equipo;
import domain.Usuario;

public class PruebaEquipos {

    public static void main(String[] args) {

        Usuario jugador1 = new Usuario(
                1, "Jugador1", "jugador1@mail.com",
                "hash123", "LAS", 5
        );

        Usuario jugador2 = new Usuario(
                2, "Jugador2", "jugador2@mail.com",
                "hash456", "LAS", 6
        );

        Usuario jugador3 = new Usuario(
                3, "Jugador3", "jugador3@mail.com",
                "hash789", "LAS", 5
        );

        Equipo equipo1 = new Equipo(1, 1, 2);
        Equipo equipo2 = new Equipo(2, 2, 2);

        equipo1.agregarJugador(jugador1);
        equipo1.agregarJugador(jugador2);
        equipo2.agregarJugador(jugador3);

        equipo1.asignarRol(jugador1, "Duelist");
        equipo1.asignarRol(jugador2, "Controller");
        equipo2.asignarRol(jugador3, "Sentinel");

        System.out.println("--- Equipos iniciales ---");

        System.out.println("Equipo 1: "
                + equipo1.getCantidadJugadores() + " jugadores");

        System.out.println("Cupos disponibles: "
                + equipo1.getCuposDisponibles());

        System.out.println("Rol de Jugador1: "
                + equipo1.getRolJugador(jugador1));

        try {
            equipo1.agregarJugador(jugador1);
        } catch (IllegalArgumentException e) {
            System.out.println("Duplicado rechazado: "
                    + e.getMessage());
        }

        System.out.println("\n--- Intercambio de jugadores ---");

        equipo1.intercambiarJugadores(
                equipo2, jugador2, jugador3
        );

        System.out.println("Equipo 1:");

        for (Usuario jugador : equipo1.getJugadores()) {
            System.out.println("- " + jugador.getUsername()
                    + " | Rol: " + equipo1.getRolJugador(jugador));
        }

        System.out.println("Equipo 2:");

        for (Usuario jugador : equipo2.getJugadores()) {
            System.out.println("- " + jugador.getUsername()
                    + " | Rol: " + equipo2.getRolJugador(jugador));
        }
    }
}
