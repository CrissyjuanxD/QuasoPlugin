package Dificultades;

// Una etapa del server que se prende y se apaga con /changes
public interface Change {

    String id();

    String description();

    void apply();

    void revert();

    boolean isApplied();
}
