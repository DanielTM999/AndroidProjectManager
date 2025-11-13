package dtm.core.dependencymanager.core;

import android.app.NotificationManager;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NotificationCreator {
    private String title;
    private String content;
    private int iconResId;
    private int notificationId;
    private boolean autoCancel;
    private boolean highPriority;
    private boolean isOngoing;

    private String channelId;
    private String channelName;
    private int chanelImportance;
    private List<NotificationAction> actions;

    public NotificationCreator(){
        this.title = "Notificação";
        this.content = "Você recebeu uma nova mensagem.";
        this.iconResId = android.R.drawable.ic_dialog_info;
        this.notificationId = (int) System.currentTimeMillis();
        this.highPriority = false;
        this.isOngoing = false;
        this.autoCancel = false;

        this.channelId = "default_channel";
        this.channelName = "Default Notification Channel";
        this.chanelImportance = NotificationManager.IMPORTANCE_DEFAULT;
        this.actions = new ArrayList<>();
    }

    public void addIntentAction(@NonNull NotificationAction action){
        this.actions.add(action);
    }
}
