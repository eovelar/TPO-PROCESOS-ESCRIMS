package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import domain.Usuario;
import facade.EscrimsFacade;
import matchmaking.Candidato;
import persistence.PostulacionRepository;
import persistence.ScrimRepository;
import service.NotificacionScrimService;
import service.WebAuthService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ServidorWeb {

    private final WebAuthService auth = new WebAuthService();

    private final EscrimsFacade facade = new EscrimsFacade();

    private final NotificacionScrimService notificaciones;

    private final Path carpetaWeb = Path.of("src", "web");

    private final HttpServer servidor;
    private final int puerto;
    private final boolean produccion;
    private final String origenPublico;
    private final String emailAdministrador;

    public ServidorWeb(int puerto) throws IOException {

        this.puerto = puerto;
        this.produccion = System.getenv("PORT") != null;
        this.origenPublico = System.getenv("PUBLIC_ORIGIN");

        String emailConfigurado = System.getenv("ADMIN_EMAIL");

        this.emailAdministrador = emailConfigurado == null
                ? null
                : emailConfigurado.trim().toLowerCase(Locale.ROOT);

        if (produccion &&
                (origenPublico == null || origenPublico.isBlank())) {
            throw new IllegalStateException(
                    "Falta configurar PUBLIC_ORIGIN"
            );
        }

        notificaciones = new NotificacionScrimService(produccion);

        notificaciones.configurar(
                "jugadores de eScrims",
                "organizador de eScrims"
        );

        String direccion = produccion
                ? "0.0.0.0"
                : "127.0.0.1";

        servidor = HttpServer.create(
                new InetSocketAddress(direccion, puerto),
                0
        );

        servidor.createContext("/health", this::health);
        servidor.createContext("/api/auth", this::manejarAuth);
        servidor.createContext("/api/scrims", this::manejarScrims);

        servidor.createContext(
                "/api/postulaciones",
                this::manejarPostulaciones
        );

        servidor.createContext(
                "/api/organizador/postulaciones",
                this::manejarOrganizador
        );

        servidor.createContext(
                "/api/organizador/recomendaciones",
                this::manejarRecomendaciones
        );

        servidor.createContext("/", this::servirPagina);
    }

    private void health(HttpExchange ex) throws IOException {

        if (!ex.getRequestURI().getPath().equals("/health")) {
            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (!ex.getRequestMethod().equals("GET")) {
            error(ex, 405, "Metodo no permitido");
            return;
        }

        enviar(
                ex,
                200,
                "READY".getBytes(StandardCharsets.UTF_8),
                "text/plain; charset=UTF-8"
        );
    }

    private void manejarAuth(HttpExchange ex) throws IOException {

        String ruta = ex.getRequestURI().getPath();
        String metodo = ex.getRequestMethod();

        if (ruta.equals("/api/auth/me")) {

            if (!metodo.equals("GET")) {
                error(ex, 405, "Metodo no permitido");
                return;
            }

            WebAuthService.Sesion sesion = sesionActual(ex);

            enviarJson(
                    ex,
                    200,
                    sesion == null
                            ? "{\"autenticado\":false}"
                            : usuarioJson(sesion)
            );
            return;
        }

        if (ruta.equals("/api/auth/logout")) {

            if (!metodo.equals("POST")) {
                error(ex, 405, "Metodo no permitido");
                return;
            }

            if (!validarOrigen(ex)) {
                return;
            }

            auth.cerrarSesion(leerCookie(ex));

            ex.getResponseHeaders().add(
                    "Set-Cookie",
                    cookieExpirada()
            );

            enviarJson(
                    ex,
                    200,
                    "{\"mensaje\":\"Sesion cerrada\"}"
            );
            return;
        }

        if (!ruta.equals("/api/auth/login") &&
                !ruta.equals("/api/auth/register")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (!metodo.equals("POST")) {
            error(ex, 405, "Metodo no permitido");
            return;
        }

        if (!validarOrigen(ex) || !validarFormulario(ex)) {
            return;
        }

        try {

            Map<String, String> datos = formulario(ex);
            WebAuthService.Acceso acceso;

            if (ruta.equals("/api/auth/register")) {

                acceso = auth.registrar(
                        datos.get("username"),
                        datos.get("email"),
                        datos.get("password"),
                        datos.get("region"),
                        entero(datos, "rango")
                );

            } else {

                acceso = auth.iniciarSesion(
                        datos.get("email"),
                        datos.get("password")
                );
            }

            if ("ORGANIZADOR".equals(acceso.sesion().rol())
                    && !esOrganizadorAutorizado(acceso.sesion())) {

                auth.cerrarSesion(acceso.token());

                error(
                        ex,
                        403,
                        "Cuenta de organizador no autorizada"
                );
                return;
            }

            if (!"JUGADOR".equals(acceso.sesion().rol())
                    && !"ORGANIZADOR".equals(acceso.sesion().rol())) {

                auth.cerrarSesion(acceso.token());

                error(
                        ex,
                        403,
                        "Rol no habilitado"
                );
                return;
            }

            ex.getResponseHeaders().add(
                    "Set-Cookie",
                    cookieSesion(acceso.token())
            );

            enviarJson(
                    ex,
                    200,
                    usuarioJson(acceso.sesion())
            );

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            System.err.println(
                    "Error de autenticacion: "
                            + e.getClass().getSimpleName()
            );

            error(
                    ex,
                    503,
                    "El servicio de cuentas no esta disponible"
            );
        }
    }

    private void manejarScrims(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/scrims")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        switch (ex.getRequestMethod()) {

            case "GET" -> listarScrims(ex);
            case "POST" -> crearScrim(ex);

            default -> error(
                    ex,
                    405,
                    "Metodo no permitido"
            );
        }
    }

    private void listarScrims(HttpExchange ex)
            throws IOException {

        try {

            List<ScrimRepository.ScrimGuardado> lista =
                    facade.listarScrims();

            StringBuilder json = new StringBuilder("[");

            for (ScrimRepository.ScrimGuardado s : lista) {

                if (json.length() > 1) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"id\":")
                        .append(s.id())

                        .append(",\"juego\":\"")
                        .append(escapar(s.juego()))

                        .append("\",\"formato\":\"")
                        .append(escapar(s.formato()))

                        .append("\",\"region\":\"")
                        .append(escapar(s.region()))

                        .append("\",\"fecha\":\"")
                        .append(escapar(s.fecha()))

                        .append("\",\"estado\":\"")
                        .append(escapar(s.estado()))

                        .append("\",\"cuposDisponibles\":")
                        .append(s.cuposDisponibles())

                        .append(",\"cuposTotales\":")
                        .append(s.cuposTotales())

                        .append("}");
            }

            enviarJson(
                    ex,
                    200,
                    json.append("]").toString()
            );

        } catch (SQLException e) {

            errorBaseDatos(ex, e);

        } catch (RuntimeException e) {

            System.err.println("Error al cargar partidas");
            e.printStackTrace();

            error(
                    ex,
                    500,
                    "No se pudieron cargar las partidas"
            );
        }
    }

    private void crearScrim(HttpExchange ex)
            throws IOException {

        WebAuthService.Sesion sesion = requerirOrganizador(ex);

        if (sesion == null) {
            return;
        }

        if (!validarOrigen(ex) || !validarFormulario(ex)) {
            return;
        }

        try {

            Map<String, String> datos = formulario(ex);

            String juego = datos.get("juego");
            String formato = datos.get("formato");
            String region = datos.get("region");

            if (juego == null ||
                    !Set.of("Valorant", "LoL", "CS2").contains(juego)) {
                throw new IllegalArgumentException(
                        "Juego no permitido"
                );
            }

            if (region == null ||
                    !Set.of("LAS", "LAN", "BR").contains(region)) {
                throw new IllegalArgumentException(
                        "Region no permitida"
                );
            }

            if (formato == null ||
                    !Set.of("1v1", "3v3", "5v5").contains(formato)) {
                throw new IllegalArgumentException(
                        "Formato no permitido"
                );
            }

            int rangoMin = entero(datos, "rangoMin");
            int rangoMax = entero(datos, "rangoMax");
            int latenciaMax = entero(datos, "latenciaMax");
            int duracion = entero(datos, "duracion");
            int cupos = entero(datos, "cupos");

            if (rangoMax > 10 || rangoMin > rangoMax) {
                throw new IllegalArgumentException(
                        "Los rangos de la partida no son validos"
                );
            }

            int cuposFormato = switch (formato) {
                case "1v1" -> 2;
                case "3v3" -> 6;
                case "5v5" -> 10;
                default -> 0;
            };

            if (cupos != cuposFormato) {
                throw new IllegalArgumentException(
                        "Los cupos no coinciden con el formato"
                );
            }

            LocalDateTime fechaHora = LocalDateTime.parse(
                    datos.getOrDefault("fechaHora", "")
            );

            // la cuenta administradora no necesita registro en usuarios
            Integer creadorId = null;

            int id = facade.crearScrim(
                    creadorId,
                    juego,
                    formato,
                    region,
                    rangoMin,
                    rangoMax,
                    latenciaMax,
                    fechaHora,
                    duracion,
                    cupos
            );

            System.out.println(
                    "Nueva scrim creada: #" + id + " - " + juego
            );

            enviarJson(
                    ex,
                    201,
                    "{"
                            + "\"mensaje\":\"Partida creada correctamente\","
                            + "\"id\":" + id
                            + ",\"estado\":\"Buscando jugadores\""
                            + "}"
            );

        } catch (DateTimeParseException e) {

            error(
                    ex,
                    400,
                    "La fecha y hora no son validas"
            );

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (SQLException e) {

            errorBaseDatos(ex, e);
        }
    }

    private void manejarPostulaciones(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/postulaciones")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        WebAuthService.Sesion sesion = requerirSesion(ex);

        if (sesion == null) {
            return;
        }

        try {

            switch (ex.getRequestMethod()) {

                case "GET" -> {

                    if (!"JUGADOR".equals(sesion.rol())) {
                        error(
                                ex,
                                403,
                                "Solo los jugadores pueden consultar sus postulaciones"
                        );
                        return;
                    }

                    List<PostulacionRepository.PostulacionGuardada> lista =
                            facade.listarPostulacionesJugador(
                                    sesion.usuario().getId()
                            );

                    enviarJson(
                            ex,
                            200,
                            postulacionesJson(lista, false)
                    );
                }

                case "POST" -> {

                    if (!validarOrigen(ex) ||
                            !validarFormulario(ex)) {
                        return;
                    }

                    if (!"JUGADOR".equals(sesion.rol())) {

                        error(
                                ex,
                                403,
                                "Solo los jugadores pueden postularse"
                        );
                        return;
                    }

                    int scrimId = entero(
                            formulario(ex),
                            "scrimId"
                    );

                    int id = facade.postularJugador(
                            sesion.usuario().getId(),
                            scrimId,
                            "Flexible"
                    );

                    enviarJson(
                            ex,
                            201,
                            "{"
                                    + "\"mensaje\":\"Postulacion registrada\","
                                    + "\"id\":" + id
                                    + ",\"scrimId\":" + scrimId
                                    + ",\"estado\":\"Pendiente\""
                                    + "}"
                    );
                }

                default -> error(
                        ex,
                        405,
                        "Metodo no permitido"
                );
            }

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            error(ex, 409, e.getMessage());

        } catch (SQLException e) {

            if ("23505".equals(e.getSQLState())) {
                error(
                        ex,
                        409,
                        "Ya existe una postulacion para esta partida"
                );
            } else {
                errorBaseDatos(ex, e);
            }
        }
    }

    private void manejarOrganizador(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/organizador/postulaciones")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        WebAuthService.Sesion sesion = requerirOrganizador(ex);

        if (sesion == null) {
            return;
        }

        try {

            switch (ex.getRequestMethod()) {

                case "GET" -> {

                    List<PostulacionRepository.PostulacionGuardada> lista =
                            facade.listarPostulacionesOrganizador();

                    enviarJson(
                            ex,
                            200,
                            postulacionesJson(lista, true)
                    );
                }

                case "POST" -> {

                    if (!validarOrigen(ex) ||
                            !validarFormulario(ex)) {
                        return;
                    }

                    Map<String, String> datos = formulario(ex);
                    int id = entero(datos, "postulacionId");

                    PostulacionRepository.PostulacionGuardada solicitud =
                            buscarPostulacion(id);

                    ScrimRepository.ScrimGuardado anterior =
                            buscarScrim(solicitud.scrimId());

                    String estado = facade.procesarPostulacion(
                            id,
                            datos.get("accion")
                    );

                    try {

                        ScrimRepository.ScrimGuardado actual =
                                buscarScrim(solicitud.scrimId());

                        if (!anterior.estado().equals(actual.estado())) {

                            notificaciones.notificarCambio(
                                    actual.id(),
                                    actual.juego(),
                                    anterior.estado(),
                                    actual.estado()
                            );
                        }

                    } catch (SQLException | RuntimeException e) {

                        System.err.println(
                                "No se pudo verificar la notificacion"
                        );
                    }

                    enviarJson(
                            ex,
                            200,
                            "{"
                                    + "\"mensaje\":\"Solicitud procesada\","
                                    + "\"id\":" + id
                                    + ",\"estado\":\""
                                    + escapar(estado)
                                    + "\""
                                    + "}"
                    );
                }

                default -> error(
                        ex,
                        405,
                        "Metodo no permitido"
                );
            }

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            error(ex, 409, e.getMessage());

        } catch (SQLException e) {

            errorBaseDatos(ex, e);
        }
    }

    private void manejarRecomendaciones(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/organizador/recomendaciones")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (!"GET".equals(ex.getRequestMethod())) {
            error(ex, 405, "Metodo no permitido");
            return;
        }

        WebAuthService.Sesion sesion = requerirOrganizador(ex);

        if (sesion == null) {
            return;
        }

        try {

            Map<String, String> parametros = leerParametros(
                    ex.getRequestURI().getRawQuery()
            );

            int scrimId = entero(parametros, "scrimId");

            int compatibilidadMinima =
                    parametros.containsKey("compatibilidadMinima")
                            ? enteroNoNegativo(
                            parametros,
                            "compatibilidadMinima"
                    )
                            : 70;

            if (compatibilidadMinima > 100) {
                throw new IllegalArgumentException(
                        "La compatibilidad debe estar entre 0 y 100"
                );
            }

            List<Candidato> recomendados =
                    facade.buscarCandidatosCompatibles(
                            scrimId,
                            compatibilidadMinima
                    );

            StringBuilder json = new StringBuilder("[");
            int posicion = 1;

            for (Candidato candidato : recomendados) {

                if (json.length() > 1) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"posicion\":")
                        .append(posicion++)

                        .append(",\"usuarioId\":")
                        .append(candidato.usuario().getId())

                        .append(",\"usuario\":\"")
                        .append(escapar(
                                candidato.usuario().getUsername()
                        ))

                        .append("\",\"rango\":")
                        .append(candidato.rango())

                        .append(",\"latencia\":")
                        .append(candidato.latencia())

                        .append(",\"compatibilidad\":")
                        .append(candidato.compatibilidad())

                        .append(",\"abandonos\":")
                        .append(candidato.abandonos())

                        .append("}");
            }

            enviarJson(
                    ex,
                    200,
                    json.append("]").toString()
            );

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            error(ex, 409, e.getMessage());

        } catch (SQLException e) {

            errorBaseDatos(ex, e);
        }
    }

    private PostulacionRepository.PostulacionGuardada buscarPostulacion(
            int id
    ) throws SQLException {

        return facade.listarPostulacionesOrganizador()
                .stream()
                .filter(p -> p.id() == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Postulacion no encontrada"
                        )
                );
    }

    private ScrimRepository.ScrimGuardado buscarScrim(
            int id
    ) throws SQLException {

        return facade.listarScrims()
                .stream()
                .filter(s -> s.id() == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Partida no encontrada"
                        )
                );
    }

    private String postulacionesJson(
            List<PostulacionRepository.PostulacionGuardada> lista,
            boolean incluirUsuario
    ) {

        StringBuilder json = new StringBuilder("[");

        for (PostulacionRepository.PostulacionGuardada p : lista) {

            if (json.length() > 1) {
                json.append(',');
            }

            json.append("{")
                    .append("\"id\":")
                    .append(p.id())

                    .append(",\"scrimId\":")
                    .append(p.scrimId())

                    .append(",\"juego\":\"")
                    .append(escapar(p.juego()))

                    .append("\",\"formato\":\"")
                    .append(escapar(p.formato()))

                    .append("\",\"region\":\"")
                    .append(escapar(p.region()))

                    .append("\",\"fecha\":\"")
                    .append(escapar(p.fecha()))

                    .append("\",\"estado\":\"")
                    .append(escapar(p.estado()))
                    .append("\"");

            if (incluirUsuario) {

                json.append(",\"usuario\":\"")
                        .append(escapar(p.usuario()))
                        .append("\"");
            }

            json.append("}");
        }

        return json.append("]").toString();
    }

    private void servirPagina(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestMethod().equals("GET")) {

            error(ex, 405, "Metodo no permitido");
            return;
        }

        String archivo;
        String tipo;

        switch (ex.getRequestURI().getPath()) {

            case "/" -> {
                archivo = "index.html";
                tipo = "text/html; charset=UTF-8";
            }

            case "/styles.css" -> {
                archivo = "styles.css";
                tipo = "text/css; charset=UTF-8";
            }

            case "/app.js" -> {
                archivo = "app.js";
                tipo = "text/javascript; charset=UTF-8";
            }

            default -> {
                error(ex, 404, "Pagina no encontrada");
                return;
            }
        }

        Path ruta = carpetaWeb.resolve(archivo);

        if (!Files.isRegularFile(ruta)) {

            error(ex, 404, "Archivo no encontrado");
            return;
        }

        enviar(
                ex,
                200,
                Files.readAllBytes(ruta),
                tipo
        );
    }

    private WebAuthService.Sesion sesionActual(
            HttpExchange ex
    ) {

        return auth.obtenerSesion(leerCookie(ex));
    }

    private WebAuthService.Sesion requerirSesion(
            HttpExchange ex
    ) throws IOException {

        WebAuthService.Sesion sesion = sesionActual(ex);

        if (sesion == null) {
            error(
                    ex,
                    401,
                    "Inicia sesion para continuar"
            );
        }

        return sesion;
    }

    private WebAuthService.Sesion requerirOrganizador(
            HttpExchange ex
    ) throws IOException {

        WebAuthService.Sesion sesion = requerirSesion(ex);

        if (sesion == null) {
            return null;
        }

        if (!esOrganizadorAutorizado(sesion)) {
            error(
                    ex,
                    403,
                    "Solo el organizador autorizado puede acceder"
            );
            return null;
        }

        return sesion;
    }

    private boolean esOrganizadorAutorizado(
            WebAuthService.Sesion sesion
    ) {

        if (sesion == null ||
                !"ORGANIZADOR".equals(sesion.rol())) {
            return false;
        }

        String email = sesion.usuario().getEmail();

        if (email == null) {
            return false;
        }

        if (!produccion) {
            return email.equalsIgnoreCase(
                    "organizador@escrims.local"
            );
        }

        return emailAdministrador != null
                && !emailAdministrador.isBlank()
                && email.equalsIgnoreCase(emailAdministrador)
                && sesion.usuario().getId() == 999999;
    }

    private String leerCookie(HttpExchange ex) {

        String cookies = ex.getRequestHeaders()
                .getFirst("Cookie");

        if (cookies == null) {
            return null;
        }

        for (String parte : cookies.split(";")) {

            String cookie = parte.trim();

            if (cookie.startsWith("escrims_session=")) {

                return cookie.substring(
                        "escrims_session=".length()
                );
            }
        }

        return null;
    }

    private boolean validarOrigen(HttpExchange ex)
            throws IOException {

        String origen = ex.getRequestHeaders()
                .getFirst("Origin");

        boolean valido = produccion
                ? origenPublico.equals(origen)
                : origen == null
                || origen.equals("http://localhost:" + puerto)
                || origen.equals("http://127.0.0.1:" + puerto);

        if (!valido) {
            error(ex, 403, "Origen no permitido");
        }

        return valido;
    }

    private boolean validarFormulario(HttpExchange ex)
            throws IOException {

        String tipo = ex.getRequestHeaders()
                .getFirst("Content-Type");

        boolean valido = tipo != null &&
                tipo.toLowerCase(Locale.ROOT).startsWith(
                        "application/x-www-form-urlencoded"
                );

        if (!valido) {
            error(ex, 415, "Formato no soportado");
        }

        return valido;
    }

    private Map<String, String> formulario(
            HttpExchange ex
    ) throws IOException {

        byte[] bytes = ex.getRequestBody().readNBytes(8193);

        if (bytes.length > 8192) {
            throw new IllegalArgumentException(
                    "Solicitud demasiado grande"
            );
        }

        return leerParametros(
                new String(bytes, StandardCharsets.UTF_8)
        );
    }

    private Map<String, String> leerParametros(String texto) {

        Map<String, String> datos = new HashMap<>();

        if (texto == null || texto.isBlank()) {
            return datos;
        }

        for (String parte : texto.split("&")) {

            if (parte.isEmpty()) {
                continue;
            }

            String[] pareja = parte.split("=", 2);

            String clave = URLDecoder.decode(
                    pareja[0],
                    StandardCharsets.UTF_8
            );

            String valor = pareja.length == 2
                    ? URLDecoder.decode(
                    pareja[1],
                    StandardCharsets.UTF_8
            )
                    : "";

            datos.put(clave, valor);
        }

        return datos;
    }

    private int entero(
            Map<String, String> datos,
            String nombre
    ) {

        try {

            int numero = Integer.parseInt(
                    datos.getOrDefault(nombre, "")
            );

            if (numero <= 0) {
                throw new NumberFormatException();
            }

            return numero;

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Parametro invalido: " + nombre
            );
        }
    }

    private int enteroNoNegativo(
            Map<String, String> datos,
            String nombre
    ) {

        try {

            int numero = Integer.parseInt(
                    datos.getOrDefault(nombre, "")
            );

            if (numero < 0) {
                throw new NumberFormatException();
            }

            return numero;

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Parametro invalido: " + nombre
            );
        }
    }

    private String usuarioJson(
            WebAuthService.Sesion sesion
    ) {

        Usuario u = sesion.usuario();

        return "{"
                + "\"autenticado\":true,"
                + "\"id\":" + u.getId()
                + ",\"username\":\""
                + escapar(u.getUsername())
                + "\",\"region\":\""
                + escapar(u.getRegion())
                + "\",\"rol\":\""
                + escapar(sesion.rol())
                + "\""
                + "}";
    }

    private String cookieSesion(String token) {

        return "escrims_session=" + token
                + "; Path=/; HttpOnly; SameSite=Strict"
                + (produccion ? "; Secure" : "")
                + "; Max-Age=28800";
    }

    private String cookieExpirada() {

        return "escrims_session="
                + "; Path=/; HttpOnly; SameSite=Strict"
                + (produccion ? "; Secure" : "")
                + "; Max-Age=0";
    }

    private String escapar(String texto) {

        if (texto == null) {
            return "";
        }

        StringBuilder resultado = new StringBuilder();

        for (char c : texto.toCharArray()) {

            switch (c) {

                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");

                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");

                default -> {

                    if (c < 0x20) {

                        resultado.append(
                                String.format(
                                        "\\u%04x",
                                        (int) c
                                )
                        );

                    } else {

                        resultado.append(c);
                    }
                }
            }
        }

        return resultado.toString();
    }

    private void errorBaseDatos(
            HttpExchange ex,
            SQLException e
    ) throws IOException {

        System.err.println(
                "Error PostgreSQL: " + e.getSQLState()
        );

        error(
                ex,
                503,
                "No se pudo acceder a la base de datos"
        );
    }

    private void error(
            HttpExchange ex,
            int codigo,
            String mensaje
    ) throws IOException {

        enviarJson(
                ex,
                codigo,
                "{\"error\":\""
                        + escapar(mensaje)
                        + "\"}"
        );
    }

    private void enviarJson(
            HttpExchange ex,
            int codigo,
            String contenido
    ) throws IOException {

        enviar(
                ex,
                codigo,
                contenido.getBytes(StandardCharsets.UTF_8),
                "application/json; charset=UTF-8"
        );
    }

    private void enviar(
            HttpExchange ex,
            int codigo,
            byte[] contenido,
            String tipo
    ) throws IOException {

        ex.getResponseHeaders().set(
                "Content-Type",
                tipo
        );

        ex.getResponseHeaders().set(
                "Cache-Control",
                "no-store"
        );

        ex.getResponseHeaders().set(
                "X-Content-Type-Options",
                "nosniff"
        );

        ex.getResponseHeaders().set(
                "Referrer-Policy",
                "same-origin"
        );

        ex.sendResponseHeaders(codigo, contenido.length);

        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public void iniciar() {

        servidor.start();

        System.out.println("Servidor eScrims iniciado");

        System.out.println(
                "Web: " + (
                        produccion
                                ? origenPublico
                                : "http://localhost:" + puerto
                )
        );

        System.out.println(
                "Notificaciones de scrims habilitadas"
        );
    }

    public void detener() {
        servidor.stop(0);
    }

    public static void main(String[] args)
            throws IOException {

        String variable = System.getenv("PORT");

        int puerto = variable == null
                ? 9090
                : Integer.parseInt(variable);

        new ServidorWeb(puerto).iniciar();
    }
}
