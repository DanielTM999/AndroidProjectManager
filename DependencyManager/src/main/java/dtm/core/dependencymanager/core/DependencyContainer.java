package dtm.core.dependencymanager.core;

import androidx.annotation.NonNull;

import dtm.core.dependencymanager.exceptions.InvalidClassRegistrationException;
import dtm.core.dependencymanager.exceptions.NewInstanceException;
import dtm.core.dependencymanager.core.prototypes.Dependency;

import java.util.List;
import java.util.function.Supplier;

public interface DependencyContainer {
    boolean isLoaded();
    void injectDependencies(Object instance);
    void load() throws InvalidClassRegistrationException;
    <T> T getDependency(Class<T> reference);
    <T> T getDependency(Class<T> reference, String referenceName);
    <T> T newInstance(Class<T> referenceClass) throws NewInstanceException;
    void registerDependency(Object dependency, String qualifier) throws InvalidClassRegistrationException;
    void registerDependency(Object dependency) throws InvalidClassRegistrationException;
    void registerDependency(Class<?> dependency) throws InvalidClassRegistrationException;
    <T> void registerDependency(FunctionRegistrationResult<T> action) throws InvalidClassRegistrationException;
    void unRegisterDependency(Class<?> dependency);
    void enableChildrenRegistration();
    void disableChildrenRegistration();
    List<Dependency> getRegisteredDependencies();
    void setInjectionStrategy(InjectionStrategy injectionStrategy);
    void enableLog();
    void disableLog();

    static <T> FunctionRegistrationResult<T> ofAction(@NonNull Class<T> reference, @NonNull Supplier<T> action){
        return ofAction(reference, action, "default");
    }

    static <T> FunctionRegistrationResult<T> ofAction(@NonNull Class<T> reference, @NonNull Supplier<T> action, @NonNull String qualifier){
        return new FunctionRegistrationResult<T>() {
            @NonNull
            @Override
            public Supplier<T> getFunction() {
                return action;
            }

            @NonNull
            @Override
            public Class<T> getReferenceClass() {
                return reference;
            }

            @NonNull
            @Override
            public String getQualifier() {
                return (qualifier.isEmpty()) ? "default" : qualifier;
            }

        };
    }

}
