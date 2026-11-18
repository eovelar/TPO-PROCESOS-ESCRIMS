
package pruebas;

import facade.EscrimsFacade;
import persistence.ConexionBD;
import persistence.LobbyRepository;
import persistence.PostulacionRepository;
import persistence.ScrimRepository;
import persistence.UsuarioRepository;
import state.ScrimContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Prueba de integracion del lobby contra PostgreSQL.
// Requiere las variables DB_* y la tabla public.confirmaciones
// (sql/confirmaciones.sql). Crea sus propios datos con prefijo unico.
public class PruebaLobbyIntegracion {

    private interface Accion {
        void ejecutar() throws Exception;
    }

    private static final String SUFIJO =
            Long.toString(System.currentTimeMillis() % 100000000L);

    private static final EscrimsFacade facade = new EscrimsFacade();
    private static final UsuarioRepository usuarios = new UsuarioRepository();
    private static final PostulacionRepository postulaciones =
            new PostulacionRepository();

    private static int contadorUsuarios = 0;
    private static int pruebasOk = 0;

    public static void main(String[] args) throws Exception {

        probarReconstruccionDeEstados();
        probarFlujoFeliz();
        probarConfirmacionesInvalidas();
        probarTransicionesInvalidas();
        probarCancelacion();
        probarConfirmacionesSimultaneas();
        probarInicioProgramado();

        System.out.println(
                "\nPruebas del lobby correctas: " + pruebasOk
        );
    }

    // ---------- pruebas ----------

    private static void probarReconstruccionDeEstados() {

        String[] nombres = {
                "Buscando jugadores", "Lobby armado", "Confirmado",
                "En juego", "Finalizado", "Cancelado"
        };

        for (String nombre : nombres) {
            igual(
                    nombre,
                    ScrimContext.desdeNombre(nombre).getEstadoActual(),
                    "Reconstruccion del estado " + nombre
            );
        }

        esperar(
                IllegalArgumentException.class,
                () -> ScrimContext.desdeNombre("Inventado"),
                "Estado desconocido rechazado"
        );
    }

    private static void probarFlujoFeliz() throws Exception {

        System.out.println("\n--- Flujo feliz ---");

        int scrim = crearScrim(2);
        int[] jugadores = {crearUsuario(), crearUsuario()};

        postularYAceptar(scrim, jugadores);

        igual("Lobby armado", estado(scrim), "Cupos completos -> Lobby armado");

        LobbyRepository.ResultadoConfirmacion r1 =
                facade.confirmarParticipacion(jugadores[0], scrim);

        igual(1, r1.confirmados(), "Primera confirmacion cuenta 1");
        verificar(r1.transicion() == null, "Con 1/2 no cambia el estado");
        igual("Lobby armado", estado(scrim), "Sigue en Lobby armado");

        LobbyRepository.ResultadoConfirmacion r2 =
                facade.confirmarParticipacion(jugadores[1], scrim);

        igual(2, r2.confirmados(), "Segunda confirmacion cuenta 2");
        verificar(r2.transicion() != null, "Con 2/2 hay transicion");
        igual("Lobby armado", r2.transicion().estadoAnterior(), "Estado anterior");
        igual("Confirmado", r2.transicion().estadoNuevo(), "Estado nuevo");

        // una instancia nueva lee lo persistido (como tras reiniciar Java)
        igual(
                "Confirmado",
                new EscrimsFacade().consultarLobby(scrim).estado(),
                "El estado Confirmado sobrevive al reinicio"
        );
        igual(
                2,
                new LobbyRepository().consultar(scrim).confirmados(),
                "Las confirmaciones sobreviven al reinicio"
        );

        igual(
                "En juego",
                facade.transicionarScrim(scrim, "iniciar").estadoNuevo(),
                "Confirmado -> En juego"
        );
        igual(
                "Finalizado",
                facade.transicionarScrim(scrim, "finalizar").estadoNuevo(),
                "En juego -> Finalizado"
        );
        igual("Finalizado", estado(scrim), "Estado final persistido");
    }

