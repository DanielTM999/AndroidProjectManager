package dtm.core.dependencymanager.core;

import java.util.Map;

public interface AppElementsMapper {
    Integer getViewIdByName(String name);
    Map<String, Integer> getIdViewMap();
}
