package dtm.dependencymanager.core.window;

public interface WindowEventListenerClient {
    default void onReceiveEvent(WindowEventListenerSender caller, Object clientArgs){}
}
