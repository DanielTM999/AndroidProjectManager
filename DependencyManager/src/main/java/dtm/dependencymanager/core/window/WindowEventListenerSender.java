package dtm.dependencymanager.core.window;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import dtm.dependencymanager.internal.WindowContextHolder;
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

    default <T extends WindowEventListenerClient> void sendEvent(@NonNull Class<T> windowEventListenerClientClass, Object clientArgs, boolean async){
        WindowEventListenerClient[] listenerClientArray = WindowContextHolder.getWindowEventListenerClientList()
                .stream()
                .filter(wcl -> wcl.getClass().equals(windowEventListenerClientClass))
                .toArray(WindowEventListenerClient[]::new);

        sendEvent(listenerClientArray, clientArgs, async);
    }

    default <T extends WindowEventListenerClient> void sendEvent(boolean async, Object clientArgs, Class<T>... windowEventListenerClientClass){
        Set<Class<T>> classSet = Arrays.stream(windowEventListenerClientClass).collect(Collectors.toSet());
        WindowEventListenerClient[] listenerClientArray = WindowContextHolder.getWindowEventListenerClientList()
                .stream()
                .filter(wcl -> classSet.contains(wcl.getClass()))
                .toArray(WindowEventListenerClient[]::new);

        sendEvent(listenerClientArray, clientArgs, async);
    }

    default <T extends WindowEventListenerClient> void sendEvent(boolean async, Object clientArgs, T... windowEventListenerClient){
        sendEvent(windowEventListenerClient, clientArgs, async);
    }

    default <T extends WindowEventListenerClient> void sendEvent(@NonNull T[] windowEventListenerClient, Object clientArgs){
        sendEvent(windowEventListenerClient, clientArgs, true);
    }

    default <T extends WindowEventListenerClient> void sendEvent(@NonNull T[] windowEventListenerClient, Object clientArgs, boolean async){
        if(async){
            CompletableFuture<?>[] tasks = Arrays.stream(windowEventListenerClient)
                    .map(client -> CompletableFuture.runAsync(() -> {
                        client.onReceiveEvent(this, clientArgs);
                    }))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(tasks).join();
        }else{
            for (T client : windowEventListenerClient) {
                client.onReceiveEvent(this, clientArgs);
            }
        }
    }

    default void sendBroadcastEvent(Object clientArgs){
        sendBroadcastEvent(clientArgs, true);
    }

    default void sendBroadcastEvent(Object clientArgs, boolean async){
        List<WindowEventListenerClient> listenersClient = WindowContextHolder.getWindowEventListenerClientList();
        if(async){
            CompletableFuture<?>[] tasks = listenersClient.stream()
                    .map(client -> CompletableFuture.runAsync(() -> {
                        client.onReceiveEvent(this, clientArgs);
                    }))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(tasks).join();
        }else{
            for (WindowEventListenerClient client : listenersClient) {
                client.onReceiveEvent(this, clientArgs);
            }
        }
    }

}
