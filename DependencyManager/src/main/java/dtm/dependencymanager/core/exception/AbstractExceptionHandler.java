package dtm.dependencymanager.core.exception;

public abstract class AbstractExceptionHandler {

    public abstract void onError(Thread thread, Throwable throwable, Object context);
}
