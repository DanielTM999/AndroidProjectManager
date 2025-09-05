package dtm.core.dependencymanager.core.fragment.popup;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import dtm.core.dependencymanager.R;
import dtm.core.dependencymanager.core.NotificationCreator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;

public abstract class ContextManagedFragmentPopup extends DialogFragment {

    @Override
    public void onStart() {
        super.onStart();
    }

    public void onStart(boolean fullScreen) {
        super.onStart();
        if(fullScreen){
            Dialog dialog = getDialog();
            if (dialog != null && dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    protected void requestPermission(@NonNull ActivityResultLauncher<String> requestPermissionLauncher, @NonNull String permissions) {
        requestPermissionLauncher.launch(permissions);
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

    protected Future<Void> runAsync(Runnable runnable){
        return CompletableFuture.runAsync(runnable);
    }

    protected <T> Future<T> runAsync(Supplier<T> runnable){
        return CompletableFuture.supplyAsync(runnable);
    }

    protected Future<Void> runDelayAsync(Runnable runnable, long delayInMillis){
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
}
