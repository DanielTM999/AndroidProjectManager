package dtm.core.dependencymanager.core.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import org.jetbrains.annotations.NotNull;
import dtm.core.dependencymanager.R;
import dtm.core.dependencymanager.core.NotificationAction;
import dtm.core.dependencymanager.core.NotificationActionType;
import dtm.core.dependencymanager.core.NotificationCreator;
import dtm.core.dependencymanager.core.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ContextActivity extends AppCompatActivity {

    private final NotificationService notificationService;
    
    public ContextActivity(){
        this.notificationService = NotificationService.of(() -> this);
    }

    protected void onCreate(Bundle savedInstanceState, @LayoutRes Integer idLayout) {
        super.onCreate(savedInstanceState);
        if(idLayout != null){
            setContentView(idLayout);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    public boolean isActivityLoad(){
        return isActivityAlive();
    }

    protected long getAvailableMemory(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    protected long getTotalMemory() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }

    protected long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    protected long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    protected String checkMemoryStatus() {
        return checkMemoryStatus("bytes");
    }

    @SuppressLint("DefaultLocale")
    protected String checkMemoryStatus(String type) {
        type = (type != null) ? type.toLowerCase() : "bytes";
        long availableMemory = getAvailableMemory();
        long totalMemory = getTotalMemory();
        long usedMemory = getUsedMemory();
        long maxMemory = getMaxMemory();

        String unit;
        double divisor = switch (type) {
            case "mb", "megabyte" -> {
                unit = "MB";
                yield 1024.0 * 1024.0;
            }
            case "gb", "gigabyte" -> {
                unit = "GB";
                yield 1024.0 * 1024.0 * 1024.0;
            }
            default -> {
                unit = "bytes";
                yield 1;
            }
        };

        return String.format(
                "Memória disponível: %.2f %s\n" +
                        "Memória total: %.2f %s\n" +
                        "Memória usada: %.2f %s\n" +
                        "Memória máxima: %.2f %s",
                availableMemory / divisor, unit,
                totalMemory / divisor, unit,
                usedMemory / divisor, unit,
                maxMemory / divisor, unit
        );
    }


    protected void showMemoryStatusDialog() {
        showMemoryStatusDialog("bytes");
    }

    protected void showMemoryStatusDialog(String type) {
        String memoryStatus = checkMemoryStatus(type);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Status da Memória do APP")
                .setMessage(memoryStatus)
                .setPositiveButton("OK", null)
                .show();
    }

    @SuppressWarnings("unchecked")
    protected <T> T getIntentParam(@NonNull String key) {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(key)) {
            Object value = intent.getExtras().get(key);
            return (T) value;
        }
        return null;
    }

    protected <T> T getIntentParam(@NonNull String key, Class<T> type) {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(key)) {
            Object value = intent.getExtras().get(key);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    protected void requestPermission(@NonNull String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }
    protected void requestPermission(@NonNull String[] permissions, int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    requestCode
            );
        }
    }
    protected void requestPermission(@NonNull List<String> permissions, int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    requestCode
            );
        }
    }


    protected void handlerPostDelayAction(Handler handler, Runnable runnable, long time){
        handler.postDelayed(runnable, time);
    }

    protected void handlerPostAction(Handler handler, Runnable runnable){
        handler.post(runnable);
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
        return runDelayAsync(runnable, delayInMillis, false);
    }

    protected CompletableFuture<Void> runDelayAsync(Runnable runnable, long delayInMillis, boolean callExceptionHandler){
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayInMillis);
                runnable.run();
            } catch (InterruptedException ignored) {}
        }).whenComplete((result, throwable) -> {
            if (throwable != null && callExceptionHandler) {
                Thread current = Thread.currentThread();
                Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
                if (handler != null) {
                    handler.uncaughtException(current, throwable);
                }
            }
        });
    }

    protected <T> Future<T> runDelayAsync(Supplier<T> runnable, long delayInMillis){
        return runDelayAsync(runnable, delayInMillis, false);
    }

    protected <T> Future<T> runDelayAsync(Supplier<T> runnable, long delayInMillis, boolean callExceptionHandler){
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayInMillis);
                return runnable.get();
            } catch (InterruptedException e) {
                return null;
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null && callExceptionHandler) {
                Thread current = Thread.currentThread();
                Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
                if (handler != null) {
                    handler.uncaughtException(current, throwable);
                }
            }
        });
    }

    protected void setOnClickListener(View view, @NotNull Runnable onClickListener){
        view.setOnClickListener(v -> onClickListener.run());
    }

    protected void setOnClickListener(View view, @NotNull View.OnClickListener onClickListener){
        view.setOnClickListener(onClickListener);
    }

    protected void setOnClickListenerAsync(View view, @NotNull Runnable onClickListener){
        view.setOnClickListener(v -> {
            runAsync(onClickListener);
        });
    }
    protected void setOnClickListenerAsync(View view, @NotNull Consumer<View> onClickListener){
        view.setOnClickListener(v -> {
            runAsync(() -> {
                onClickListener.accept(v);
            });
        });
    }
    protected void setOnClickListenerAsync(View view, @NotNull Runnable onClickListener, boolean callExceptionHandler){
        view.setOnClickListener(v -> {
            runAsync(onClickListener, callExceptionHandler);
        });
    }
    protected void setOnClickListenerAsync(View view, @NotNull Consumer<View> onClickListener, boolean callExceptionHandler){
        view.setOnClickListener(v -> {
            runAsync(() -> {
                onClickListener.accept(v);
            }, callExceptionHandler);
        });
    }

    protected void sendSimpleNotification(Consumer<NotificationCreator> creatorConsumer){
        notificationService.sendSimpleNotification(creatorConsumer);
    }

    protected void sendSimpleNotification(String title, String content) {
        notificationService.sendSimpleNotification(title, content);
    }

    protected void sendSimpleNotification(String title, String content, boolean highPriority) {
        notificationService.sendSimpleNotification(title, content, highPriority);
    }


    protected boolean isActivityAlive() {
        return !this.isFinishing();
    }

}
