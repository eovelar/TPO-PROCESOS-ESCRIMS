
package state;

public class ScrimContext {

    private ScrimState estado;

    public ScrimContext() {
        this.estado = new BuscandoState();
    }

    public void setEstado(ScrimState estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
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
}
