package cl.duoc.dsy1107.biblioteca_backend.exception;

public class SinStockException extends RuntimeException {
    
    public SinStockException(String mensaje) {
        super(mensaje);
    }
}
