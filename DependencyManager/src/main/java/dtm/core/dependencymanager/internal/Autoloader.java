package dtm.core.dependencymanager.internal;

import dtm.core.dependencymanager.core.DependencyContainer;

public interface Autoloader {
    void load(DependencyContainer dependencyContainer);
}
