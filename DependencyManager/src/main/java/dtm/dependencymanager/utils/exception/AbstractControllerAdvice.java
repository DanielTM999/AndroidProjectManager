package dtm.dependencymanager.utils.exception;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dtm.dependencymanager.annotations.HandleException;
import dtm.dependencymanager.annotations.Inject;
import dtm.dependencymanager.core.exception.ExceptionHandler;

public abstract class AbstractControllerAdvice extends ExceptionHandler {
    private final Map<Class<? extends Throwable>, Method> throwableMethodMap;
    @Inject
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    protected AbstractControllerAdvice(){
        this.throwableMethodMap = new ConcurrentHashMap<>();
        initExceptionHandlers();
    }

    @Override
    public void onError(Thread thread, Throwable throwable, Context context) {
        //super.onError(thread, throwable, context);
        Throwable realThrowable = unwrapThrowable(throwable);
        Class<?> throwableClass = realThrowable.getClass();
        Method handlerMethod = throwableMethodMap.get(throwableClass);
        if (handlerMethod != null) {
            try {
                handlerMethod.setAccessible(true);
                handlerMethod.invoke(this, thread, realThrowable, context);
                return;
            } catch (Exception e) {
                Log.e("ControllerAdvice", "Erro ao invocar handler de exceção", e);
            }
        }

        if (uncaughtExceptionHandler != null) {
            uncaughtExceptionHandler.uncaughtException(thread, throwable);
        } else {
            Log.e("ControllerAdvice", "Erro não tratado", throwable);
        }
    }

    private void initExceptionHandlers() {
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(HandleException.class)) {
                HandleException annotation = method.getAnnotation(HandleException.class);
                Class<? extends Throwable>[] classList = annotation.value();
                if(classList != null){
                    for (Class<? extends Throwable> exClass : annotation.value()) {
                        throwableMethodMap.put(exClass, method);
                    }
                }
            }
        }
    }

    private Throwable unwrapThrowable(Throwable throwable) {
        Throwable t = throwable;
        while (t instanceof java.util.concurrent.ExecutionException
                || t instanceof java.util.concurrent.CompletionException) {
            if (t.getCause() != null) {
                t = t.getCause();
            } else {
                break;
            }
        }
        return t;
    }

}
