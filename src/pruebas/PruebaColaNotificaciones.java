
package pruebas;

import observer.EventoScrim;
import service.ColaNotificaciones;

import java.util.concurrent.atomic.AtomicInteger;

public class PruebaColaNotificaciones {

    public static void main(String[] args) {

        AtomicInteger intentos = new AtomicInteger();

        ColaNotificaciones cola = new ColaNotificaciones(
                evento -> {

                    int intento = intentos.incrementAndGet();

                    if (intento < 3) {
                        throw new IllegalStateException(
                                "Fallo simulado en el envio"
                        );
                    }

                    System.out.println(
                            "Aviso recibido: scrim #"
                                    + evento.scrimId()
                                    + " - " + evento.estadoNuevo()
                    );
                },
                3
        );

        EventoScrim evento = new EventoScrim(
                1,
                "Valorant",
                "Buscando jugadores",
                "Lobby armado"
        );

        cola.agregar(evento);

        comprobar(
                cola.cantidadPendientes() == 1,
                "El aviso debe quedar pendiente"
        );

        // primer intento
        cola.procesarPendientes();

        comprobar(
                cola.cantidadPendientes() == 1,
                "El aviso debe continuar pendiente"
        );

        // segundo intento
        cola.procesarPendientes();

        comprobar(
                cola.cantidadPendientes() == 1,
                "El segundo fallo debe permitir otro intento"
        );

        // tercer intento
        cola.procesarPendientes();

        comprobar(
                cola.cantidadPendientes() == 0,
                "La cola debe quedar vacia"
        );

        comprobar(
                cola.cantidadEnviadas() == 1,
                "Debe registrarse un envio exitoso"
        );

        comprobar(
                cola.cantidadFallidas() == 0,
                "No debe haber avisos fallidos"
        );

        comprobar(
                intentos.get() == 3,
                "El aviso debe haberse intentado tres veces"
        );

        System.out.println(
                "\nPrueba de reintentos finalizada correctamente"
        );

        probarFalloDefinitivo();
    }

    private static void probarFalloDefinitivo() {

        ColaNotificaciones cola = new ColaNotificaciones(
                evento -> {
                    throw new IllegalStateException(
                            "Servicio no disponible"
                    );
                },
                2
        );

        cola.agregar(
                new EventoScrim(
                        2,
                        "LoL",
                        "Buscando jugadores",
                        "Lobby armado"
                )
        );

        cola.procesarPendientes();
        cola.procesarPendientes();

        comprobar(
                cola.cantidadPendientes() == 0,
                "El aviso debe salir de la cola al agotar los intentos"
        );

        comprobar(
                cola.cantidadEnviadas() == 0,
                "No debe haber envios exitosos"
        );

        comprobar(
                cola.cantidadFallidas() == 1,
                "Debe registrarse un aviso fallido"
        );

        System.out.println(
                "Prueba de fallo definitivo finalizada correctamente"
        );
    }

    private static void comprobar(
            boolean condicion,
            String mensaje
    ) {

        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
