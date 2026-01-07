package dtm.dependencymanager.core.Exception;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;

public abstract class ExceptionHandler {

    private static final String TAG = "ExceptionHandler";

    public void onError(Thread thread, Throwable throwable, Context context){
        boolean isUiThread = isUiThread(thread);
        Log.e(
                TAG,
                "Exception In!\n" +
                        "Thread: " + thread.getName() + (isUiThread ? " (UI Thread)" : " (Background Thread)") + "\n" +
                        "Context: " + (context != null ? context.getClass().getSimpleName() : "null"),
                throwable
        );
        if(isUiThread) {
            onUiThreadError(thread, throwable, context);
            throw new RuntimeException("Stub!");
        }
    }

    protected boolean isUiThread(Thread thread){
        return  (thread == Looper.getMainLooper().getThread());
    }

    protected void runOnUiThread(Runnable runnable){
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    protected void onUiThreadError(Thread thread, Throwable throwable, Context context){
        Log.e(
                TAG,
                "FATAL ERROR on UI Thread!\n" +
                        "Thread: " + thread.getName() + "\n" +
                        "Context: " + (context != null ? context.getClass().getSimpleName() : "null"),
                throwable
        );
    }

}
