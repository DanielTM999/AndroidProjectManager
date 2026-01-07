package dtm.dependencymanager.core;

import java.util.Map;

public interface AppElementsMapper {
    Integer getViewIdByName(String name);
    Map<String, Integer> getIdViewMap();
}