    private static void probarConfirmacionesInvalidas() throws Exception {

        System.out.println("\n--- Confirmaciones invalidas ---");

        int scrim = crearScrim(2);
        int[] jugadores = {crearUsuario(), crearUsuario()};
        int ajeno = crearUsuario();

        // aun buscando jugadores: nadie puede confirmar
        esperar(
                IllegalStateException.class,
                () -> facade.confirmarParticipacion(jugadores[0], scrim),
                "No se confirma fuera de Lobby armado"
        );

        // se postula antes de que se complete el lobby y queda pendiente
        facade.postularJugador(ajeno, scrim, "Flexible");

        postularYAceptar(scrim, jugadores);

        esperar(
                IllegalArgumentException.class,
                () -> facade.confirmarParticipacion(ajeno, scrim),
                "Un usuario no aceptado no puede confirmar"
        );

        facade.confirmarParticipacion(jugadores[0], scrim);

        esperar(
                IllegalStateException.class,
                () -> facade.confirmarParticipacion(jugadores[0], scrim),
                "No se confirma dos veces"
        );

        igual(
                1,
                facade.consultarLobby(scrim).confirmados(),
                "La doble confirmacion no suma"
        );
        igual("Lobby armado", estado(scrim), "Lobby incompleto no avanza");

        esperar(
                IllegalArgumentException.class,
                () -> facade.confirmarParticipacion(jugadores[0], 999999),
                "Partida inexistente"
        );
    }

