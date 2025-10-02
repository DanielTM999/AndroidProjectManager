package dtm.core.dependencymanager.core.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import dtm.core.dependencymanager.R;
import dtm.core.dependencymanager.core.NotificationCreator;
import dtm.core.dependencymanager.core.window.WindowEventListener;
import dtm.core.dependencymanager.internal.WindowContextHolder;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ContextManagedFragment extends Fragment implements WindowEventListener {

    protected ViewBinding viewBinding;

    protected int idLayout;

    protected ContextManagedFragment(){
    }

    protected ContextManagedFragment(int idLayout){
        this.idLayout = idLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        saveState();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        saveState();
        return view;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState, int idLayout) {
        View view = inflater.inflate(idLayout, container, false);
        saveState();
        return view;
    }

    public <T extends ViewBinding> View onCreateView(@NonNull Supplier<T> action){
        viewBinding = action.get();
        View view = viewBinding.getRoot();
        saveState();
        return view;
    }

    protected long getAvailableMemory(){
        Context context = requireContext();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    protected long getTotalMemory() {
        Context context = requireContext();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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
    
    protected void requestPermission(String permission, int requestCode) {
        Context context = getContext();
        Activity activity = getActivity();
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(context), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Objects.requireNonNull(activity), new String[]{permission}, requestCode);
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
        return CompletableFuture.runAsync(runnable);
    }

    protected <T> CompletableFuture<T> runAsync(Supplier<T> runnable){
        return CompletableFuture.supplyAsync(runnable);
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

    protected <T> CompletableFuture<T> runDelayAsync(Supplier<T> runnable, long delayInMillis){
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
        Context context = requireContext();
        NotificationCreator notificationCreator = new NotificationCreator();
        creatorConsumer.accept(notificationCreator);

        NotificationChannel channel = new NotificationChannel(
                notificationCreator.getChannelId(),
                notificationCreator.getChannelName(),
                notificationCreator.getChanelImportance()
        );

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(context, notificationCreator.getChannelId())
                .setContentTitle(notificationCreator.getTitle())
                .setContentText(notificationCreator.getContent())
                .setSmallIcon(notificationCreator.getIconResId())
                .setPriority(notificationCreator.isHighPriority() ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
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

    protected void runOnUiThread(Runnable action){
        requireActivity().runOnUiThread(action);
    }
    protected boolean isActivityAlive() {
        Activity activity = getActivity();
        return activity != null && !activity.isFinishing();
    }

    protected void hideKeyboard() {
        View view = getView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected void withContext(Consumer<Context> action) {
        Context context = getContext();
        if (context != null) action.accept(context);
    }

    protected void withActivity(Consumer<FragmentActivity> action) {
        FragmentActivity context = getActivity();
        if (context != null) action.accept(context);
    }

    protected void waitForMemory(float minPercentMemory){
        long waitNanos = TimeUnit.MILLISECONDS.toNanos(100);
        while (!canExecuteWithMemory(minPercentMemory)) {
            LockSupport.parkNanos(waitNanos);
            waitNanos = Math.min(waitNanos * 2, TimeUnit.SECONDS.toNanos(2));
        }
    }

    private boolean canExecuteWithMemory(float minPercentFreeMemory) {
        long maxHeap = getMaxMemory();
        long usedHeap = getUsedMemory();
        long freeHeap = maxHeap - usedHeap;

        long minRequired = (long) (maxHeap * minPercentFreeMemory);
        checkMemoryStatus("mb");
        return freeHeap >= minRequired;
    }

    private void saveState(){
        WindowContextHolder.registerFragment(getActivity(), this);
    }
}
