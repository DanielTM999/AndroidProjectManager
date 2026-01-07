package dtm.dependencymanager.core;

import android.app.Application;
import android.util.Log;

import dtm.dependencymanager.exceptions.ApplicationStartupException;
import dtm.dependencymanager.internal.Autoloader;

public abstract class AbstractApplication extends Application {
    protected void autoloader(DependencyContainer dependencyContainer){
        try {
            Class<?> clazz = Class.forName(
                    "dtm.core.dependencymanager.generated.DependencyManagerAutoloader"
            );

            Object autoloaderObject = clazz.getMethod("getInstance").invoke(null);

            if(autoloaderObject instanceof Autoloader autoloader){
                autoloader.load(dependencyContainer);
            }

        } catch (Exception e) {
            Log.e("autoloaderError", "Erro ao buscar processor", e);
        }
    }

    protected void onCreateContainer(DependencyContainer dependencyContainer){};

    protected void beforeLoad(){}

    protected void afterLoad(){}

    protected void onLoadError(Throwable th){}

    protected void onApplicationError(Thread thread, Throwable throwable){}
}
