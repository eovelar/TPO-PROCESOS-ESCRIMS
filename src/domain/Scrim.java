
package domain;

import state.ScrimContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private final List<Usuario> jugadoresAceptados;
    private final List<Confirmacion> confirmaciones;
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

        this.jugadoresAceptados = new ArrayList<>();
        this.confirmaciones = new ArrayList<>();
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
        return jugadoresAceptados.size();
    }

    public List<Usuario> getJugadores() {
        return new ArrayList<>(jugadoresAceptados);
    }

    public int getCuposDisponibles() {
        return cupos - jugadoresAceptados.size();
    }

    public String getEstadoActual() {
        return contexto.getEstadoActual();
    }

    public void registrarJugadorAceptado(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo"
            );
        }

        if (!getEstadoActual().equals("Buscando jugadores")) {
            throw new IllegalStateException(
                    "El scrim ya no acepta jugadores"
            );
        }

        for (Usuario jugador : jugadoresAceptados) {
            if (jugador.getId() == usuario.getId()) {
                throw new IllegalArgumentException(
                        "El jugador ya participa en este scrim"
                );
            }
        }

        if (getCuposDisponibles() == 0) {
            throw new IllegalStateException(
                    "No quedan cupos disponibles"
            );
        }

        jugadoresAceptados.add(usuario);

        if (getCuposDisponibles() == 0) {
            contexto.completarCupos();
        }
    }

    public void completarCupos() {

        if (getCuposDisponibles() != 0) {
            throw new IllegalStateException(
                    "Todavia faltan jugadores para completar el lobby"
            );
        }

        contexto.completarCupos();
    }

    public int getCantidadConfirmados() {
        int cantidad = 0;

        for (Confirmacion confirmacion : confirmaciones) {
            if (confirmacion.isConfirmado()) {
                cantidad++;
            }
        }

        return cantidad;
    }

    public void confirmarJugador(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo"
            );
        }

        if (!getEstadoActual().equals("Lobby armado")) {
            throw new IllegalStateException(
                    "El scrim no esta esperando confirmaciones"
            );
        }

        boolean participa = false;

        for (Usuario jugador : jugadoresAceptados) {
            if (jugador.getId() == usuario.getId()) {
                participa = true;
                break;
            }
        }

        if (!participa) {
            throw new IllegalArgumentException(
                    "El usuario no participa en este scrim"
            );
        }

        for (Confirmacion confirmacion : confirmaciones) {
            if (confirmacion.getUsuario().getId() == usuario.getId()) {
                throw new IllegalStateException(
                        "El jugador ya confirmo su participacion"
                );
            }
        }

        Confirmacion confirmacion = new Confirmacion(
                confirmaciones.size() + 1, usuario, this
        );

        confirmacion.confirmar();
        confirmaciones.add(confirmacion);

        if (getCantidadConfirmados() == cupos) {
            contexto.confirmarJugadores();
        }
    }

    public void confirmarJugadores() {

        if (!getEstadoActual().equals("Lobby armado")) {
            throw new IllegalStateException(
                    "El scrim no esta esperando confirmaciones"
            );
        }

        if (getCantidadConfirmados() != cupos) {
            throw new IllegalStateException(
                    "Todavia faltan jugadores por confirmar"
            );
        }

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
