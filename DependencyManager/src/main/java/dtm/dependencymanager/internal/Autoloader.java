package dtm.dependencymanager.internal;

import dtm.dependencymanager.core.DependencyContainer;

public interface Autoloader {
    void load(DependencyContainer dependencyContainer);
}
