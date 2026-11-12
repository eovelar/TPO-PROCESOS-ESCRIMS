
package state;

public class ConfirmadoState implements ScrimState {

    @Override
    public void completarCupos(ScrimContext context) {
        throw new IllegalStateException(
                "Los cupos ya fueron completados"
        );
    }

    @Override
    public void confirmarJugadores(ScrimContext context) {
        throw new IllegalStateException(
                "Los jugadores ya confirmaron su participacion"
        );
    }

    @Override
    public void iniciar(ScrimContext context) {
        context.setEstado(new EnJuegoState());
    }

    @Override
    public void finalizar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede finalizar una partida que no inicio"
        );
    }

    @Override
    public void cancelar(ScrimContext context) {
        context.setEstado(new CanceladoState());
    }

    @Override
    public String getNombre() {
        return "Confirmado";
    }
}
