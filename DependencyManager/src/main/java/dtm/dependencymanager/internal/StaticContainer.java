package dtm.dependencymanager.internal;

import dtm.dependencymanager.containers.DependencyContainerStorage;
import lombok.Getter;

public final class StaticContainer {

    @Getter
    private static DependencyContainerStorage containerStorage;

    public static void setContainerStorage(DependencyContainerStorage containerStorage) {
        StaticContainer.containerStorage = containerStorage;
    }

}
