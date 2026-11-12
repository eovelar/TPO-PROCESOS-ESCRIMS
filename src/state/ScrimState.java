
package state;

public interface ScrimState {

    // define las operaciones que puede realizar cada estado del scrim

    void completarCupos(ScrimContext context);

    void confirmarJugadores(ScrimContext context);

    void iniciar(ScrimContext context);

    void finalizar(ScrimContext context);

    void cancelar(ScrimContext context);

    String getNombre();
}