    private static void probarTransicionesInvalidas() throws Exception {

        System.out.println("\n--- Transiciones invalidas ---");

        int scrim = crearScrim(2);
        int[] jugadores = {crearUsuario(), crearUsuario()};

        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "iniciar"),
                "Buscando -> iniciar"
        );
        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "finalizar"),
                "Buscando -> finalizar"
        );

        postularYAceptar(scrim, jugadores);

        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "iniciar"),
                "Lobby armado sin confirmar -> iniciar"
        );

        facade.confirmarParticipacion(jugadores[0], scrim);
        facade.confirmarParticipacion(jugadores[1], scrim);

        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "finalizar"),
                "Confirmado -> finalizar"
        );
        esperar(
                IllegalStateException.class,
                () -> facade.confirmarParticipacion(jugadores[0], scrim),
                "Confirmado no admite mas confirmaciones"
        );

        facade.transicionarScrim(scrim, "iniciar");

        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "cancelar"),
                "En juego -> cancelar"
        );
        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "iniciar"),
                "En juego -> iniciar"
        );

        facade.transicionarScrim(scrim, "finalizar");

        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(scrim, "cancelar"),
                "Finalizado -> cancelar"
        );
        esperar(
                IllegalArgumentException.class,
                () -> facade.transicionarScrim(scrim, "borrar"),
                "Accion desconocida"
        );
    }

    private static void probarCancelacion() throws Exception {

        System.out.println("\n--- Cancelacion ---");

        int buscando = crearScrim(2);

        igual(
                "Cancelado",
                facade.transicionarScrim(buscando, "cancelar").estadoNuevo(),
                "Buscando -> cancelar"
        );

        int lobby = crearScrim(2);
        int[] jugadores = {crearUsuario(), crearUsuario()};
        postularYAceptar(lobby, jugadores);

        igual(
                "Cancelado",
                facade.transicionarScrim(lobby, "cancelar").estadoNuevo(),
                "Lobby armado -> cancelar"
        );
        esperar(
                IllegalStateException.class,
                () -> facade.confirmarParticipacion(jugadores[0], lobby),
                "No se confirma una partida cancelada"
        );
        esperar(
                IllegalStateException.class,
                () -> facade.transicionarScrim(lobby, "cancelar"),
                "No se cancela dos veces"
        );

        int confirmada = crearScrim(2);
        int[] otros = {crearUsuario(), crearUsuario()};
        postularYAceptar(confirmada, otros);
        facade.confirmarParticipacion(otros[0], confirmada);
        facade.confirmarParticipacion(otros[1], confirmada);

        igual(
                "Cancelado",
                facade.transicionarScrim(confirmada, "cancelar").estadoNuevo(),
                "Confirmado -> cancelar"
        );
    }

    private static void probarConfirmacionesSimultaneas() throws Exception {

        System.out.println("\n--- Confirmaciones simultaneas ---");

        int cupos = 6;
        int scrim = crearScrim(cupos);
        int[] jugadores = new int[cupos];

        for (int i = 0; i < cupos; i++) {
            jugadores[i] = crearUsuario();
        }

        postularYAceptar(scrim, jugadores);

        ExecutorService hilos = Executors.newFixedThreadPool(cupos);
        List<Future<LobbyRepository.ResultadoConfirmacion>> tareas =
                new ArrayList<>();

        for (int jugador : jugadores) {
            Callable<LobbyRepository.ResultadoConfirmacion> tarea =
                    () -> facade.confirmarParticipacion(jugador, scrim);
            tareas.add(hilos.submit(tarea));
        }

        int transiciones = 0;

        for (Future<LobbyRepository.ResultadoConfirmacion> tarea : tareas) {
            if (tarea.get().transicion() != null) {
                transiciones++;
            }
        }

        hilos.shutdown();

        igual(1, transiciones, "Una sola transicion a Confirmado");
        igual("Confirmado", estado(scrim), "Estado coherente");
        igual(
                cupos,
                facade.consultarLobby(scrim).confirmados(),
                "Todas las confirmaciones quedaron registradas"
        );
    }

    private static void probarInicioProgramado() throws Exception {

        System.out.println("\n--- Inicio por horario ---");

        int vencida = crearScrim(2);
        int futura = crearScrim(2);

        for (int scrim : new int[]{vencida, futura}) {

            int[] jugadores = {crearUsuario(), crearUsuario()};
            postularYAceptar(scrim, jugadores);
            facade.confirmarParticipacion(jugadores[0], scrim);
            facade.confirmarParticipacion(jugadores[1], scrim);
        }

        // una partida cuyo horario ya llego
        try (Connection c = ConexionBD.conectar();
             PreparedStatement q = c.prepareStatement(
                     "UPDATE public.scrims SET fecha_hora = ? WHERE id = ?"
             )) {
            q.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(
                            LocalDateTime.now().minusMinutes(5)
                    )
            );
            q.setInt(2, vencida);
            q.executeUpdate();
        }

        facade.iniciarProgramadas();

        igual("En juego", estado(vencida), "Vencida inicia sola");
        igual("Confirmado", estado(futura), "La futura espera su horario");
    }

    // ---------- utilidades ----------

    private static int crearScrim(int cupos) throws SQLException {

        return facade.crearScrim(
                null,
                "Valorant",
                "Prueba",
                "LAS",
                1,
                10,
                100,
                LocalDateTime.now().plusDays(2),
                60,
                cupos
        );
    }

    private static int crearUsuario() throws SQLException {

        contadorUsuarios++;
        String nombre = "lobby" + SUFIJO + "_" + contadorUsuarios;

        return usuarios.guardar(
                nombre,
                nombre + "@test.com",
                "hash-prueba",
                "LAS",
                5,
                "JUGADOR"
        ).usuario().getId();
    }

    private static void postularYAceptar(int scrim, int[] jugadores)
            throws SQLException {

        List<Integer> solicitudes = new ArrayList<>();

        for (int jugador : jugadores) {
            solicitudes.add(
                    facade.postularJugador(jugador, scrim, "Flexible")
            );
        }

        for (int solicitud : solicitudes) {
            facade.procesarPostulacion(solicitud, "aceptar");
        }
    }

    private static String estado(int scrimId) throws SQLException {

        for (ScrimRepository.ScrimGuardado s : facade.listarScrims()) {
            if (s.id() == scrimId) {
                return s.estado();
            }
        }

        throw new IllegalStateException("Partida no encontrada");
    }

    private static void verificar(boolean condicion, String mensaje) {

        if (!condicion) {
            throw new AssertionError("FALLO: " + mensaje);
        }

        pruebasOk++;
        System.out.println("OK  " + mensaje);
    }

    private static void igual(Object esperado, Object actual, String mensaje) {

        if (!esperado.equals(actual)) {
            throw new AssertionError(
                    "FALLO: " + mensaje
                            + " (esperado " + esperado
                            + ", obtenido " + actual + ")"
            );
        }

        pruebasOk++;
        System.out.println("OK  " + mensaje);
    }

    private static void esperar(
            Class<? extends Exception> tipo,
            Accion accion,
            String mensaje
    ) {

        try {
            accion.ejecutar();

        } catch (Exception e) {

            if (tipo.isInstance(e)) {
                pruebasOk++;
                System.out.println("OK  " + mensaje);
                return;
            }

            throw new AssertionError(
                    "FALLO: " + mensaje + " (llego " + e + ")"
            );
        }

        throw new AssertionError(
                "FALLO: " + mensaje + " (no lanzo " + tipo.getSimpleName() + ")"
        );
    }
}
