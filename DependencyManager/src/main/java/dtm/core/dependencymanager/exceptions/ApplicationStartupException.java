package dtm.core.dependencymanager.exceptions;

public class ApplicationStartupException extends RuntimeException {
    public ApplicationStartupException(String message, Throwable root) {
        super(message, root);
    }
}
