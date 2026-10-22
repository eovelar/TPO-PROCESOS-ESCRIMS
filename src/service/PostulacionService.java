
package service;

import domain.Usuario;
import domain.Scrim;
import domain.Postulacion;

public class PostulacionService {

    public boolean cumpleRequisitos(Usuario usuario, Scrim scrim) {

        if (usuario == null || scrim == null) {
            return false;
        }

        if (usuario.getRegion() == null ||
                scrim.getRegion() == null) {
            return false;
        }

        if (!usuario.getRegion().equalsIgnoreCase(scrim.getRegion())) {
            return false;
        }

        if (usuario.getRango() < scrim.getRangoMin()) {
            return false;
        }

        if (usuario.getRango() > scrim.getRangoMax()) {
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

        for (Usuario jugador : scrim.getJugadores()) {
            if (jugador.getId() == usuario.getId()) {
                throw new IllegalArgumentException(
                        "El jugador ya participa en este scrim"
                );
            }
        }

        return new Postulacion(id, usuario, scrim, rolDeseado);
    }

    public void aceptarPostulacion(Postulacion postulacion) {

        if (postulacion == null) {
            throw new IllegalArgumentException(
                    "La postulacion no puede ser nula"
            );
        }

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

        if (scrim.getCuposDisponibles() == 0) {
            throw new IllegalStateException(
                    "No quedan cupos disponibles"
            );
        }

        scrim.registrarJugadorAceptado(usuario);
        postulacion.aceptar();
    }

    public void rechazarPostulacion(Postulacion postulacion) {

        if (postulacion == null) {
            throw new IllegalArgumentException(
                    "La postulacion no puede ser nula"
            );
        }

        postulacion.rechazar();
    }
}
