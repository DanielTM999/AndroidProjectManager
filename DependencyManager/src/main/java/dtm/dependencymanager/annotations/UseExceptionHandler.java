package dtm.dependencymanager.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import dtm.dependencymanager.core.exception.AbstractExceptionHandler;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UseExceptionHandler {
    Class<? extends AbstractExceptionHandler> value();
}
