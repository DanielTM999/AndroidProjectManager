package dtm.core.apptest;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

import dtm.core.dependencymanager.annotations.HandleException;
import dtm.core.dependencymanager.annotations.Inject;
import dtm.core.dependencymanager.core.Exception.ExceptionHandler;
import dtm.core.dependencymanager.core.activity.ContextHolderActivity;
import dtm.core.dependencymanager.utils.Exception.AbstractControllerAdvice;

public class ExceptionHandlerTeste extends AbstractControllerAdvice {

    @HandleException(RuntimeException.class)
    public void handleRuntime(Thread thread, Throwable throwable, Context context) {
        Log.e("MyControllerAdvice", "RuntimeException capturada!", throwable);
        runOnUiThread(() -> {
            Context activicyContext = ContextHolderActivity.getCurrentActivity();

            if(activicyContext == null) return;

            new AlertDialog.Builder(activicyContext)
                    .setTitle("Erro inesperado")
                    .setMessage(
                            "Thread: " + thread.getName() + "\n" +
                                    "Mensagem: " + throwable.getMessage()
                    )
                    .setPositiveButton("OK", null)
                    .show();
        });

    }

}
