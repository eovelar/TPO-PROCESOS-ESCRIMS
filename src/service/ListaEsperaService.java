
package service;

import domain.Scrim;
import domain.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListaEsperaService {

    private final Map<Integer, List<Usuario>> listasEspera;
    private final PostulacionService postulacionService;

    public ListaEsperaService(PostulacionService postulacionService) {

        if (postulacionService == null) {
            throw new IllegalArgumentException(
                    "El servicio de postulaciones es obligatorio"
            );
        }

        this.listasEspera = new HashMap<>();
        this.postulacionService = postulacionService;
    }

    public void agregarSuplente(Scrim scrim, Usuario usuario) {

        if (scrim == null || usuario == null) {
            throw new IllegalArgumentException(
                    "El scrim y el usuario son obligatorios"
            );
        }

        String estado = scrim.getEstadoActual();

        if (!estado.equals("Lobby armado")
                && !estado.equals("Confirmado")) {
            throw new IllegalStateException(
                    "La lista de espera solo admite suplentes antes del inicio"
            );
        }

        if (!postulacionService.cumpleRequisitos(usuario, scrim)) {
            throw new IllegalArgumentException(
                    "El usuario no cumple los requisitos del scrim"
            );
        }

        for (Usuario jugador : scrim.getJugadores()) {
            if (jugador.getId() == usuario.getId()) {
                throw new IllegalArgumentException(
                        "El jugador ya participa en este scrim"
                );
            }
        }

        List<Usuario> suplentes = listasEspera.computeIfAbsent(
                scrim.getId(),
                id -> new ArrayList<>()
        );

        for (Usuario suplente : suplentes) {
            if (suplente.getId() == usuario.getId()) {
                throw new IllegalArgumentException(
                        "El jugador ya esta en la lista de espera"
                );
            }
        }

        suplentes.add(usuario);
    }

    public List<Usuario> obtenerSuplentes(Scrim scrim) {

        if (scrim == null) {
            throw new IllegalArgumentException(
                    "El scrim no puede ser nulo"
            );
        }

        List<Usuario> suplentes = listasEspera.get(scrim.getId());

        if (suplentes == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(suplentes);
    }

    public Usuario obtenerPrimerSuplente(Scrim scrim) {

        List<Usuario> suplentes = obtenerSuplentes(scrim);

        if (suplentes.isEmpty()) {
            return null;
        }

        return suplentes.get(0);
    }

    public void quitarSuplente(Scrim scrim, Usuario usuario) {

        if (scrim == null || usuario == null) {
            throw new IllegalArgumentException(
                    "El scrim y el usuario son obligatorios"
            );
        }

        List<Usuario> suplentes = listasEspera.get(scrim.getId());

        if (suplentes == null) {
            throw new IllegalArgumentException(
                    "El scrim no tiene suplentes"
            );
        }

        boolean eliminado = suplentes.removeIf(
                suplente -> suplente.getId() == usuario.getId()
        );

        if (!eliminado) {
            throw new IllegalArgumentException(
                    "El usuario no esta en la lista de espera"
            );
        }
    }

    public int getCantidadSuplentes(Scrim scrim) {
        return obtenerSuplentes(scrim).size();
    }

    public Usuario reemplazarJugador(Scrim scrim, Usuario titular) {

        if (scrim == null || titular == null) {
            throw new IllegalArgumentException(
                    "El scrim y el titular son obligatorios"
            );
        }

        Usuario suplente = obtenerPrimerSuplente(scrim);

        if (suplente == null) {
            throw new IllegalStateException(
                    "No hay suplentes disponibles"
            );
        }

        if (!postulacionService.cumpleRequisitos(suplente, scrim)) {
            throw new IllegalStateException(
                    "El suplente ya no cumple los requisitos"
            );
        }

        // libera el cupo y vuelve a armar el lobby
        scrim.registrarBaja(titular);
        scrim.registrarJugadorAceptado(suplente);

        quitarSuplente(scrim, suplente);

        return suplente;
    }
}
