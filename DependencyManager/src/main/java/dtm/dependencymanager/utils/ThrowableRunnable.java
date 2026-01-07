package dtm.dependencymanager.utils;

@FunctionalInterface
public interface ThrowableRunnable {
    void run() throws Exception;
}
