package dtm.core.apptest;

import java.util.concurrent.atomic.AtomicInteger;

import dtm.dependencymanager.annotations.Service;
import dtm.dependencymanager.annotations.Singleton;

@Service
@Singleton
public class Counter {
    private final AtomicInteger value = new AtomicInteger(0);
    private final String stamp = Integer.toHexString(System.identityHashCode(this));

    public int incrementAndGet() {
        return value.incrementAndGet();
    }

    public int get() {
        return value.get();
    }

    public String stamp() {
        return stamp;
    }
}
