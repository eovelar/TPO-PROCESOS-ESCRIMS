
package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import domain.Postulacion;
import domain.Scrim;
import domain.Usuario;
import service.PostulacionService;
import service.ScrimService;
import service.WebAuthService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorWeb {

    private record Registro(
            Postulacion postulacion,
            Scrim scrim,
            Usuario usuario
    ) {}

    private final ScrimService scrimService = new ScrimService();

    private final PostulacionService postulacionService =
            new PostulacionService();

    private final WebAuthService auth = new WebAuthService();

    private final Map<Integer, Registro> registros =
            new ConcurrentHashMap<>();

    private final AtomicInteger siguientePostulacion =
            new AtomicInteger(1);

    private final Path carpetaWeb = Path.of("src", "web");

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
                    "Falta configurar PUBLIC_ORIGIN en Render"
            );
        }

        String direccion = produccion
                ? "0.0.0.0"
                : "127.0.0.1";

        servidor = HttpServer.create(
                new InetSocketAddress(direccion, puerto),
                0
        );

        servidor.createContext(
                "/health",
                this::health
        );

        servidor.createContext(
                "/api/auth",
                this::manejarAuth
        );

        servidor.createContext(
                "/api/organizador/postulaciones",
                this::manejarOrganizador
        );

        servidor.createContext(
                "/api/postulaciones",
                this::manejarPostulaciones
        );

        servidor.createContext(
                "/api/scrims",
                this::listarScrims
        );

        servidor.createContext(
                "/",
                this::servirPagina
        );

        cargarDatosIniciales();
    }

    private void cargarDatosIniciales() {

        scrimService.agregarScrim(new Scrim(
                1, "Valorant", "5v5", "LAS",
                3, 7, 80,
                LocalDateTime.now().plusDays(2),
                60, 10
        ));

        scrimService.agregarScrim(new Scrim(
                2, "LoL", "5v5", "LAS",
                4, 9, 100,
                LocalDateTime.now().plusDays(3),
                90, 10
        ));

        scrimService.agregarScrim(new Scrim(
                3, "CS2", "5v5", "BR",
                2, 6, 60,
                LocalDateTime.now().plusDays(4),
                60, 10
        ));
    }

    private void health(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath().equals("/health")) {
            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (!ex.getRequestMethod().equals("GET")) {
            error(ex, 405, "Metodo no permitido");
            return;
        }

        enviarTexto(ex, 200, "READY", "text/plain");
    }

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

            if (sesion == null) {
                enviarJson(ex, 200, "{\"autenticado\":false}");
            } else {
                enviarJson(ex, 200, usuarioJson(sesion));
            }

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
                    ex, 200,
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

                int rango = entero(datos, "rango");

                acceso = auth.registrar(
                        datos.get("username"),
                        datos.get("email"),
                        datos.get("password"),
                        datos.get("region"),
                        rango
                );

            } else {

                String email = datos.get("email");

                if (produccion &&
                        email != null &&
                        email.trim().equalsIgnoreCase(
                                "organizador@escrims.local"
                        )) {

                    error(
                            ex, 403,
                            "La cuenta de demostracion no esta disponible"
                    );

                    return;
                }

                acceso = auth.iniciarSesion(
                        email,
                        datos.get("password")
                );
            }

            if (produccion &&
                    !acceso.sesion().rol().equals("JUGADOR")) {

                auth.cerrarSesion(acceso.token());

                error(ex, 403, "Rol no habilitado en esta version");
                return;
            }

            ex.getResponseHeaders().add(
                    "Set-Cookie",
                    cookieSesion(acceso.token())
            );

            enviarJson(
                    ex, 200,
                    usuarioJson(acceso.sesion())
            );

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            System.err.println(
                    "Error de autenticacion: " + e.getMessage()
            );

            error(
                    ex, 503,
                    "El servicio de cuentas no esta disponible"
            );
        }
    }

    private String cookieSesion(String token) {

        return "escrims_session=" + token
                + "; Path=/"
                + "; HttpOnly"
                + "; SameSite=Strict"
                + (produccion ? "; Secure" : "")
                + "; Max-Age=28800";
    }

    private String cookieExpirada() {

        return "escrims_session="
                + "; Path=/"
                + "; HttpOnly"
                + "; SameSite=Strict"
                + (produccion ? "; Secure" : "")
                + "; Max-Age=0";
    }

    private String usuarioJson(
            WebAuthService.Sesion sesion
    ) {

        Usuario usuario = sesion.usuario();

        return "{"
                + "\"autenticado\":true,"
                + "\"id\":" + usuario.getId() + ","
                + "\"username\":\""
                + escapar(usuario.getUsername()) + "\","
                + "\"region\":\""
                + escapar(usuario.getRegion()) + "\","
                + "\"rol\":\""
                + escapar(sesion.rol()) + "\""
                + "}";
    }

    private void manejarPostulaciones(
            HttpExchange ex
    ) throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/postulaciones")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        WebAuthService.Sesion sesion = requerirSesion(ex);

        if (sesion == null) {
            return;
        }

        switch (ex.getRequestMethod()) {

            case "GET" -> enviarJson(
                    ex,
                    200,
                    construirListado(
                            sesion.usuario().getId(),
                            false
                    )
            );

            case "POST" -> registrarPostulacion(
                    ex,
                    sesion.usuario()
            );

            default -> error(
                    ex, 405,
                    "Metodo no permitido"
            );
        }
    }

    private synchronized void registrarPostulacion(
            HttpExchange ex,
            Usuario usuario
    ) throws IOException {

        if (!validarOrigen(ex) || !validarFormulario(ex)) {
            return;
        }

        try {
            int scrimId = entero(
                    formulario(ex),
                    "scrimId"
            );

            Scrim scrim = scrimService.buscarPorId(scrimId);

            if (scrim == null) {
                error(ex, 404, "Partida no encontrada");
                return;
            }

            if (!scrim.getEstadoActual()
                    .equals("Buscando jugadores") ||
                    scrim.getCuposDisponibles() <= 0) {

                error(
                        ex, 409,
                        "La partida no admite postulaciones"
                );

                return;
            }

            Postulacion postulacion =
                    postulacionService.crearPostulacion(
                            siguientePostulacion.getAndIncrement(),
                            usuario,
                            scrim,
                            "Flexible"
                    );

            registros.put(
                    postulacion.getId(),
                    new Registro(postulacion, scrim, usuario)
            );

            String respuesta = "{"
                    + "\"mensaje\":\"Postulacion registrada\","
                    + "\"id\":" + postulacion.getId() + ","
                    + "\"scrimId\":" + scrim.getId() + ","
                    + "\"estado\":\""
                    + escapar(postulacion.getEstado()) + "\""
                    + "}";

            enviarJson(ex, 201, respuesta);

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            error(ex, 409, e.getMessage());
        }
    }

    private void manejarOrganizador(
            HttpExchange ex
    ) throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/organizador/postulaciones")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (produccion) {

            error(
                    ex, 403,
                    "Panel de organizador no habilitado en produccion"
            );

            return;
        }

        WebAuthService.Sesion sesion = requerirSesion(ex);

        if (sesion == null) {
            return;
        }

        if (!sesion.rol().equals("ORGANIZADOR")) {

            error(ex, 403, "No tenes permisos");
            return;
        }

        switch (ex.getRequestMethod()) {

            case "GET" -> enviarJson(
                    ex,
                    200,
                    construirListado(null, true)
            );

            case "POST" -> gestionarPostulacion(ex);

            default -> error(
                    ex, 405,
                    "Metodo no permitido"
            );
        }
    }

    private synchronized void gestionarPostulacion(
            HttpExchange ex
    ) throws IOException {

        if (!validarOrigen(ex) || !validarFormulario(ex)) {
            return;
        }

        try {

            Map<String, String> datos = formulario(ex);

            int id = entero(datos, "postulacionId");
            String accion = datos.get("accion");

            if (!"aceptar".equals(accion) &&
                    !"rechazar".equals(accion)) {

                throw new IllegalArgumentException(
                        "Accion invalida"
                );
            }

            Registro registro = registros.get(id);

            if (registro == null) {

                error(ex, 404, "Postulacion no encontrada");
                return;
            }

            Postulacion postulacion = registro.postulacion();

            if (!postulacion.getEstado().equals("Pendiente")) {

                error(ex, 409, "La solicitud ya fue resuelta");
                return;
            }

            if (accion.equals("aceptar")) {

                postulacionService.aceptarPostulacion(
                        postulacion
                );

            } else {

                postulacionService.rechazarPostulacion(
                        postulacion
                );
            }

            enviarJson(
                    ex, 200,
                    "{"
                            + "\"mensaje\":\"Solicitud procesada\","
                            + "\"id\":" + postulacion.getId() + ","
                            + "\"estado\":\""
                            + escapar(postulacion.getEstado())
                            + "\""
                            + "}"
            );

        } catch (IllegalArgumentException e) {

            error(ex, 400, e.getMessage());

        } catch (IllegalStateException e) {

            error(ex, 409, e.getMessage());
        }
    }

    private String construirListado(
            Integer usuarioId,
            boolean incluirUsuario
    ) {

        List<Registro> lista =
                new ArrayList<>(registros.values());

        lista.sort(
                Comparator.comparingInt(
                        r -> r.postulacion().getId()
                )
        );

        StringBuilder json = new StringBuilder("[");
        boolean primero = true;

        for (Registro registro : lista) {

            Usuario usuario = registro.usuario();

            if (usuarioId != null &&
                    usuario.getId() != usuarioId) {
                continue;
            }

            if (!primero) {
                json.append(",");
            }

            primero = false;

            Postulacion postulacion = registro.postulacion();
            Scrim scrim = registro.scrim();

            json.append("{")
                    .append("\"id\":")
                    .append(postulacion.getId())

                    .append(",\"scrimId\":")
                    .append(scrim.getId())

                    .append(",\"juego\":\"")
                    .append(escapar(scrim.getJuego()))

                    .append("\",\"formato\":\"")
                    .append(escapar(scrim.getFormato()))

                    .append("\",\"region\":\"")
                    .append(escapar(scrim.getRegion()))

                    .append("\",\"fecha\":\"")
                    .append(scrim.getFechaHora())

                    .append("\",\"estado\":\"")
                    .append(escapar(postulacion.getEstado()))
                    .append("\"");

            if (incluirUsuario) {

                json.append(",\"usuario\":\"")
                        .append(escapar(usuario.getUsername()))
                        .append("\"");
            }

            json.append("}");
        }

        return json.append("]").toString();
    }

    private void listarScrims(
            HttpExchange ex
    ) throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/scrims")) {

            error(ex, 404, "Ruta no encontrada");
            return;
        }

        if (!ex.getRequestMethod().equals("GET")) {

            error(ex, 405, "Metodo no permitido");
            return;
        }

        List<Scrim> lista = scrimService.listarScrims();

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < lista.size(); i++) {

            Scrim scrim = lista.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"id\":")
                    .append(scrim.getId())

                    .append(",\"juego\":\"")
                    .append(escapar(scrim.getJuego()))

                    .append("\",\"formato\":\"")
                    .append(escapar(scrim.getFormato()))

                    .append("\",\"region\":\"")
                    .append(escapar(scrim.getRegion()))

                    .append("\",\"fecha\":\"")
                    .append(scrim.getFechaHora())

                    .append("\",\"estado\":\"")
                    .append(escapar(scrim.getEstadoActual()))

                    .append("\",\"cuposDisponibles\":")
                    .append(scrim.getCuposDisponibles())

                    .append("}");
        }

        enviarJson(ex, 200, json.append("]").toString());
    }

    private void servirPagina(
            HttpExchange ex
    ) throws IOException {

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

        String contenido = Files.readString(
                ruta,
                StandardCharsets.UTF_8
        );

        byte[] bytes = contenido.getBytes(
                StandardCharsets.UTF_8
        );

        enviar(ex, 200, bytes, tipo);
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
            error(ex, 401, "Inicia sesion para continuar");
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

    private boolean validarOrigen(
            HttpExchange ex
    ) throws IOException {

        String origen = ex.getRequestHeaders()
                .getFirst("Origin");

        if (produccion) {

            if (origen == null ||
                    !origen.equals(origenPublico)) {

                error(ex, 403, "Origen no permitido");
                return false;
            }

        } else {

            if (origen != null &&
                    !origen.equals(
                            "http://localhost:" + puerto
                    ) &&
                    !origen.equals(
                            "http://127.0.0.1:" + puerto
                    )) {

                error(ex, 403, "Origen no permitido");
                return false;
            }
        }

        return true;
    }

    private boolean validarFormulario(
            HttpExchange ex
    ) throws IOException {

        String tipo = ex.getRequestHeaders()
                .getFirst("Content-Type");

        if (tipo == null ||
                !tipo.toLowerCase(Locale.ROOT).startsWith(
                        "application/x-www-form-urlencoded"
                )) {

            error(ex, 415, "Formato no soportado");
            return false;
        }

        return true;
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

        String cuerpo = new String(
                bytes,
                StandardCharsets.UTF_8
        );

        Map<String, String> datos = new HashMap<>();

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
                                String.format("\\u%04x", (int) c)
                        );
                    } else {
                        resultado.append(c);
                    }
                }
            }
        }

        return resultado.toString();
    }

    private void error(
            HttpExchange ex,
            int codigo,
            String mensaje
    ) throws IOException {

        enviarJson(
                ex,
                codigo,
                "{\"error\":\"" + escapar(mensaje) + "\"}"
        );
    }

    private void enviarJson(
            HttpExchange ex,
            int codigo,
            String contenido
    ) throws IOException {

        enviarTexto(
                ex,
                codigo,
                contenido,
                "application/json; charset=UTF-8"
        );
    }

    private void enviarTexto(
            HttpExchange ex,
            int codigo,
            String contenido,
            String tipo
    ) throws IOException {

        enviar(
                ex,
                codigo,
                contenido.getBytes(StandardCharsets.UTF_8),
                tipo
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
        System.out.println("Puerto: " + puerto);
        System.out.println(
                produccion
                        ? "Modo Render"
                        : "Modo local"
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

        ServidorWeb servidor = new ServidorWeb(puerto);

        servidor.iniciar();
    }
}
