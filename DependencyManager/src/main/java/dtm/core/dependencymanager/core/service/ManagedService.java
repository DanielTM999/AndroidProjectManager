package dtm.core.dependencymanager.core.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import dtm.core.dependencymanager.containers.DependencyContainerStorage;
import dtm.core.dependencymanager.R;
import dtm.core.dependencymanager.core.DependencyContainer;
import dtm.core.dependencymanager.core.NotificationCreator;
import dtm.core.dependencymanager.exceptions.InvalidClassRegistrationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ManagedService extends Service {
    private final DependencyContainer dependencyContainer;
    private boolean load = false;

    public ManagedService(){
        this.dependencyContainer = DependencyContainerStorage.getInstance();
    }

    @Override
    public void onCreate() {
        preInit();
        super.onCreate();
        dependencyContainer.injectDependencies(this);
        this.load = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        preInit();
        if(!load){
            dependencyContainer.injectDependencies(this);
            this.load = true;
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void preInit(){
        try {
            load();
        } catch (InvalidClassRegistrationException e) {
            Log.e("InvalidClassRegistrationException", "preInit: erro ao registrar classe"+e.getReferenceClass(), e);
        }
    }

    protected Handler newHandlerPostDelayAction(Runnable runnable, long time){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, time);
        return handler;
    }

    protected Handler newHandlerPostAction(Runnable runnable){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
        return handler;
    }

    protected CompletableFuture<Void> runAsync(Runnable runnable){
        return runAsync(runnable, false);
    }

    protected CompletableFuture<Void> runAsync(Runnable runnable, boolean callExceptionHandler){
        return CompletableFuture.runAsync(runnable).whenComplete((result, throwable) -> {
            if (throwable != null && callExceptionHandler) {
                Thread current = Thread.currentThread();
                Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
                if (handler != null) {
                    handler.uncaughtException(current, throwable);
                }
            }
        });
    }

    protected <T> CompletableFuture<T> runAsync(Supplier<T> runnable) {
        return runAsync(runnable, false);
    }

    protected <T> CompletableFuture<T> runAsync(Supplier<T> runnable, boolean callExceptionHandler) {
        return CompletableFuture.supplyAsync(runnable)
                .whenComplete((result, throwable) -> {
                    if (throwable != null && callExceptionHandler) {
                        Thread current = Thread.currentThread();
                        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
                        if (handler != null) {
                            handler.uncaughtException(current, throwable);
                        }
                    }
                });
    }

    protected CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return runAsync(runnable, executor, false);
    }

    protected CompletableFuture<Void> runAsync(Runnable runnable, Executor executor, boolean callExceptionHandler) {
        return CompletableFuture.runAsync(runnable, executor)
                .whenComplete((result, throwable) -> {
                    if (throwable != null && callExceptionHandler) {
                        Thread current = Thread.currentThread();
                        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
                        if (handler != null) {
                            handler.uncaughtException(current, throwable);
                        }
                    }
                });
    }

    protected <T> CompletableFuture<T> runAsync(Supplier<T> runnable, Executor executor) {
        return runAsync(runnable, executor, false);
    }

    protected <T> CompletableFuture<T> runAsync(Supplier<T> runnable, Executor executor, boolean callExceptionHandler) {
        return CompletableFuture.supplyAsync(runnable, executor)
                .whenComplete((result, throwable) -> {
                    if (throwable != null && callExceptionHandler) {
                        Thread current = Thread.currentThread();
                        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
                        if (handler != null) {
                            handler.uncaughtException(current, throwable);
                        }
                    }
                });
    }

    protected CompletableFuture<Void> runDelayAsync(Runnable runnable, long delayInMillis){
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayInMillis);
                runnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    protected <T> Future<T> runDelayAsync(Supplier<T> runnable, long delayInMillis){
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayInMillis);
                return runnable.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        });
    }

    protected void sendSimpleNotification(Consumer<NotificationCreator> creatorConsumer){
        sendSimpleNotification(creatorConsumer, true);
    }
    protected void sendSimpleNotification(Consumer<NotificationCreator> creatorConsumer, boolean show){
        if(!show) return;
        NotificationCreator notificationCreator = new NotificationCreator();
        creatorConsumer.accept(notificationCreator);

        NotificationChannel channel = new NotificationChannel(
                notificationCreator.getChannelId(),
                notificationCreator.getChannelName(),
                notificationCreator.getChanelImportance()
        );

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        PendingIntent pendingIntent = null;
        if(notificationCreator.getReferenceActivicy() != null){
            Intent intent = new Intent(getApplicationContext(), notificationCreator.getReferenceActivicy());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }

        Notification notification = new NotificationCompat.Builder(this, notificationCreator.getChannelId())
                .setContentTitle(notificationCreator.getTitle())
                .setContentText(notificationCreator.getContent())
                .setSmallIcon(notificationCreator.getIconResId())
                .setPriority(notificationCreator.isHighPriority() ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(notificationCreator.isOngoing())
                .setAutoCancel(notificationCreator.isAutoCancel())
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(notificationCreator.getNotificationId(), notification);
    }

    protected void sendSimpleNotification(String title, String content) {
        sendSimpleNotification((nc) -> {
            nc.setTitle(title);
            nc.setContent(content);
            nc.setIconResId(R.drawable.new_message);
        });
    }

    protected void sendSimpleNotification(String title, String content, boolean highPriority) {
        sendSimpleNotification((nc) -> {
            nc.setTitle(title);
            nc.setContent(content);
            nc.setHighPriority(highPriority);
            nc.setIconResId(R.drawable.new_message);
        });
    }

    protected void preInjection(){}

    protected boolean isServiceLoad(){
        return load;
    }

    protected void load() throws InvalidClassRegistrationException {
        if(!dependencyContainer.isLoaded()){
            dependencyContainer.load();
        }
    }
}
