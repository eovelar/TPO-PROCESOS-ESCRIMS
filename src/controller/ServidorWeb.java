
package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import domain.Postulacion;
import domain.Scrim;
import domain.Usuario;
import service.PostulacionService;
import service.ScrimService;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorWeb {

    private final ScrimService scrimService;
    private final PostulacionService postulacionService;
    private final HttpServer servidor;
    private final int puerto;

    private final Usuario jugadorDemo;

    private final AtomicInteger siguientePostulacionId =
            new AtomicInteger(1);

    private final Map<Integer, RegistroPostulacion> registros =
            new ConcurrentHashMap<>();

    private final Path carpetaWeb = Path.of("src", "web");

    private record RegistroPostulacion(
            Postulacion postulacion,
            Scrim scrim
    ) {}

    public ServidorWeb(int puerto) throws IOException {

        this.puerto = puerto;

        scrimService = new ScrimService();
        postulacionService = new PostulacionService();

        jugadorDemo = new Usuario(
                1000,
                "JugadorDemo",
                "demo@escrims.com",
                "demo",
                "LAS",
                5
        );

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0
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

        servidor.createContext("/", this::servirPagina);

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

    private void manejarPostulaciones(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestURI().getPath()
                .equals("/api/postulaciones")) {

            enviarError(exchange, 404, "Ruta no encontrada");
            return;
        }

        switch (exchange.getRequestMethod()) {

            case "GET" -> consultarPostulaciones(exchange);

            case "POST" -> registrarPostulacion(exchange);

            default -> {
                exchange.getResponseHeaders().set(
                        "Allow", "GET, POST"
                );

                enviarError(
                        exchange, 405, "Metodo no permitido"
                );
            }
        }
    }

    private synchronized void registrarPostulacion(
            HttpExchange exchange
    ) throws IOException {

        if (!esFormulario(exchange)) {
            enviarError(
                    exchange, 415, "Formato no soportado"
            );
            return;
        }

        try {
            String cuerpo = leerCuerpo(exchange);
            int scrimId = leerEntero(cuerpo, "scrimId");

            Scrim scrim = scrimService.buscarPorId(scrimId);

            if (scrim == null) {
                enviarError(
                        exchange, 404, "Partida no encontrada"
                );
                return;
            }

            if (!scrim.getEstadoActual()
                    .equals("Buscando jugadores")
                    || scrim.getCuposDisponibles() <= 0) {

                enviarError(
                        exchange, 409,
                        "La partida no admite postulaciones"
                );
                return;
            }

            Postulacion postulacion =
                    postulacionService.crearPostulacion(
                            siguientePostulacionId.getAndIncrement(),
                            jugadorDemo,
                            scrim,
                            "Flexible"
                    );

            registros.put(
                    postulacion.getId(),
                    new RegistroPostulacion(postulacion, scrim)
            );

            String respuesta = "{"
                    + "\"mensaje\":\"Postulacion registrada\","
                    + "\"id\":" + postulacion.getId() + ","
                    + "\"scrimId\":" + scrim.getId() + ","
                    + "\"usuario\":\""
                    + escapar(jugadorDemo.getUsername()) + "\","
                    + "\"estado\":\""
                    + escapar(postulacion.getEstado()) + "\""
                    + "}";

            enviarJson(exchange, 201, respuesta);

        } catch (IllegalArgumentException e) {
            enviarError(exchange, 400, e.getMessage());

        } catch (IllegalStateException e) {
            enviarError(exchange, 409, e.getMessage());
        }
    }

    private void consultarPostulaciones(
            HttpExchange exchange
    ) throws IOException {

        enviarJson(
                exchange,
                200,
                construirListado(false)
        );
    }

    private void manejarOrganizador(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestURI().getPath()
                .equals("/api/organizador/postulaciones")) {

            enviarError(exchange, 404, "Ruta no encontrada");
            return;
        }

        switch (exchange.getRequestMethod()) {

            case "GET" -> enviarJson(
                    exchange,
                    200,
                    construirListado(true)
            );

            case "POST" -> gestionarPostulacion(exchange);

            default -> {
                exchange.getResponseHeaders().set(
                        "Allow", "GET, POST"
                );

                enviarError(
                        exchange, 405, "Metodo no permitido"
                );
            }
        }
    }

    private synchronized void gestionarPostulacion(
            HttpExchange exchange
    ) throws IOException {

        if (!esFormulario(exchange)) {
            enviarError(
                    exchange, 415, "Formato no soportado"
            );
            return;
        }

        try {
            String cuerpo = leerCuerpo(exchange);

            int postulacionId = leerEntero(
                    cuerpo, "postulacionId"
            );

            String accion = obtenerParametro(
                    cuerpo, "accion"
            );

            if (accion == null ||
                    (!accion.equals("aceptar")
                            && !accion.equals("rechazar"))) {

                throw new IllegalArgumentException(
                        "La accion debe ser aceptar o rechazar"
                );
            }

            RegistroPostulacion registro =
                    registros.get(postulacionId);

            if (registro == null) {
                enviarError(
                        exchange, 404,
                        "Postulacion no encontrada"
                );
                return;
            }

            Postulacion postulacion = registro.postulacion();

            if (!postulacion.getEstado().equals("Pendiente")) {
                enviarError(
                        exchange, 409,
                        "La postulacion ya fue resuelta"
                );
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

            String respuesta = "{"
                    + "\"mensaje\":\"Solicitud procesada\","
                    + "\"id\":" + postulacion.getId() + ","
                    + "\"scrimId\":"
                    + registro.scrim().getId() + ","
                    + "\"estado\":\""
                    + escapar(postulacion.getEstado()) + "\""
                    + "}";

            enviarJson(exchange, 200, respuesta);

        } catch (IllegalArgumentException e) {
            enviarError(exchange, 400, e.getMessage());

        } catch (IllegalStateException e) {
            enviarError(exchange, 409, e.getMessage());
        }
    }

    private String construirListado(
            boolean incluirUsuario
    ) {

        List<RegistroPostulacion> lista =
                new ArrayList<>(registros.values());

        lista.sort(
                Comparator.comparingInt(
                        registro -> registro.postulacion().getId()
                )
        );

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < lista.size(); i++) {

            RegistroPostulacion registro = lista.get(i);
            Postulacion postulacion = registro.postulacion();
            Scrim scrim = registro.scrim();

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"id\":")
                    .append(postulacion.getId())
                    .append(",")

                    .append("\"scrimId\":")
                    .append(scrim.getId())
                    .append(",")

                    .append("\"juego\":\"")
                    .append(escapar(scrim.getJuego()))
                    .append("\",")

                    .append("\"formato\":\"")
                    .append(escapar(scrim.getFormato()))
                    .append("\",")

                    .append("\"region\":\"")
                    .append(escapar(scrim.getRegion()))
                    .append("\",")

                    .append("\"fecha\":\"")
                    .append(scrim.getFechaHora())
                    .append("\",")

                    .append("\"estado\":\"")
                    .append(escapar(postulacion.getEstado()))
                    .append("\"");

            if (incluirUsuario) {
                json.append(",\"usuario\":\"")
                        .append(escapar(jugadorDemo.getUsername()))
                        .append("\"");
            }

            json.append("}");
        }

        json.append("]");

        return json.toString();
    }

    private boolean esFormulario(
            HttpExchange exchange
    ) {

        String tipo = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        return tipo != null
                && tipo.toLowerCase().startsWith(
                "application/x-www-form-urlencoded"
        );
    }

    private String leerCuerpo(
            HttpExchange exchange
    ) throws IOException {

        byte[] datos = exchange.getRequestBody()
                .readNBytes(4097);

        if (datos.length > 4096) {
            throw new IllegalArgumentException(
                    "Solicitud demasiado grande"
            );
        }

        return new String(
                datos, StandardCharsets.UTF_8
        );
    }

    private int leerEntero(
            String cuerpo,
            String nombre
    ) {

        String valor = obtenerParametro(cuerpo, nombre);

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Falta el parametro: " + nombre
            );
        }

        try {
            int numero = Integer.parseInt(valor);

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

    private String obtenerParametro(
            String cuerpo,
            String nombre
    ) {

        for (String parte : cuerpo.split("&")) {

            String[] datos = parte.split("=", 2);

            String clave = URLDecoder.decode(
                    datos[0], StandardCharsets.UTF_8
            );

            if (clave.equals(nombre)) {

                return datos.length > 1
                        ? URLDecoder.decode(
                        datos[1], StandardCharsets.UTF_8
                )
                        : "";
            }
        }

        return null;
    }

    private void servirPagina(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {

            exchange.getResponseHeaders().set("Allow", "GET");

            enviarError(
                    exchange, 405, "Metodo no permitido"
            );
            return;
        }

        String ruta = exchange.getRequestURI().getPath();
        String archivo;
        String tipo;

        switch (ruta) {

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
                enviarTexto(
                        exchange, 404,
                        "Pagina no encontrada",
                        "text/plain; charset=UTF-8"
                );
                return;
            }
        }

        Path rutaArchivo = carpetaWeb.resolve(archivo);

        if (!Files.isRegularFile(rutaArchivo)) {

            enviarTexto(
                    exchange, 404,
                    "No se encontro el archivo: " + archivo,
                    "text/plain; charset=UTF-8"
            );
            return;
        }

        enviarBytes(
                exchange,
                200,
                Files.readAllBytes(rutaArchivo),
                tipo
        );
    }

    private void listarScrims(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestURI().getPath()
                .equals("/api/scrims")) {

            enviarError(exchange, 404, "Ruta no encontrada");
            return;
        }

        if (!exchange.getRequestMethod().equals("GET")) {

            exchange.getResponseHeaders().set(
                    "Allow", "GET"
            );

            enviarError(
                    exchange, 405, "Metodo no permitido"
            );
            return;
        }

        List<Scrim> scrims = scrimService.listarScrims();

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < scrims.size(); i++) {

            Scrim scrim = scrims.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"id\":")
                    .append(scrim.getId())
                    .append(",")

                    .append("\"juego\":\"")
                    .append(escapar(scrim.getJuego()))
                    .append("\",")

                    .append("\"formato\":\"")
                    .append(escapar(scrim.getFormato()))
                    .append("\",")

                    .append("\"region\":\"")
                    .append(escapar(scrim.getRegion()))
                    .append("\",")

                    .append("\"fecha\":\"")
                    .append(scrim.getFechaHora())
                    .append("\",")

                    .append("\"estado\":\"")
                    .append(escapar(scrim.getEstadoActual()))
                    .append("\",")

                    .append("\"cuposDisponibles\":")
                    .append(scrim.getCuposDisponibles())
                    .append("}");
        }

        json.append("]");

        enviarJson(exchange, 200, json.toString());
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

    private void enviarError(
            HttpExchange exchange,
            int codigo,
            String mensaje
    ) throws IOException {

        enviarJson(
                exchange,
                codigo,
                "{\"error\":\"" + escapar(mensaje) + "\"}"
        );
    }

    private void enviarJson(
            HttpExchange exchange,
            int codigo,
            String contenido
    ) throws IOException {

        enviarTexto(
                exchange,
                codigo,
                contenido,
                "application/json; charset=UTF-8"
        );
    }

    private void enviarTexto(
            HttpExchange exchange,
            int codigo,
            String contenido,
            String tipo
    ) throws IOException {

        enviarBytes(
                exchange,
                codigo,
                contenido.getBytes(StandardCharsets.UTF_8),
                tipo
        );
    }

    private void enviarBytes(
            HttpExchange exchange,
            int codigo,
            byte[] contenido,
            String tipo
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", tipo
        );

        exchange.getResponseHeaders().set(
                "Cache-Control", "no-store"
        );

        exchange.sendResponseHeaders(
                codigo, contenido.length
        );

        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public void iniciar() {

        servidor.start();

        System.out.println("Servidor eScrims iniciado");
        System.out.println("Web: http://localhost:" + puerto);
        System.out.println(
                "Partidas: http://localhost:"
                        + puerto + "/api/scrims"
        );
        System.out.println(
                "Mis postulaciones: http://localhost:"
                        + puerto + "/api/postulaciones"
        );
        System.out.println(
                "Organizador: http://localhost:"
                        + puerto + "/api/organizador/postulaciones"
        );
    }

    public void detener() {
        servidor.stop(0);
    }

    public static void main(String[] args) throws IOException {

        ServidorWeb servidor = new ServidorWeb(9090);
        servidor.iniciar();
    }
}
