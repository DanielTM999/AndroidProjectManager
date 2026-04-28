package dtm.dependencymanager.internal;

import dtm.dependencymanager.containers.DependencyContainerStorage;

public final class StaticContainer {

    private static volatile DependencyContainerStorage containerStorage;

    private StaticContainer() {}

    public static DependencyContainerStorage getContainerStorage() {
        return containerStorage;
    }

    public static void setContainerStorage(DependencyContainerStorage storage) {
        containerStorage = storage;
    }

    public static DependencyContainerStorage getOrCreate(java.util.function.Supplier<DependencyContainerStorage> factory) {
        DependencyContainerStorage local = containerStorage;
        if (local == null) {
            synchronized (StaticContainer.class) {
                local = containerStorage;
                if (local == null) {
                    local = factory.get();
                    containerStorage = local;
                }
            }
        }
        return local;
    }
}
