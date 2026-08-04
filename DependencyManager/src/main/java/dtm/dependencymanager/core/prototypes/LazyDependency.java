package dtm.dependencymanager.core.prototypes;

import dtm.dependencymanager.exceptions.LazyDependencyException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface LazyDependency<T>{
    T get();
    T awaitOrNull(long timeout, TimeUnit unit);
    T awaitOr(long timeout, TimeUnit unit, Supplier<T> result);
    T awaitOrThrow(long timeout, TimeUnit unit) throws LazyDependencyException;
    T awaitOrThrow() throws LazyDependencyException;
    boolean isPresent();
}
