
package domain;

import state.ScrimContext;
import java.time.LocalDateTime;

public class Scrim {

    private int id;
    private String juego;
    private String formato;
    private String region;
    private int rangoMin;
    private int rangoMax;
    private int latenciaMax;
    private LocalDateTime fechaHora;
    private int duracion;
    private int cupos;
    private int jugadoresAceptados;

    private final ScrimContext contexto;

    public Scrim(int id, String juego, String formato, String region,
                 int rangoMin, int rangoMax, int latenciaMax,
                 LocalDateTime fechaHora, int duracion, int cupos) {

        if (cupos <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de cupos debe ser mayor a cero"
            );
        }

        this.id = id;
        this.juego = juego;
        this.formato = formato;
        this.region = region;
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
        this.latenciaMax = latenciaMax;
        this.fechaHora = fechaHora;
        this.duracion = duracion;
        this.cupos = cupos;
        this.jugadoresAceptados = 0;

        this.contexto = new ScrimContext();
    }

    public int getId() {
        return id;
    }

    public String getJuego() {
        return juego;
    }

    public String getFormato() {
        return formato;
    }

    public String getRegion() {
        return region;
    }

    public int getRangoMin() {
        return rangoMin;
    }

    public int getRangoMax() {
        return rangoMax;
    }

    public int getLatenciaMax() {
        return latenciaMax;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public int getDuracion() {
        return duracion;
    }

    public int getCupos() {
        return cupos;
    }

    public int getJugadoresAceptados() {
        return jugadoresAceptados;
    }

    public int getCuposDisponibles() {
        return cupos - jugadoresAceptados;
    }

    public String getEstadoActual() {
        return contexto.getEstadoActual();
    }

    public void registrarJugadorAceptado() {

        if (!getEstadoActual().equals("Buscando jugadores")) {
            throw new IllegalStateException(
                    "El scrim ya no acepta jugadores"
            );
        }

        if (jugadoresAceptados >= cupos) {
            throw new IllegalStateException(
                    "No quedan cupos disponibles"
            );
        }

        jugadoresAceptados++;

        if (jugadoresAceptados == cupos) {
            contexto.completarCupos();
        }
    }

    public void completarCupos() {
        if (jugadoresAceptados != cupos) {
            throw new IllegalStateException(
                    "Todavia faltan jugadores para completar el lobby"
            );
        }

        contexto.completarCupos();
    }

    public void confirmarJugadores() {
        contexto.confirmarJugadores();
    }

    public void iniciar() {
        contexto.iniciar();
    }

    public void finalizar() {
        contexto.finalizar();
    }

    public void cancelar() {
        contexto.cancelar();
    }
}
