
package state;

public class BuscandoState implements ScrimState {

    @Override
    public void completarCupos(ScrimContext context) {
        context.setEstado(new LobbyArmadoState());
    }

    @Override
    public void confirmarJugadores(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede confirmar mientras se buscan jugadores"
        );
    }

    @Override
    public void iniciar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede iniciar un scrim sin confirmar"
        );
    }

    @Override
    public void finalizar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede finalizar un scrim que no inicio"
        );
    }

    @Override
    public void cancelar(ScrimContext context) {
        context.setEstado(new CanceladoState());
    }

    @Override
    public String getNombre() {
        return "Buscando jugadores";
    }
}
