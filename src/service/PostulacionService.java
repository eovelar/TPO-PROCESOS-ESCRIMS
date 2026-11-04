
package service;

import domain.Usuario;
import domain.Scrim;
import domain.Postulacion;

import java.util.ArrayList;
import java.util.List;

public class PostulacionService {

    private final List<Postulacion> postulaciones;

    public PostulacionService() {
        this.postulaciones = new ArrayList<>();
    }

    public boolean cumpleRequisitos(Usuario usuario, Scrim scrim) {

        if (usuario == null || scrim == null) {
            return false;
        }

        if (usuario.getRegion() == null || scrim.getRegion() == null) {
            return false;
        }

        if (!usuario.getRegion().equalsIgnoreCase(scrim.getRegion())) {
            return false;
        }

        // usa el rango del juego si el usuario lo tiene configurado
        Integer rangoJuego = usuario.getRangoPorJuego(scrim.getJuego());

        int rangoJugador = rangoJuego != null
                ? rangoJuego
                : usuario.getRango();

        if (rangoJugador < scrim.getRangoMin()
                || rangoJugador > scrim.getRangoMax()) {
            return false;
        }

        return true;
    }

    public Postulacion crearPostulacion(int id, Usuario usuario,
                                        Scrim scrim, String rolDeseado) {

        if (!cumpleRequisitos(usuario, scrim)) {
            throw new IllegalArgumentException(
                    "El usuario no cumple los requisitos del scrim"
            );
        }

        if (!scrim.getEstadoActual().equals("Buscando jugadores")) {
            throw new IllegalStateException(
                    "El scrim ya no acepta postulaciones"
            );
        }

        if (scrim.getCuposDisponibles() == 0) {
            throw new IllegalStateException(
                    "No quedan cupos disponibles"
            );
        }

        if (rolDeseado == null || rolDeseado.isBlank()) {
            throw new IllegalArgumentException(
                    "El rol deseado es obligatorio"
            );
        }

        // evita postulaciones repetidas para una misma partida
        for (Postulacion existente : postulaciones) {

            if (existente.getScrim().getId() == scrim.getId()
                    && existente.getUsuario().getId() == usuario.getId()
                    && !existente.getEstado().equals("Rechazada")) {

                throw new IllegalArgumentException(
                        "El usuario ya tiene una postulacion para este scrim"
                );
            }
        }

        for (Usuario jugador : scrim.getJugadores()) {
            if (jugador.getId() == usuario.getId()) {
                throw new IllegalArgumentException(
                        "El jugador ya participa en este scrim"
                );
            }
        }

        Postulacion postulacion = new Postulacion(
                id, usuario, scrim, rolDeseado.trim()
        );

        postulaciones.add(postulacion);

        return postulacion;
    }

    public void aceptarPostulacion(Postulacion postulacion) {

        validarPostulacion(postulacion);

        if (!postulacion.getEstado().equals("Pendiente")) {
            throw new IllegalStateException(
                    "La postulacion ya fue procesada"
            );
        }

        Usuario usuario = postulacion.getUsuario();
        Scrim scrim = postulacion.getScrim();

        if (!cumpleRequisitos(usuario, scrim)) {
            throw new IllegalArgumentException(
                    "El usuario no cumple los requisitos del scrim"
            );
        }

        if (!scrim.getEstadoActual().equals("Buscando jugadores")) {
            throw new IllegalStateException(
                    "El scrim ya no acepta jugadores"
            );
        }

        if (scrim.getCuposDisponibles() == 0) {
            throw new IllegalStateException(
                    "No quedan cupos disponibles"
            );
        }

        // ocupa un cupo y actualiza el estado de la postulacion
        scrim.registrarJugadorAceptado(usuario);
        postulacion.aceptar();
    }

    public void rechazarPostulacion(Postulacion postulacion) {

        validarPostulacion(postulacion);

        postulacion.rechazar();
    }

    public List<Postulacion> listarPostulaciones() {
        return new ArrayList<>(postulaciones);
    }

    public List<Postulacion> buscarPorScrim(int idScrim) {

        List<Postulacion> resultados = new ArrayList<>();

        for (Postulacion postulacion : postulaciones) {
            if (postulacion.getScrim().getId() == idScrim) {
                resultados.add(postulacion);
            }
        }

        return resultados;
    }

    private void validarPostulacion(Postulacion postulacion) {

        if (postulacion == null) {
            throw new IllegalArgumentException(
                    "La postulacion no puede ser nula"
            );
        }

        if (!postulaciones.contains(postulacion)) {
            throw new IllegalArgumentException(
                    "La postulacion no esta registrada en el servicio"
            );
        }
    }
}
