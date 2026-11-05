
package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import domain.Scrim;
import service.ScrimService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ServidorDemo {

    private final HttpServer servidor;
    private final ScrimService scrimService;
    private final Path carpetaWeb = Path.of("src", "web");

    public ServidorDemo(int puerto) throws IOException {

        scrimService = new ScrimService();

        servidor = HttpServer.create(
                new InetSocketAddress("0.0.0.0", puerto), 0
        );

        servidor.createContext(
                "/api/scrims", this::listarScrims
        );

        servidor.createContext(
                "/api/auth", this::authDemo
        );

        servidor.createContext(
                "/api/postulaciones", this::bloquearOperaciones
        );

        servidor.createContext(
                "/api/organizador", this::bloquearOperaciones
        );

        servidor.createContext(
                "/health", this::health
        );

        servidor.createContext(
                "/", this::servirPagina
        );

        cargarDatos();
    }

    private void cargarDatos() {

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

    private void listarScrims(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestURI().getPath()
                .equals("/api/scrims")) {
            responder(ex, 404,
                    "{\"error\":\"Ruta no encontrada\"}",
                    "application/json");
            return;
        }

        if (!ex.getRequestMethod().equals("GET")) {
            responder(ex, 405,
                    "{\"error\":\"Metodo no permitido\"}",
                    "application/json");
            return;
        }

        StringBuilder json = new StringBuilder("[");

        var scrims = scrimService.listarScrims();

        for (int i = 0; i < scrims.size(); i++) {

            Scrim s = scrims.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"id\":").append(s.getId())
                    .append(",\"juego\":\"")
                    .append(escapar(s.getJuego()))
                    .append("\",\"formato\":\"")
                    .append(escapar(s.getFormato()))
                    .append("\",\"region\":\"")
                    .append(escapar(s.getRegion()))
                    .append("\",\"fecha\":\"")
                    .append(s.getFechaHora())
                    .append("\",\"estado\":\"")
                    .append(escapar(s.getEstadoActual()))
                    .append("\",\"cuposDisponibles\":")
                    .append(s.getCuposDisponibles())
                    .append("}");
        }

        json.append("]");

        responder(ex, 200, json.toString(),
                "application/json");
    }

    private void authDemo(HttpExchange ex)
            throws IOException {

        if (ex.getRequestURI().getPath()
                .equals("/api/auth/me")
                && ex.getRequestMethod().equals("GET")) {

            responder(ex, 200,
                    "{\"autenticado\":false}",
                    "application/json");
            return;
        }

        bloquearOperaciones(ex);
    }

    private void bloquearOperaciones(HttpExchange ex)
            throws IOException {

        responder(ex, 403,
                "{\"error\":\"Funcion deshabilitada en la demo publica\"}",
                "application/json");
    }

    private void health(HttpExchange ex)
            throws IOException {

        responder(ex, 200, "OK", "text/plain");
    }

    private void servirPagina(HttpExchange ex)
            throws IOException {

        if (!ex.getRequestMethod().equals("GET")) {
            responder(ex, 405, "Metodo no permitido",
                    "text/plain");
            return;
        }

        String archivo;
        String tipo;

        switch (ex.getRequestURI().getPath()) {
            case "/" -> {
                archivo = "index.html";
                tipo = "text/html";
            }

            case "/styles.css" -> {
                archivo = "styles.css";
                tipo = "text/css";
            }

            case "/app.js" -> {
                archivo = "app.js";
                tipo = "text/javascript";
            }

            default -> {
                responder(ex, 404, "Pagina no encontrada",
                        "text/plain");
                return;
            }
        }

        Path ruta = carpetaWeb.resolve(archivo);

        if (!Files.isRegularFile(ruta)) {
            responder(ex, 404, "Archivo no encontrado",
                    "text/plain");
            return;
        }

        byte[] contenido = Files.readAllBytes(ruta);

        ex.getResponseHeaders().set(
                "Content-Type", tipo + "; charset=UTF-8"
        );

        ex.sendResponseHeaders(200, contenido.length);

        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(contenido);
        }
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void responder(
            HttpExchange ex,
            int codigo,
            String contenido,
            String tipo
    ) throws IOException {

        byte[] bytes = contenido.getBytes(
                StandardCharsets.UTF_8
        );

        ex.getResponseHeaders().set(
                "Content-Type", tipo + "; charset=UTF-8"
        );

        ex.sendResponseHeaders(codigo, bytes.length);

        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    public void iniciar() {
        servidor.start();
        System.out.println("Demo eScrims iniciada");
    }

    public static void main(String[] args)
            throws IOException {

        String variable = System.getenv("PORT");

        int puerto = variable == null
                ? 9091
                : Integer.parseInt(variable);

        ServidorDemo demo = new ServidorDemo(puerto);
        demo.iniciar();

        System.out.println("Puerto: " + puerto);
    }
}
