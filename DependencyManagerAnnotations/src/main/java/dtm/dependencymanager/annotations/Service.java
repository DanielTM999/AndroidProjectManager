package dtm.dependencymanager.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Service {
    String qualifier() default "default";
}
