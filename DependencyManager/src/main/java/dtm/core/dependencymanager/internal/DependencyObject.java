package dtm.core.dependencymanager.internal;

import dtm.core.dependencymanager.core.prototypes.Dependency;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class DependencyObject extends Dependency {
    private final Class<?> dependencyClass;
    private final String qualifier;
    private final boolean singleton;
    private final Supplier<?> creatorFunction;
    private final Object singletonInstance;
    private final List<Class<?>> classesInstanceTypes;

    public DependencyObject(Class<?> dependencyClass, String qualifier, boolean singleton, Supplier<?> creatorFunction, Object singletonInstance) {
        this.dependencyClass = dependencyClass;
        this.qualifier = qualifier;
        this.singleton = singleton;
        this.creatorFunction = creatorFunction;
        this.singletonInstance = singletonInstance;
        this.classesInstanceTypes = new ArrayList<>();
    }

    @Override
    public Object getDependency() {
        if(singleton){
            return singletonInstance;
        }
        return creatorFunction.get();
    }

    @Override
    public List<Class<?>> getDependencyClassInstanceTypes() {
        if(classesInstanceTypes.isEmpty()){

            if (dependencyClass.equals(Object.class) || dependencyClass.isInterface()) {
                return List.of();
            }
            Class<?> superClass = dependencyClass.getSuperclass();
            Class<?>[] interfaces = dependencyClass.getInterfaces();

            if (superClass != null && !superClass.equals(Object.class)) {
                classesInstanceTypes.add(superClass);
            }

            for(Class<?> interfaceObj : interfaces){
                if (!interfaceObj.equals(Object.class)) {
                    classesInstanceTypes.add(interfaceObj);
                }
            }
            classesInstanceTypes.add(dependencyClass);
        }


        return new ArrayList<>(classesInstanceTypes);
    }
}
