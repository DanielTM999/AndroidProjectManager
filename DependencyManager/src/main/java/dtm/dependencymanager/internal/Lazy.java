package dtm.dependencymanager.internal;

import dtm.dependencymanager.exceptions.LazyDependencyException;
import dtm.dependencymanager.core.prototypes.LazyDependency;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class Lazy {

    public static <T> LazyDependency<T> of(Supplier<T> supplier) {
        return new LazyDependency<>() {
            private T dependency;

            @Override
            public T get() {
                if(dependency == null){
                    dependency = supplier.get();
                }
                return dependency;
            }

            @Override
            public boolean isPresent() {
                return supplier.get() != null;
            }

            @Override
            public T awaitOrNull(long timeout, TimeUnit unit) {
                long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

                while (System.currentTimeMillis() < deadline) {
                    T value  = get();
                    if (value != null) return value;
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                }

                return null;
            }

            @Override
            public T awaitOrThrow(long timeout, TimeUnit unit) throws LazyDependencyException {
                T value = awaitOrNull(timeout, unit);
                if(value != null) return value;
                throw new LazyDependencyException("Dependecia não carregada.");
            }

            @Override
            public T awaitOrThrow() throws LazyDependencyException {
                return awaitOrThrow(5, TimeUnit.SECONDS);
            }
        };
    }

    public static <T> LazyDependency<T> constant(T instance) {
        return new LazyDependency<>() {
            @Override
            public T get() {
                return instance;
            }

            @Override
            public boolean isPresent() {
                return instance != null;
            }

            @Override
            public T awaitOrNull(long timeout, TimeUnit unit) {
                return get();
            }

            @Override
            public T awaitOrThrow(long timeout, TimeUnit unit) throws LazyDependencyException {
                T value = awaitOrNull(timeout, unit);
                if(value != null) return value;
                throw new LazyDependencyException("Dependecia não carregada.");
            }

            @Override
            public T awaitOrThrow() throws LazyDependencyException {
                return awaitOrThrow(5, TimeUnit.SECONDS);
            }
        };
    }
}
