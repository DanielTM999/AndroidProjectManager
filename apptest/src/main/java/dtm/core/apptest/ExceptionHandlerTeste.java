package dtm.core.apptest;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import dtm.core.dependencymanager.utils.Exception.AbstractControllerAdvice;
import dtm.dependencymanager.annotations.HandleException;

public class ExceptionHandlerTeste extends AbstractControllerAdvice {

    @HandleException(RuntimeException.class)
    public void handleRuntime(Thread thread, Throwable throwable, Context context) {
        Log.e("MyControllerAdvice", "RuntimeException capturada!", throwable);

        if(isUiThread(thread)) return;

        runOnUiThread(() -> {

            if(context == null) return;

            new AlertDialog.Builder(context)
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
