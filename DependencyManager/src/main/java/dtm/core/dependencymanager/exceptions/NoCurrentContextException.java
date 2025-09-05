package dtm.core.dependencymanager.exceptions;

public class NoCurrentContextException extends RuntimeException {
    public NoCurrentContextException(String message) {
        super(message);
    }
}
