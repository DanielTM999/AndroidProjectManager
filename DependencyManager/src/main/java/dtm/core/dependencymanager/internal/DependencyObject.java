package dtm.core.dependencymanager.internal;

import dtm.core.dependencymanager.core.prototypes.Dependency;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DependencyObject extends Dependency {
    private Class<?> dependencyClass;
    private String qualifier;
    private boolean singleton;
    private Supplier<?> creatorFunction;
    private Object singletonInstance;

    @Override
    public Object getDependency() {
        if(singleton){
            return singletonInstance;
        }
        return creatorFunction.get();
    }

    @Override
    public List<Class<?>> getDependencyClassInstanceTypes() {
        List<Class<?>> classes = new ArrayList<>();
        if (dependencyClass.equals(Object.class) || dependencyClass.isInterface()) {
            return List.of();
        }
        Class<?> superClass = dependencyClass.getSuperclass();
        Class<?>[] interfaces = dependencyClass.getInterfaces();

        if (superClass != null && !superClass.equals(Object.class)) {
            classes.add(superClass);
        }

        for(Class<?> interfaceObj : interfaces){
            if (!interfaceObj.equals(Object.class)) {
                classes.add(interfaceObj);
            }
        }
        classes.add(dependencyClass);

        return classes;
    }
}
