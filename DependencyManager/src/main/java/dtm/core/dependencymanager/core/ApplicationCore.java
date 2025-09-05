package dtm.core.dependencymanager.core;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import dtm.core.dependencymanager.annotations.UseExceptionHandler;
import dtm.core.dependencymanager.core.Exception.ExceptionHandler;
import dtm.core.dependencymanager.exceptions.ApplicationStartupException;
import dtm.core.dependencymanager.internal.AppElementsMapperStorage;
import dtm.core.dependencymanager.containers.DependencyContainerStorage;
import dtm.core.dependencymanager.exceptions.InvalidClassRegistrationException;
import dtm.core.dependencymanager.internal.ExceptionHandlerDefault;

public abstract class ApplicationCore extends Application {

    protected final DependencyContainer dependencyContainer;
    private final AtomicReference<ExceptionHandler> defaultExceptionHandlerRef;
    private final Class<? extends ExceptionHandler> exceptionHandlerClass;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    protected ApplicationCore(){
        this.dependencyContainer = DependencyContainerStorage.getInstance();
        this.defaultExceptionHandlerRef = new AtomicReference<>(
                new ExceptionHandlerDefault(Thread.getDefaultUncaughtExceptionHandler())
        );
        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.exceptionHandlerClass = getExceptionHandlerClass();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this::onApplicationError);
        beforeLoad();
        load().whenComplete((result, throwable) -> {
            if (throwable != null) {
                runOnUiThread(() -> onLoadError(throwable));
            }else {
                configureCustomExceptionHandler();
                runOnUiThread(this::afterLoad);
            }
        });
    }

    protected void beforeLoad(){
        AppElementsMapperStorage.getInstance(this);
    }

    protected void afterLoad(){}

    protected void onLoadError(Throwable th){
        if(th instanceof ApplicationStartupException e){
            throw e;
        }
        throw new ApplicationStartupException("Erro ao iniciar a aplicação", th);
    }

    protected void onApplicationError(Thread thread, Throwable throwable){
        ExceptionHandler exceptionHandler = defaultExceptionHandlerRef.get();
        if(exceptionHandler != null){
            exceptionHandler.onError(thread, throwable, getApplicationContext());
        }else{
            Log.e("onApplicationError", "Erro não tratado na thread: " + thread.getName(), throwable);
        }
    }

    protected void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }

    private CompletableFuture<?> load(){
        return CompletableFuture.runAsync(() -> {
            try {
                dependencyContainer.load();
            } catch (InvalidClassRegistrationException e) {
                throw new ApplicationStartupException("Erro ao carregar Conteiner de dependencias", e);
            }
        });
    }

    private void configureCustomExceptionHandler(){
        try{
            dependencyContainer.registerDependency(uncaughtExceptionHandler);
            if(exceptionHandlerClass != null){
                ExceptionHandler exceptionHandlerCustom = dependencyContainer.newInstance(exceptionHandlerClass);
                if(exceptionHandlerCustom != null) defaultExceptionHandlerRef.set(exceptionHandlerCustom);
            }
        }catch (Exception ignored){}
    }

    private Class<? extends ExceptionHandler> getExceptionHandlerClass(){
        UseExceptionHandler exceptionHandlerAnnotation = getClass().getAnnotation(UseExceptionHandler.class);
        if(exceptionHandlerAnnotation != null){
            return exceptionHandlerAnnotation.value();
        }

        return null;
    }

}
