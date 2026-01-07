package dtm.dependencymanager.exceptions;

public class InvalidContextActivity extends RuntimeException {
    public InvalidContextActivity(String message) {
        super(message);
    }

    public InvalidContextActivity(String message, Throwable th) {
      super(message, th);
    }
}
