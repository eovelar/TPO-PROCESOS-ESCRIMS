
package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import domain.Usuario;
import persistence.PostulacionRepository;
import persistence.ScrimRepository;
import service.WebAuthService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServidorWeb {

    private final WebAuthService auth = new WebAuthService();

    private final ScrimRepository scrims =
            new ScrimRepository();

    private final PostulacionRepository postulaciones =
            new PostulacionRepository();

    private final Path carpetaWeb =
            Path.of("src", "web");

    private final HttpServer servidor;
    private final int puerto;
    private final boolean produccion;
    private final String origenPublico;

    public ServidorWeb(int puerto) throws IOException {

        this.puerto = puerto;

        produccion = System.getenv("PORT") != null;
        origenPublico = System.getenv("PUBLIC_ORIGIN");

        if (produccion &&
                (origenPublico == null || origenPublico.isBlank())) {

            throw new IllegalStateException(
                    "Falta configurar PUBLIC_ORIGIN"
            );
        }

        String direccion = produccion
                ? "0.0.0.0"
                : "127.0.0.1";

        servidor = HttpServer.create(
                new InetSocketAddress(direccion, puerto),
                0
        );

        servidor.createContext("/health", this::health);

        servidor.createContext(
                "/api/auth",
                this::manejarAuth
        );

        servidor.createContext(
                "/api/scrims",
                this::listarScrims
        );

        servidor.createContext(
                "/api/postulaciones",
                this::manejarPostulaciones
        );

        servidor.createContext(
                "/api/organizador/postulaciones",
                this::manejarOrganizador
        );

        servidor.createContext("/", this::servirPagina);
    }

    private void health(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath().equals("/health")) {

            error(ex, 404, "Ruta no encontrada");

        } else if (!ex.getRequestMethod().equals("GET")) {

            error(ex, 405, "Metodo no permitido");

        } else {

            enviar(
                    ex,
                    200,
                    "READY".getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=UTF-8"
            );
        }
    }

    // administra el registro, login y cierre de sesión
    private void manejarAuth(HttpExchange ex)
            throws IOException {

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

                String email = datos.get("email");

                // la cuenta de prueba solo se utiliza localmente
                if (produccion &&
                        email != null &&
                        email.trim().equalsIgnoreCase(
                                "organizador@escrims.local"
                        )) {

                    error(
                            ex,
                            403,
                            "Cuenta de demostracion no disponible"
                    );

                    return;
                }

                acceso = auth.iniciarSesion(
                        email,
                        datos.get("password")
                );
            }

            if (produccion &&
                    !"JUGADOR".equals(acceso.sesion().rol())) {

                auth.cerrarSesion(acceso.token());

                error(
                        ex,
                        403,
                        "Rol no habilitado en esta version"
                );

                return;
            }

            // guarda la sesión en una cookie protegida
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
                    "Error en autenticacion: "
                            + e.getClass().getSimpleName()
            );

            error(
                    ex,
                    503,
                    "El servicio de cuentas no esta disponible"
            );
        }
    }

    // obtiene las partidas directamente desde supabase
    private void listarScrims(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/scrims")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (!ex.getRequestMethod().equals("GET")) {

            error(ex, 405, "Metodo no permitido");
            return;
        }

        try {

            List<ScrimRepository.ScrimGuardado> lista =
                    scrims.listar();

            StringBuilder json = new StringBuilder("[");

            for (ScrimRepository.ScrimGuardado s : lista) {

                if (json.length() > 1) {
                    json.append(',');
                }

                json.append('{')
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

                        .append('}');
            }

            enviarJson(
                    ex,
                    200,
                    json.append(']').toString()
            );

        } catch (SQLException e) {

            errorBaseDatos(ex, e);
        }
    }

    // permite consultar y enviar postulaciones
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

                    List<PostulacionRepository.PostulacionGuardada> lista =
                            postulaciones.listarPorUsuario(
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

                    // la base de datos genera el id de la solicitud
                    int id = postulaciones.guardar(
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

    // el organizador puede revisar y resolver solicitudes
    private void manejarOrganizador(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/organizador/postulaciones")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (produccion) {

            error(
                    ex,
                    403,
                    "Panel de organizador no habilitado en produccion"
            );

            return;
        }

        WebAuthService.Sesion sesion = requerirSesion(ex);

        if (sesion == null) {
            return;
        }

        if (!"ORGANIZADOR".equals(sesion.rol())) {

            error(ex, 403, "No tenes permisos");
            return;
        }

        try {

            switch (ex.getRequestMethod()) {

                case "GET" -> {

                    List<PostulacionRepository.PostulacionGuardada> lista =
                            postulaciones.listarTodas();

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

                    int id = entero(
                            datos,
                            "postulacionId"
                    );

                    // acepta o rechaza en una transacción
                    String estado = postulaciones.procesar(
                            id,
                            datos.get("accion")
                    );

                    enviarJson(
                            ex,
                            200,
                            "{"
                                    + "\"mensaje\":\"Solicitud procesada\","
                                    + "\"id\":" + id
                                    + ",\"estado\":\"" + estado + "\""
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

    // transforma las postulaciones al formato que usa app.js
    private String postulacionesJson(
            List<PostulacionRepository.PostulacionGuardada> lista,
            boolean incluirUsuario
    ) {

        StringBuilder json = new StringBuilder("[");

        for (PostulacionRepository.PostulacionGuardada p : lista) {

            if (json.length() > 1) {
                json.append(',');
            }

            json.append('{')
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
                    .append('"');

            if (incluirUsuario) {

                json.append(",\"usuario\":\"")
                        .append(escapar(p.usuario()))
                        .append('"');
            }

            json.append('}');
        }

        return json.append(']').toString();
    }

    // sirve los archivos de la interfaz sin modificarlos
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

    // valida que las solicitudes provengan de nuestra web
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
                tipo.toLowerCase(Locale.ROOT)
                        .startsWith(
                                "application/x-www-form-urlencoded"
                        );

        if (!valido) {
            error(ex, 415, "Formato no soportado");
        }

        return valido;
    }

    // lee los datos enviados desde los formularios
    private Map<String, String> formulario(HttpExchange ex)
            throws IOException {

        byte[] bytes = ex.getRequestBody()
                .readNBytes(8193);

        if (bytes.length > 8192) {

            throw new IllegalArgumentException(
                    "Solicitud demasiado grande"
            );
        }

        Map<String, String> datos = new HashMap<>();

        String cuerpo = new String(
                bytes,
                StandardCharsets.UTF_8
        );

        for (String parte : cuerpo.split("&")) {

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

    private String usuarioJson(
            WebAuthService.Sesion sesion
    ) {

        Usuario u = sesion.usuario();

        return "{\"autenticado\":true,\"id\":"
                + u.getId()

                + ",\"username\":\""
                + escapar(u.getUsername())

                + "\",\"region\":\""
                + escapar(u.getRegion())

                + "\",\"rol\":\""
                + escapar(sesion.rol())

                + "\"}";
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

    // escapa caracteres especiales antes de generar el json
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
                "Error de PostgreSQL: " + e.getSQLState()
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

        ex.sendResponseHeaders(
                codigo,
                contenido.length
        );

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
