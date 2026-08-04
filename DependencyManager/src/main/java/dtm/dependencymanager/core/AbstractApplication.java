package dtm.dependencymanager.core;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import dtm.dependencymanager.exceptions.ApplicationStartupException;
import dtm.dependencymanager.internal.Autoloader;

public abstract class AbstractApplication extends Application {
    protected void autoloader(DependencyContainer dependencyContainer){

        try{
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }

            Enumeration<URL> files = classLoader.getResources("META-INF/dtm/autoloader.name");

            while (files.hasMoreElements()) {
                URL url = files.nextElement();

                String className;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    className = reader.readLine().trim();

                    Class<?> clazz = Class.forName(className);

                    Object instance = clazz.getMethod("getInstance").invoke(null);

                    if (instance instanceof Autoloader autoloader) {
                        autoloader.load(dependencyContainer);
                    }

                } catch (Exception e) {
                    Log.e("autoloaderError", "Erro ao buscar processor", e);
                }
            }

        }catch (Exception e) {
            throw new ApplicationStartupException("Falha ao iniciar o Dependency Manager", e);
        }

    }

    protected void onCreateContainer(DependencyContainer dependencyContainer){};

    protected void beforeLoad(){}

    protected void afterLoad(){}

    protected void onLoadError(Throwable th){}

    protected void onApplicationError(Thread thread, Throwable throwable){}
}
