
package service;

import domain.Usuario;
import domain.Scrim;
import domain.Postulacion;

public class PostulacionService {

    public boolean cumpleRequisitos(Usuario usuario, Scrim scrim) {

        if (usuario == null || scrim == null) {
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

        return new Postulacion(id, usuario, scrim, rolDeseado);
    }
}
