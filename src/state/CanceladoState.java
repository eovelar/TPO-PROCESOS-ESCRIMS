
package state;

public class CanceladoState implements ScrimState {

    @Override
    public void completarCupos(ScrimContext context) {
        throw new IllegalStateException(
                "No se pueden completar cupos de una partida cancelada"
        );
    }

    @Override
    public void confirmarJugadores(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede confirmar una partida cancelada"
        );
    }

    @Override
    public void iniciar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede iniciar una partida cancelada"
        );
    }

    @Override
    public void finalizar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede finalizar una partida cancelada"
        );
    }

    @Override
    public void cancelar(ScrimContext context) {
        throw new IllegalStateException(
                "La partida ya fue cancelada"
        );
    }

    @Override
    public String getNombre() {
        return "Cancelado";
    }
}
