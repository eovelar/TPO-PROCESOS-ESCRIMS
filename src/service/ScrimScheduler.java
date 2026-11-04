
package service;

import domain.Scrim;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScrimScheduler {

    private final ScrimService scrimService;
    private ScheduledExecutorService scheduler;

    public ScrimScheduler(ScrimService scrimService) {

        if (scrimService == null) {
            throw new IllegalArgumentException(
                    "El servicio de scrims es obligatorio"
            );
        }

        this.scrimService = scrimService;
    }

    public void verificarInicios() {

        List<Scrim> scrims = scrimService.listarScrims();
        LocalDateTime ahora = LocalDateTime.now();

        // revisa las partidas que ya llegaron a su horario
        for (Scrim scrim : scrims) {

            if (scrim == null) {
                continue;
            }

            if (!scrim.getEstadoActual().equals("Confirmado")) {
                continue;
            }

            if (ahora.isBefore(scrim.getFechaHora())) {
                continue;
            }

            try {
                scrim.iniciar();

                System.out.println(
                        "Scrim iniciado automaticamente: "
                                + scrim.getId()
                );

            } catch (RuntimeException e) {
                System.err.println(
                        "No se pudo iniciar el scrim "
                                + scrim.getId() + ": "
                                + e.getMessage()
                );
            }
        }
    }

    public void iniciarScheduler() {

        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();

        // verifica los inicios cada cinco segundos
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        verificarInicios();
                    } catch (RuntimeException e) {
                        System.err.println(
                                "Error al verificar scrims: "
                                        + e.getMessage()
                        );
                    }
                },
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    public void detenerScheduler() {

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
