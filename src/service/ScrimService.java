
package service;

import domain.Scrim;
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
}
