
import domain.Scrim;
import domain.Usuario;
import service.ScrimService;

import java.time.LocalDate;
import java.util.List;

public class PruebaBusquedaScrims {

    public static void main(String[] args) {

        ScrimService servicio = new ScrimService();

        LocalDate fecha = LocalDate.now().plusDays(2);

        Scrim scrim1 = new Scrim(
                1, "Valorant", "5v5", "LAS",
                3, 7, 80,
                fecha.atTime(20, 0),
                60, 10
        );

        Scrim scrim2 = new Scrim(
                2, "LoL", "5v5", "LAS",
                4, 9, 100,
                fecha.atTime(21, 0),
                90, 10
        );

        Scrim scrim3 = new Scrim(
                3, "CS2", "5v5", "BR",
                2, 6, 60,
                fecha.plusDays(1).atTime(19, 0),
                60, 10
        );

        Scrim scrim4 = new Scrim(
                4, "Valorant", "1v1", "LAS",
                4, 7, 80,
                fecha.atTime(22, 0),
                30, 2
        );

        servicio.agregarScrim(scrim1);
        servicio.agregarScrim(scrim2);
        servicio.agregarScrim(scrim3);
        servicio.agregarScrim(scrim4);

        Usuario jugador1 = new Usuario(
                1, "Jugador1", "jugador1@mail.com",
                "hash123", "LAS", 5
        );

        Usuario jugador2 = new Usuario(
                2, "Jugador2", "jugador2@mail.com",
                "hash456", "LAS", 6
        );

        scrim4.registrarJugadorAceptado(jugador1);
        scrim4.registrarJugadorAceptado(jugador2);

        System.out.println("--- Todos los scrims ---");
        mostrarResultados(servicio.listarScrims());

        System.out.println("\n--- Buscar por juego: Valorant ---");
        mostrarResultados(servicio.buscarPorJuego("Valorant"));

        System.out.println("\n--- Buscar por formato: 1v1 ---");
        mostrarResultados(servicio.buscarPorFormato("1v1"));

        System.out.println("\n--- Buscar por region: LAS ---");
        mostrarResultados(servicio.buscarPorRegion("LAS"));

        System.out.println("\n--- Buscar por rango: 5 ---");
        mostrarResultados(servicio.buscarPorRango(5));

        System.out.println("\n--- Buscar por fecha ---");
        mostrarResultados(servicio.buscarPorFecha(fecha));

        System.out.println("\n--- Buscar por latencia: 70 ms ---");
        mostrarResultados(servicio.buscarPorLatencia(70));

        System.out.println("\n--- Buscar por estado: Lobby armado ---");
        mostrarResultados(servicio.buscarPorEstado("Lobby armado"));

        System.out.println("\n--- Scrims con cupos disponibles ---");
        mostrarResultados(servicio.buscarConCuposDisponibles());

        System.out.println("\n--- Busqueda combinada ---");

        List<Scrim> resultados = servicio.buscarConFiltros(
                "Valorant",
                "5v5",
                "LAS",
                5,
                fecha,
                60,
                "Buscando jugadores",
                true
        );

        mostrarResultados(resultados);
    }

    private static void mostrarResultados(List<Scrim> scrims) {

        if (scrims.isEmpty()) {
            System.out.println("No se encontraron scrims");
            return;
        }

        for (Scrim scrim : scrims) {
            System.out.println(
                    "ID: " + scrim.getId()
                            + " | Juego: " + scrim.getJuego()
                            + " | Formato: " + scrim.getFormato()
                            + " | Region: " + scrim.getRegion()
                            + " | Estado: " + scrim.getEstadoActual()
                            + " | Cupos: " + scrim.getCuposDisponibles()
            );
        }
    }
}
