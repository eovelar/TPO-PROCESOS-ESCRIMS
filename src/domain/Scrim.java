
package domain;

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

    public Scrim(int id, String juego, String formato, String region,
                 int rangoMin, int rangoMax, int latenciaMax,
                 LocalDateTime fechaHora, int duracion, int cupos) {

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
}
