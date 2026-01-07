package dtm.core.dependencymanager.internal;

import android.content.Context;
import android.util.Log;

import dtm.core.dependencymanager.core.exception.ExceptionHandler;

public class ExceptionHandlerDefault extends ExceptionHandler {
    private final Thread.UncaughtExceptionHandler exceptionHandler;

    public ExceptionHandlerDefault(Thread.UncaughtExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void onError(Thread thread, Throwable throwable, Context context) {
        Log.e("ExceptionHandlerDefault", "Erro não tratado na thread: " + thread.getName(), throwable);
        if (exceptionHandler != null) exceptionHandler.uncaughtException(thread, throwable);
    }


}
