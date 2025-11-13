package dtm.core.dependencymanager.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.function.Consumer;
import java.util.function.Supplier;

import dtm.core.dependencymanager.R;

public interface NotificationService {
    void sendSimpleNotification(Consumer<NotificationCreator> creatorConsumer);
    void sendSimpleNotification(String title, String content);
    void sendSimpleNotification(String title, String content, boolean highPriority);


    static NotificationService of(Supplier<Context> contextAct){
        return new NotificationService() {

            @Override
            public void sendSimpleNotification(Consumer<NotificationCreator> creatorConsumer) {
                Context context = contextAct.get();
                NotificationCreator notificationCreator = new NotificationCreator();
                creatorConsumer.accept(notificationCreator);

                NotificationChannel channel = new NotificationChannel(
                        notificationCreator.getChannelId(),
                        notificationCreator.getChannelName(),
                        notificationCreator.getChanelImportance()
                );

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);


                NotificationCompat.Builder notificationBuilder  = new NotificationCompat.Builder(context, notificationCreator.getChannelId())
                        .setContentTitle(notificationCreator.getTitle())
                        .setContentText(notificationCreator.getContent())
                        .setSmallIcon(notificationCreator.getIconResId() != 0
                            ? notificationCreator.getIconResId()
                            : android.R.drawable.ic_dialog_info
                        )
                        .setPriority(notificationCreator.isHighPriority() ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(notificationCreator.isAutoCancel());



                boolean hasMainActivityIntent = false;

                for (NotificationAction action : notificationCreator.getActions()) {
                    if (action.getType() == null || action.getIntent() == null) continue;
                    PendingIntent pendingIntent = null;
                    switch (action.getType()) {
                        case ACTIVITY -> {
                            if (!hasMainActivityIntent) {
                                hasMainActivityIntent = true;
                                pendingIntent = PendingIntent.getActivity(
                                        context, 0, action.getIntent(),
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                );
                                notificationBuilder.setContentIntent(pendingIntent);
                            } else {
                                Log.w("NotificationHelper", "Apenas uma Activity pode ser associada à notificação principal.");
                            }
                        }
                        case BROADCAST -> {
                            pendingIntent = PendingIntent.getBroadcast(
                                    context, 0, action.getIntent(),
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );
                        }
                        case SERVICE -> {
                            pendingIntent = PendingIntent.getService(
                                    context, 0, action.getIntent(),
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );
                        }
                        case NONE -> {}
                    }

                    if (pendingIntent != null && action.getType() != NotificationActionType.ACTIVITY) {
                        notificationBuilder.addAction(action.getIconResId(), action.getLabel(), pendingIntent);
                    }
                }


                notificationManager.notify(notificationCreator.getNotificationId(), notificationBuilder.build());

            }

            @Override
            public void sendSimpleNotification(String title, String content) {
                sendSimpleNotification((nc) -> {
                    nc.setTitle(title);
                    nc.setContent(content);
                    nc.setIconResId(R.drawable.new_message);
                });
            }

            @Override
            public void sendSimpleNotification(String title, String content, boolean highPriority) {
                sendSimpleNotification((nc) -> {
                    nc.setTitle(title);
                    nc.setContent(content);
                    nc.setHighPriority(highPriority);
                    nc.setIconResId(R.drawable.new_message);
                });
            }
        };
    }

}
