package dtm.dependencymanager.internal;

import dtm.dependencymanager.core.prototypes.Dependency;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DependencyObject extends Dependency {
    private final Class<?> dependencyClass;
    private final String qualifier;
    private final boolean singleton;
    private final Supplier<?> creatorFunction;
    private volatile Object singletonInstance;
    private volatile List<Class<?>> classesInstanceTypes;

    public DependencyObject(Class<?> dependencyClass,
                            String qualifier,
                            boolean singleton,
                            Supplier<?> creatorFunction,
                            Object singletonInstance) {
        this.dependencyClass = dependencyClass;
        this.qualifier = qualifier;
        this.singleton = singleton;
        this.creatorFunction = creatorFunction;
        this.singletonInstance = singletonInstance;
    }

    @Override
    public Class<?> getDependencyClass() {
        return dependencyClass;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public boolean isSingleton() {
        return singleton;
    }

    public Supplier<?> getCreatorFunction() {
        return creatorFunction;
    }

    public Object getSingletonInstance() {
        return singletonInstance;
    }

    public void setSingletonInstance(Object instance) {
        this.singletonInstance = instance;
    }

    @Override
    public Object getDependency() {
        if (singleton) {
            return singletonInstance;
        }
        return (creatorFunction != null) ? creatorFunction.get() : null;
    }

    @Override
    public List<Class<?>> getDependencyClassInstanceTypes() {
        List<Class<?>> cached = classesInstanceTypes;
        if (cached != null) return cached;
        synchronized (this) {
            if (classesInstanceTypes != null) return classesInstanceTypes;
            if (dependencyClass == null
                    || dependencyClass.equals(Object.class)
                    || dependencyClass.isInterface()) {
                classesInstanceTypes = Collections.emptyList();
                return classesInstanceTypes;
            }
            List<Class<?>> list = new ArrayList<>();
            Class<?> superClass = dependencyClass.getSuperclass();
            if (superClass != null && !superClass.equals(Object.class)) {
                list.add(superClass);
            }
            for (Class<?> ifc : dependencyClass.getInterfaces()) {
                if (!ifc.equals(Object.class)) {
                    list.add(ifc);
                }
            }
            list.add(dependencyClass);
            classesInstanceTypes = Collections.unmodifiableList(list);
            return classesInstanceTypes;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyObject other)) return false;
        return Objects.equals(dependencyClass, other.dependencyClass)
                && Objects.equals(qualifier, other.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencyClass, qualifier);
    }

    @Override
    public String toString() {
        return "DependencyObject{class=" + (dependencyClass != null ? dependencyClass.getName() : "null")
                + ", qualifier=" + qualifier
                + ", singleton=" + singleton + "}";
    }
}
