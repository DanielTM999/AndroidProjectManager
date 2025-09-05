package dtm.core.dependencymanager.core.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import dtm.core.dependencymanager.internal.AppElementsMapperStorage;
import dtm.core.dependencymanager.annotations.ViewElement;
import dtm.core.dependencymanager.core.AppElementsMapper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ViewManagedActivity extends ContextHolderActivity {
    protected AppElementsMapper appElementsMapper;
    private boolean loadActivityView = false;
    protected ViewManagedActivity(){}

    protected void preInit(){}


    @Override
    protected void onCreate(Bundle savedInstanceState, @LayoutRes Integer idLayout){
        preInit();
        super.onCreate(savedInstanceState);
        if(idLayout != null){
            setContentView(idLayout);
        }
        bindViewElement();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        preInit();
        super.onCreate(savedInstanceState);
        bindViewElement();
        loadActivityView = true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        preInit();
        super.onCreate(savedInstanceState, persistentState);
        bindViewElement();
        loadActivityView = true;
    }


    protected void bindViewElement(){
        try{
            this.appElementsMapper = AppElementsMapperStorage.getInstance(this);
            List<Field> viewFields = Arrays
                    .stream(getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(ViewElement.class))
                    .collect(Collectors.toList());


            for (Field variable : viewFields) {
                int id = getId(variable);
                View view = findViewById(id);
                if(!variable.isAccessible()){
                    variable.setAccessible(true);
                }
                variable.set(this, view);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private int getId(Field variable) {
        ViewElement viewElement = variable.getAnnotation(ViewElement.class);
        final String viewElementId = viewElement.id();
        final String idName = (viewElementId == null || viewElementId.isEmpty()) ? variable.getName() : viewElementId;
        Integer id = appElementsMapper.getViewIdByName(idName);
        if(id == null){
            throw new RuntimeException("Id: "+idName+" não encontrado");
        }

        return id;
    }

    public void waitForFragmentToLoad(Runnable action) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityLoad()) {
                    action.run();
                } else {
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    @Override
    public boolean isActivityLoad(){
        return loadActivityView;
    }

}
