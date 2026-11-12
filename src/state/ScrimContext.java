
package state;

public class ScrimContext {

    private ScrimState estado;

    public ScrimContext() {
        this.estado = new BuscandoState();
    }

    public void setEstado(ScrimState estado) {
        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado no puede ser nulo"
            );
        }

        this.estado = estado;
    }

    public String getEstadoActual() {
        return estado.getNombre();
    }

    public void completarCupos() {
        estado.completarCupos(this);
    }

    public void confirmarJugadores() {
        estado.confirmarJugadores(this);
    }

    public void iniciar() {
        estado.iniciar(this);
    }

    public void finalizar() {
        estado.finalizar(this);
    }

    public void cancelar() {
        estado.cancelar(this);
    }

    public void registrarBaja() {
        String estadoActual = getEstadoActual();

        if (!estadoActual.equals("Buscando jugadores")
                && !estadoActual.equals("Lobby armado")
                && !estadoActual.equals("Confirmado")) {
            throw new IllegalStateException(
                    "No se pueden registrar bajas en este estado"
            );
        }

        // vuelve a buscar jugadores si se libera un cupo
        if (!estadoActual.equals("Buscando jugadores")) {
            setEstado(new BuscandoState());
        }
    }
}
