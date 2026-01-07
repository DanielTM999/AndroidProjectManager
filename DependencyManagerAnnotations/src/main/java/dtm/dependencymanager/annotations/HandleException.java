package dtm.core.dependencymanager.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HandleException {
    Class<? extends Throwable>[] value();
}
