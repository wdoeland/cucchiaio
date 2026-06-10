package nl.wouterdoeland.cucchiaio.service;

public class RecipeForbiddenException extends RuntimeException {
    public RecipeForbiddenException(Long id) {
        super("Forbidden to access: " + id);
    }
}
