package state;

public interface ScrimState {

    void completarCupos(ScrimContext context);

    void confirmarJugadores(ScrimContext context);

    void iniciar(ScrimContext context);

    void finalizar(ScrimContext context);

    void cancelar(ScrimContext context);

    String getNombre();
}
