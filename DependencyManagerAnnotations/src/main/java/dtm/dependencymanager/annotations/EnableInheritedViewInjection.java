package dtm.dependencymanager.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableInheritedViewInjection {
    
    /**
     * Quantos níveis de superclasse serão escaneados.
     * 0 = não sobe (somente a classe atual)
     * 1 = sobe 1 nível (default)
     * 2 = sobe até o "avô"
     * N = sobe até N níveis acima
     */
    int levels() default 1;
}
