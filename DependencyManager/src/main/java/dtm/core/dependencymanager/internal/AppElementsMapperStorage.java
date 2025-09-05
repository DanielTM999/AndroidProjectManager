package dtm.core.dependencymanager.internal;

import android.content.Context;
import dtm.core.dependencymanager.core.AppElementsMapper;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppElementsMapperStorage implements AppElementsMapper {
    private static AppElementsMapperStorage appElementsMapperStorage;

    private final Map<String, Integer> idActivity;
    private final String packageName;

    private AppElementsMapperStorage(Context lauchActivity){
        this.packageName = lauchActivity.getPackageName();
        idActivity = new ConcurrentHashMap<>();
        populateIdsMap();
    }

    @Override
    public Integer getViewIdByName(String name) {
        return idActivity.getOrDefault(name, null);
    }

    @Override
    public Map<String, Integer> getIdViewMap() {
        return idActivity;
    }

    private void populateIdsMap(){
        try {
            String rClassName = packageName + ".R$id";
            Class<?> rClass = Class.forName(rClassName);
            Field[] fields = rClass.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                int fieldValue = field.getInt(null);
                idActivity.put(fieldName, fieldValue);
            }
        }catch (Exception e){}
    }

    public static AppElementsMapper getInstance(Context lauchActivity){
        if(appElementsMapperStorage == null) appElementsMapperStorage = new AppElementsMapperStorage(lauchActivity);
        return appElementsMapperStorage;
    }

    public static AppElementsMapper getInstance(){
        if(appElementsMapperStorage == null) throw new RuntimeException("Un initialize: "+AppElementsMapper.class);
        return appElementsMapperStorage;
    }
}
