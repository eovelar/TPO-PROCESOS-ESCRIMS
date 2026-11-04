
package service;

import domain.Scrim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScrimService {

    private final List<Scrim> scrims;

    public ScrimService() {
        this.scrims = new ArrayList<>();
    }

    public void agregarScrim(Scrim scrim) {
        if (scrim == null) {
            throw new IllegalArgumentException("El scrim no puede ser nulo");
        }

        for (Scrim existente : scrims) {
            if (existente.getId() == scrim.getId()) {
                throw new IllegalArgumentException("Ya existe un scrim con ese ID");
            }
        }

        scrims.add(scrim);
    }

    public Scrim buscarPorId(int id) {
        for (Scrim scrim : scrims) {
            if (scrim.getId() == id) {
                return scrim;
            }
        }

        return null;
    }

    public List<Scrim> listarScrims() {
        return new ArrayList<>(scrims);
    }

    public List<Scrim> buscarPorJuego(String juego) {
        List<Scrim> resultados = new ArrayList<>();

        if (juego == null || juego.isBlank()) {
            return resultados;
        }

        for (Scrim scrim : scrims) {
            if (scrim.getJuego().equalsIgnoreCase(juego.trim())) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarPorFormato(String formato) {
        List<Scrim> resultados = new ArrayList<>();

        if (formato == null || formato.isBlank()) {
            return resultados;
        }

        for (Scrim scrim : scrims) {
            if (scrim.getFormato().equalsIgnoreCase(formato.trim())) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarPorRegion(String region) {
        List<Scrim> resultados = new ArrayList<>();

        if (region == null || region.isBlank()) {
            return resultados;
        }

        for (Scrim scrim : scrims) {
            if (scrim.getRegion().equalsIgnoreCase(region.trim())) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarPorRango(int rangoJugador) {
        List<Scrim> resultados = new ArrayList<>();

        for (Scrim scrim : scrims) {
            if (rangoJugador >= scrim.getRangoMin()
                    && rangoJugador <= scrim.getRangoMax()) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarPorFecha(LocalDate fecha) {
        List<Scrim> resultados = new ArrayList<>();

        if (fecha == null) {
            return resultados;
        }

        for (Scrim scrim : scrims) {
            if (scrim.getFechaHora().toLocalDate().equals(fecha)) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarPorLatencia(int latenciaJugador) {
        List<Scrim> resultados = new ArrayList<>();

        if (latenciaJugador < 0) {
            throw new IllegalArgumentException("La latencia no puede ser negativa");
        }

        for (Scrim scrim : scrims) {
            if (latenciaJugador <= scrim.getLatenciaMax()) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarPorEstado(String estado) {
        List<Scrim> resultados = new ArrayList<>();

        if (estado == null || estado.isBlank()) {
            return resultados;
        }

        for (Scrim scrim : scrims) {
            if (scrim.getEstadoActual().equalsIgnoreCase(estado.trim())) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarConCuposDisponibles() {
        List<Scrim> resultados = new ArrayList<>();

        for (Scrim scrim : scrims) {
            if (scrim.getCuposDisponibles() > 0
                    && scrim.getEstadoActual().equals("Buscando jugadores")) {
                resultados.add(scrim);
            }
        }

        return resultados;
    }

    public List<Scrim> buscarConFiltros(
            String juego,
            String formato,
            String region,
            Integer rangoJugador,
            LocalDate fecha,
            Integer latenciaJugador,
            String estado,
            boolean soloDisponibles) {

        List<Scrim> resultados = new ArrayList<>();

        if (latenciaJugador != null && latenciaJugador < 0) {
            throw new IllegalArgumentException("La latencia no puede ser negativa");
        }

        for (Scrim scrim : scrims) {

            if (juego != null && !juego.isBlank()
                    && !scrim.getJuego().equalsIgnoreCase(juego.trim())) {
                continue;
            }

            if (formato != null && !formato.isBlank()
                    && !scrim.getFormato().equalsIgnoreCase(formato.trim())) {
                continue;
            }

            if (region != null && !region.isBlank()
                    && !scrim.getRegion().equalsIgnoreCase(region.trim())) {
                continue;
            }

            if (rangoJugador != null
                    && (rangoJugador < scrim.getRangoMin()
                    || rangoJugador > scrim.getRangoMax())) {
                continue;
            }

            if (fecha != null
                    && !scrim.getFechaHora().toLocalDate().equals(fecha)) {
                continue;
            }

            if (latenciaJugador != null
                    && latenciaJugador > scrim.getLatenciaMax()) {
                continue;
            }

            if (estado != null && !estado.isBlank()
                    && !scrim.getEstadoActual().equalsIgnoreCase(estado.trim())) {
                continue;
            }

            if (soloDisponibles
                    && (scrim.getCuposDisponibles() == 0
                    || !scrim.getEstadoActual().equals("Buscando jugadores"))) {
                continue;
            }

            resultados.add(scrim);
        }

        return resultados;
    }
}
