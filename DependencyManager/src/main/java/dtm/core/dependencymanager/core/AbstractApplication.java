package dtm.core.dependencymanager.core;

import android.app.Application;
import android.util.Log;

import dtm.core.dependencymanager.internal.Autoloader;

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
}
