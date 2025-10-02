package dtm.core.dependencymanager.core.window;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import dtm.core.dependencymanager.internal.WindowContextHolder;
import lombok.NonNull;

public interface WindowEventListenerSender {

    default <T extends WindowEventListenerClient> void sendEvent(@NonNull Class<T> windowEventListenerClientClass, Object clientArgs){
        WindowEventListenerClient[] listenerClientArray = WindowContextHolder.getWindowEventListenerClientList()
                .stream()
                .filter(wcl -> wcl.getClass().equals(windowEventListenerClientClass))
                .toArray(WindowEventListenerClient[]::new);

        sendEvent(listenerClientArray, clientArgs);
    }

    default <T extends WindowEventListenerClient> void sendEvent(Object clientArgs, Class<T>... windowEventListenerClientClass){
        Set<Class<T>> classSet = Arrays.stream(windowEventListenerClientClass).collect(Collectors.toSet());
        WindowEventListenerClient[] listenerClientArray = WindowContextHolder.getWindowEventListenerClientList()
                .stream()
                .filter(wcl -> classSet.contains(wcl.getClass()))
                .toArray(WindowEventListenerClient[]::new);

        sendEvent(listenerClientArray, clientArgs);
    }

    default <T extends WindowEventListenerClient> void sendEvent(Object clientArgs, T... windowEventListenerClient){
        sendEvent(windowEventListenerClient, clientArgs);
    }

    default <T extends WindowEventListenerClient> void sendEvent(@NonNull T[] windowEventListenerClient, Object clientArgs){
        for (T client : windowEventListenerClient) {
            client.onReceiveEvent(this, clientArgs);
        }
    }

    default void sendBroadcastEvent(Object clientArgs){
        for (WindowEventListenerClient client : WindowContextHolder.getWindowEventListenerClientList()) {
            client.onReceiveEvent(this, clientArgs);
        }
    }

}
