package iuh.fit.airsky.exception;

public class GoogleAuthException extends RuntimeException {
    public GoogleAuthException(String message) {
        super(message);
    }
    
    public GoogleAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
