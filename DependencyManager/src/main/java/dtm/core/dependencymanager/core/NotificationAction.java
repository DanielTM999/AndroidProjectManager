package dtm.core.dependencymanager.core;

import android.content.Intent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAction {
    private String label;
    private int iconResId;
    private Intent intent;
    private NotificationActionType type;
}
