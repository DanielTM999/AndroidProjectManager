package dtm.core.dependencymanager.core.prototypes;

import dtm.core.dependencymanager.exceptions.LazyDependencyException;

import java.util.concurrent.TimeUnit;

public interface LazyDependency<T>{
    T get();

    T awaitOrNull(long timeout, TimeUnit unit);
    T awaitOrThrow(long timeout, TimeUnit unit) throws LazyDependencyException;
    T awaitOrThrow() throws LazyDependencyException;
    boolean isPresent();
}
