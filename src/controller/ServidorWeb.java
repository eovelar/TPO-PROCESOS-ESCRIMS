
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
import java.util.List;

public class ServidorWeb {

    private final ScrimService scrimService;
    private final HttpServer servidor;
    private final int puerto;

    private final Path carpetaWeb = Path.of("src", "web");

    public ServidorWeb(int puerto) throws IOException {

        this.puerto = puerto;
        this.scrimService = new ScrimService();

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0
        );

        servidor.createContext("/api/scrims", this::listarScrims);
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

    private void servirPagina(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            enviarTexto(
                    exchange, 405,
                    "{\"error\":\"Metodo no permitido\"}",
                    "application/json; charset=UTF-8"
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

        byte[] contenido = Files.readAllBytes(rutaArchivo);

        enviarBytes(exchange, 200, contenido, tipo);
    }

    private void listarScrims(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestURI().getPath().equals("/api/scrims")) {
            enviarTexto(
                    exchange, 404,
                    "{\"error\":\"Ruta no encontrada\"}",
                    "application/json; charset=UTF-8"
            );
            return;
        }

        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            enviarTexto(
                    exchange, 405,
                    "{\"error\":\"Metodo no permitido\"}",
                    "application/json; charset=UTF-8"
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

        enviarTexto(
                exchange,
                200,
                json.toString(),
                "application/json; charset=UTF-8"
        );
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

        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        exchange.sendResponseHeaders(codigo, contenido.length);

        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public void iniciar() {

        servidor.start();

        System.out.println("Servidor eScrims iniciado");
        System.out.println("Web: http://localhost:" + puerto);
        System.out.println("API: http://localhost:" + puerto + "/api/scrims");
    }

    public void detener() {
        servidor.stop(0);
    }

    public static void main(String[] args) throws IOException {

        ServidorWeb servidor = new ServidorWeb(9090);
        servidor.iniciar();
    }
}
