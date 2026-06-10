package nl.wouterdoeland.cucchiaio.service;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(Long id) {
        super("Recipe not found: " + id);
    }
}
