package Dificultades;

import org.bukkit.inventory.Recipe;

import java.util.List;

// Una etapa del server que se prende y se apaga con /changes
public interface Change {

    String id();

    String description();

    void apply();

    void revert();

    boolean isApplied();

    // Lo que se anuncia en el chat cuando un admin activa la etapa: el título y una línea por cambio
    default String tituloAnuncio() {
        return null;
    }

    default List<String> anuncio() {
        return List.of();
    }

    // Las recetas de la etapa sin registrarlas en el servidor: la web las usa para mostrar todos los crafteos
    default List<Recipe> recetas() {
        return List.of();
    }
}
