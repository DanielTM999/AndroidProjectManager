package dtm.dependencymanager.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import dtm.dependencymanager.annotations.UseExceptionHandler;
import dtm.dependencymanager.exceptions.ApplicationStartupException;
import dtm.dependencymanager.internal.AppElementsMapperStorage;
import dtm.dependencymanager.containers.DependencyContainerStorage;
import dtm.dependencymanager.exceptions.InvalidClassRegistrationException;
import dtm.dependencymanager.internal.ExceptionHandlerDefault;
import dtm.dependencymanager.core.exception.AbstractExceptionHandler;

public abstract class ApplicationCore extends AbstractApplication {

    protected final DependencyContainerStorage dependencyContainer;
    private final AtomicReference<AbstractExceptionHandler> defaultExceptionHandlerRef;
    private final Class<? extends AbstractExceptionHandler> exceptionHandlerClass;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private final Handler mainHandler;

    protected ApplicationCore() {
        this.dependencyContainer = DependencyContainerStorage.getInstance();
        this.defaultExceptionHandlerRef = new AtomicReference<>(
                new ExceptionHandlerDefault(Thread.getDefaultUncaughtExceptionHandler())
        );
        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.exceptionHandlerClass = getExceptionHandlerClass();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this::onApplicationError);
        // Autoloader runs after Application.onCreate so context-dependent loaders are safe
        this.autoloader(this.dependencyContainer);
        this.onCreateContainer(this.dependencyContainer);
        this.beforeLoad();
        load().whenComplete((result, throwable) -> {
            if (throwable != null) {
                runOnUiThread(() -> onLoadError(throwable));
            } else {
                configureCustomExceptionHandler();
                runOnUiThread(this::afterLoad);
            }
        });
    }

    @Override
    protected void beforeLoad() {
        AppElementsMapperStorage.getInstance(this);
    }

    @Override
    protected void onLoadError(Throwable th) {
        if (th instanceof ApplicationStartupException e) throw e;
        throw new ApplicationStartupException("Erro ao iniciar a aplicação", th);
    }

    @Override
    protected void onApplicationError(Thread thread, Throwable throwable) {
        AbstractExceptionHandler exceptionHandler = defaultExceptionHandlerRef.get();
        if (exceptionHandler != null) {
            exceptionHandler.onError(thread, throwable, getApplicationContext());
        } else {
            Log.e("onApplicationError", "Erro não tratado na thread: " + thread.getName(), throwable);
        }
    }

    protected void runOnUiThread(Runnable action) {
        if (Looper.getMainLooper().isCurrentThread()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }

    private CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            try {
                dependencyContainer.load();
            } catch (InvalidClassRegistrationException e) {
                throw new ApplicationStartupException("Erro ao carregar Conteiner de dependencias", e);
            }
        }, dependencyContainer.getExecutor());
    }

    private void configureCustomExceptionHandler() {
        try {
            dependencyContainer.registerDependency(uncaughtExceptionHandler);
            if (exceptionHandlerClass != null) {
                AbstractExceptionHandler custom = dependencyContainer.newInstance(exceptionHandlerClass);
                if (custom != null) defaultExceptionHandlerRef.set(custom);
            }
        } catch (Exception ignored) {}
    }

    private Class<? extends AbstractExceptionHandler> getExceptionHandlerClass() {
        UseExceptionHandler ann = getClass().getAnnotation(UseExceptionHandler.class);
        return (ann != null) ? ann.value() : null;
    }
}
