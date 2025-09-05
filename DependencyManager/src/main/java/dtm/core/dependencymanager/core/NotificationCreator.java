package dtm.core.dependencymanager.core;

import android.app.NotificationManager;

import androidx.appcompat.app.AppCompatActivity;

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
    private Class<? extends AppCompatActivity> referenceActivicy;

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
        this.referenceActivicy = null;
    }
}
