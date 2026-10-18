package cl.duoc.dsy1107.biblioteca_backend.exception;

public class IsbnDuplicadoException extends RuntimeException {

    public IsbnDuplicadoException(String isbn) {
        super("Ya existe un libro con el ISBN: " + isbn);
    }
}