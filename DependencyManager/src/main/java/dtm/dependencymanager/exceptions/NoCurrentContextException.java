package dtm.dependencymanager.exceptions;

public class NoCurrentContextException extends RuntimeException {
    public NoCurrentContextException(String message) {
        super(message);
    }
}
